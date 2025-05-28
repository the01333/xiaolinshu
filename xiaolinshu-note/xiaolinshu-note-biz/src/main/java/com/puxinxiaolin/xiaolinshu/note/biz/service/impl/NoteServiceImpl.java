package com.puxinxiaolin.xiaolinshu.note.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.google.common.base.Preconditions;
import com.puxinxiaolin.framework.biz.context.holder.LoginUserContextHolder;
import com.puxinxiaolin.framework.common.exception.BizException;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.dataobject.NoteDO;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper.NoteDOMapper;
import com.puxinxiaolin.xiaolinshu.note.biz.domain.mapper.TopicDOMapper;
import com.puxinxiaolin.xiaolinshu.note.biz.enums.NoteStatusEnum;
import com.puxinxiaolin.xiaolinshu.note.biz.enums.NoteTypeEnum;
import com.puxinxiaolin.xiaolinshu.note.biz.enums.NoteVisibleEnum;
import com.puxinxiaolin.xiaolinshu.note.biz.enums.ResponseCodeEnum;
import com.puxinxiaolin.xiaolinshu.note.biz.model.vo.FindNoteDetailReqVO;
import com.puxinxiaolin.xiaolinshu.note.biz.model.vo.FindNoteDetailRspVO;
import com.puxinxiaolin.xiaolinshu.note.biz.model.vo.PublishNoteReqVO;
import com.puxinxiaolin.xiaolinshu.note.biz.rpc.DistributedIdGeneratorRpcService;
import com.puxinxiaolin.xiaolinshu.note.biz.rpc.KeyValueRpcService;
import com.puxinxiaolin.xiaolinshu.note.biz.rpc.UserRpcService;
import com.puxinxiaolin.xiaolinshu.note.biz.service.NoteService;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Service
@Slf4j
public class NoteServiceImpl implements NoteService {
    @Resource
    private NoteDOMapper noteDOMapper;
    @Resource
    private TopicDOMapper topicDOMapper;
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource
    private KeyValueRpcService keyValueRpcService;
    @Resource
    private UserRpcService userRpcService;

    /**
     * 笔记发布
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> publishNote(PublishNoteReqVO request) {
        Integer type = request.getType();
        NoteTypeEnum noteTypeEnum = NoteTypeEnum.valueOf(type);
        if (Objects.isNull(noteTypeEnum)) {
            throw new BizException(ResponseCodeEnum.NOTE_TYPE_ERROR);
        }

        String imgUris = null;
        // 笔记内容是否为空, 默认值为 true, 即空
        Boolean isContentEmpty = true;
        String videoUri = null;
        switch (noteTypeEnum) {
            case VIDEO -> {
                videoUri = request.getVideoUri();

                Preconditions.checkArgument(StringUtils.isNotBlank(videoUri), "笔记视频不能为空");
            }
            case IMAGE_TEXT -> {
                List<String> imgUriList = request.getImgUris();

                Preconditions.checkArgument(CollUtil.isNotEmpty(imgUriList), "笔记图片不能为空");
                Preconditions.checkArgument(imgUriList.size() <= 8, "笔记图片不能多于 8 张");

                imgUris = StringUtils.join(imgUriList, ",");
            }
            default -> {
            }
        }

        String snowflakeId = distributedIdGeneratorRpcService.getSnowflakeId("note");
        String contentUuid = null;
        String content = request.getContent();
        if (StringUtils.isNotBlank(content)) {
            isContentEmpty = false;
            contentUuid = UUID.randomUUID().toString();
            boolean isSavedSuccess = keyValueRpcService.saveNoteContent(contentUuid, content);

            if (!isSavedSuccess) {
                throw new BizException(ResponseCodeEnum.NOTE_PUBLISH_FAIL);
            }
        }

        Long topicId = request.getTopicId();
        String topicName = null;
        if (Objects.nonNull(topicId)) {
            topicName = topicDOMapper.selectNameByPrimaryKey(topicId);
        }

        Long creatorId = LoginUserContextHolder.getUserId();
        NoteDO noteDO = NoteDO.builder()
                .id(Long.valueOf(snowflakeId))
                .isContentEmpty(isContentEmpty)
                .creatorId(creatorId)
                .imgUris(imgUris)
                .videoUri(videoUri)
                .type(type)
                .topicId(topicId)
                .topicName(topicName)
                .visible(NoteVisibleEnum.PUBLIC.getCode())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .status(NoteStatusEnum.NORMAL.getCode())
                .title(request.getTitle())
                .isTop(Boolean.FALSE)
                .contentUuid(contentUuid)
                .build();

        try {
            noteDOMapper.insert(noteDO);
        } catch (Exception e) {
            log.error("==> 笔记存储失败", e);

            // RPC: 笔记保存失败, 则删除笔记内容
            if (StringUtils.isNotBlank(contentUuid)) {
                keyValueRpcService.deleteNoteContent(contentUuid);
            }
        }

        return Response.success();
    }

    /**
     * 查询笔记详情
     *
     * @param request
     * @return
     */
    @Override
    public Response<FindNoteDetailRspVO> findNoteDetail(FindNoteDetailReqVO request) {
        Long noteId = request.getId();
        Long userId = LoginUserContextHolder.getUserId();

        NoteDO noteDO = noteDOMapper.selectByPrimaryKey(noteId);
        if (Objects.isNull(noteDO)) {
            throw new BizException(ResponseCodeEnum.NOTE_NOT_FOUND);
        }

        Integer visible = noteDO.getVisible();
        checkNoteVisible(visible, userId, noteDO.getCreatorId());
        
        // 走 rpc
        Long creatorId = noteDO.getCreatorId();
        FindUserByIdRspDTO findUserByIdRspDTO = userRpcService.findById(creatorId);
        
        String content = null;
        if (Objects.equals(noteDO.getIsContentEmpty(), Boolean.FALSE)) {
            content = keyValueRpcService.findNoteContent(noteDO.getContentUuid());
        }

        Integer type = noteDO.getType();
        String imgUrisStr = noteDO.getImgUris();
        List<String> imgUris = null;
        if (Objects.equals(type, NoteTypeEnum.IMAGE_TEXT.getCode())
                && StringUtils.isNotBlank(imgUrisStr)) {
            imgUris = List.of(imgUrisStr.split(","));
        }

        FindNoteDetailRspVO vo = FindNoteDetailRspVO.builder()
                .id(noteDO.getId())
                .type(noteDO.getType())
                .title(noteDO.getTitle())
                .content(content)
                .imgUris(imgUris)
                .topicId(noteDO.getTopicId())
                .topicName(noteDO.getTopicName())
                .creatorId(noteDO.getCreatorId())
                .creatorName(findUserByIdRspDTO.getNickName())
                .avatar(findUserByIdRspDTO.getAvatar())
                .videoUri(noteDO.getVideoUri())
                .updateTime(noteDO.getUpdateTime())
                .visible(noteDO.getVisible())
                .build();
        return Response.success(vo);
    }

    /**
     * 检查笔记是否可见
     *
     * @param visible
     * @param userId
     * @param creatorId
     */
    private void checkNoteVisible(Integer visible, Long userId, Long creatorId) {
        if (Objects.equals(visible, NoteVisibleEnum.PRIVATE.getCode())
                && !Objects.equals(userId, creatorId)) {
            throw new BizException(ResponseCodeEnum.NOTE_PRIVATE);
        }
    }

}
