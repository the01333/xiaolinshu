package com.puxinxiaolin.xiaolinshu.comment.biz.service.impl;

import cn.hutool.core.collection.CollUtil;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.puxinxiaolin.framework.biz.context.holder.LoginUserContextHolder;
import com.puxinxiaolin.framework.common.constant.DateConstants;
import com.puxinxiaolin.framework.common.response.PageResponse;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.framework.common.util.DateUtils;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.xiaolinshu.comment.biz.constant.MQConstants;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.dataobject.CommentDO;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.CommentDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.domain.mapper.NoteCountDOMapper;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.dto.PublishCommentMqDTO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.vo.FindCommentItemRspVO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.vo.FindCommentPageListReqVO;
import com.puxinxiaolin.xiaolinshu.comment.biz.model.vo.PublishCommentReqVO;
import com.puxinxiaolin.xiaolinshu.comment.biz.retry.SendMqRetryHelper;
import com.puxinxiaolin.xiaolinshu.comment.biz.rpc.DistributedIdGeneratorRpcService;
import com.puxinxiaolin.xiaolinshu.comment.biz.rpc.KeyValueRpcService;
import com.puxinxiaolin.xiaolinshu.comment.biz.rpc.UserRpcService;
import com.puxinxiaolin.xiaolinshu.comment.biz.service.CommentService;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.FindCommentContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.rsp.FindCommentContentRspDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;

@Service
@Slf4j
public class CommentServiceImpl implements CommentService {
    @Resource
    private SendMqRetryHelper sendMqRetryHelper;
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;
    @Resource
    private NoteCountDOMapper noteCountDOMapper;
    @Resource
    private CommentDOMapper commentDOMapper;
    @Resource
    private UserRpcService userRpcService;
    @Resource
    private KeyValueRpcService keyValueRpcService;

    /**
     * 评论列表分页查询
     *
     * @param request
     * @return
     */
    @Override
    public PageResponse<FindCommentItemRspVO> findCommentPageList(FindCommentPageListReqVO request) {
        Long noteId = request.getNoteId();
        Integer pageNo = request.getPageNo();
        long pageSize = 20;

        // TODO [YCcLin 2025/8/17]: 先走缓存

        Long count = noteCountDOMapper.selectCommentTotalByNoteId(noteId);
        if (Objects.isNull(count)) {
            return PageResponse.success(null, pageNo, pageSize);
        }

        List<FindCommentItemRspVO> result = null;
        if (count > 0) {
            result = Lists.newArrayList();

            long offset = PageResponse.getOffset(pageNo, pageSize);
            // 一级评论
            List<CommentDO> oneLevelCommentDOS = commentDOMapper.selectPageList(noteId, offset, pageSize);
            // 最早回复的二级评论 id
            List<Long> twoLevelCommentIds = oneLevelCommentDOS.stream()
                    .map(CommentDO::getFirstReplyCommentId)
                    .filter(id -> id != 0)
                    .toList();

            Map<Long, CommentDO> commentIdAndDOMap = null;
            // 二级评论
            List<CommentDO> twoLevelCommentDOS = null;
            if (CollUtil.isNotEmpty(twoLevelCommentIds)) {
                twoLevelCommentDOS = commentDOMapper.selectTwoLevelCommentByIds(twoLevelCommentIds);

                commentIdAndDOMap = twoLevelCommentDOS.stream()
                        .collect(Collectors.toMap(CommentDO::getId, commentDO -> commentDO));
            }

            // 调用 KV 服务需要的入参
            List<FindCommentContentReqDTO> kvCommentContentReqDTOS = Lists.newArrayList();
            // 调用用户服务的入参
            List<Long> userIds = Lists.newArrayList();

            // 将一级评论和二级评论合并到一起
            List<CommentDO> allCommentDOS = Lists.newArrayList();
            CollUtil.addAll(allCommentDOS, oneLevelCommentDOS);
            CollUtil.addAll(allCommentDOS, twoLevelCommentDOS);

            allCommentDOS.forEach(commentDO -> {
                Boolean isContentEmpty = commentDO.getIsContentEmpty();
                if (!isContentEmpty) {
                    FindCommentContentReqDTO kvCommentContentReqDTO = FindCommentContentReqDTO.builder()
                            .contentId(commentDO.getContentUuid())
                            .yearMonth(DateConstants.Y_M.format(commentDO.getCreateTime()))
                            .build();

                    kvCommentContentReqDTOS.add(kvCommentContentReqDTO);
                }

                userIds.add(commentDO.getUserId());
            });

            // 走 RPC
            // kv - rpc
            List<FindCommentContentRspDTO> findCommentContentRspDTOS = keyValueRpcService.batchFindCommentContent(noteId, kvCommentContentReqDTOS);
            Map<String, String> commentUuidAndContentMap = null;
            if (CollUtil.isNotEmpty(findCommentContentRspDTOS)) {
                commentUuidAndContentMap = findCommentContentRspDTOS.stream()
                        .collect(Collectors.toMap(FindCommentContentRspDTO::getContentId, FindCommentContentRspDTO::getContent));
            }

            // user - rpc
            List<FindUserByIdRspDTO> findUserByIdRspDTOS = userRpcService.findByIds(userIds);
            Map<Long, FindUserByIdRspDTO> userIdAndDTOMap = null;
            if (CollUtil.isNotEmpty(findUserByIdRspDTOS)) {
                userIdAndDTOMap = findUserByIdRspDTOS.stream()
                        .collect(Collectors.toMap(FindUserByIdRspDTO::getId, dto -> dto));
            }

            // 拼接所有参数
            // 1. 先拼接一级评论
            for (CommentDO oneLevelCommentDO : oneLevelCommentDOS) {
                Long userId = oneLevelCommentDO.getUserId();
                FindCommentItemRspVO oneLevelCommentItemRspVO = FindCommentItemRspVO.builder()
                        .userId(userId)
                        .commentId(oneLevelCommentDO.getId())
                        .imageUrl(oneLevelCommentDO.getImageUrl())
                        .likeTotal(oneLevelCommentDO.getLikeTotal())
                        .childCommentTotal(oneLevelCommentDO.getChildCommentTotal())
                        .createTime(DateUtils.formatRelativeTime(oneLevelCommentDO.getCreateTime()))
                        .build();

                setUserInfo(oneLevelCommentItemRspVO, userIdAndDTOMap, userId, commentIdAndDOMap);
                setCommentContent(oneLevelCommentItemRspVO, commentUuidAndContentMap, oneLevelCommentDO);

                // 2. 拼接二级评论
                Long firstReplyCommentId = oneLevelCommentDO.getFirstReplyCommentId();
                if (CollUtil.isNotEmpty(commentIdAndDOMap)) {
                    CommentDO twoLevelCommentDO = commentIdAndDOMap.get(firstReplyCommentId);
                    if (Objects.nonNull(twoLevelCommentDO)) {
                        Long twoLevelCommentUserId = twoLevelCommentDO.getUserId();
                        FindCommentItemRspVO twoLevelCommentItemRspVO = FindCommentItemRspVO.builder()
                                .userId(twoLevelCommentUserId)
                                .commentId(twoLevelCommentDO.getId())
                                .imageUrl(twoLevelCommentDO.getImageUrl())
                                .likeTotal(twoLevelCommentDO.getLikeTotal())
                                .createTime(DateUtils.formatRelativeTime(twoLevelCommentDO.getCreateTime()))
                                .build();

                        setUserInfo(twoLevelCommentItemRspVO, userIdAndDTOMap, twoLevelCommentUserId, commentIdAndDOMap);
                        oneLevelCommentItemRspVO.setFirstReplyComment(twoLevelCommentItemRspVO);
                        setCommentContent(twoLevelCommentItemRspVO, commentUuidAndContentMap, twoLevelCommentDO);
                    }
                }

                result.add(oneLevelCommentItemRspVO);
            }
        }
        return PageResponse.success(result, pageNo, count, pageSize);
    }

    /**
     * 发布评论（由于该接口是高并发接口, 这里发 MQ 来实现异步写）
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> publishComment(PublishCommentReqVO request) {
        String content = request.getContent();
        String imageUrl = request.getImageUrl();

        Preconditions.checkArgument(StringUtils.isNotBlank(content) || StringUtils.isNotBlank(imageUrl),
                "评论正文和图片不能同时为空");

        Long creatorId = LoginUserContextHolder.getUserId();
        String commentId = distributedIdGeneratorRpcService.generateCommentId();
        PublishCommentMqDTO mqDTO = PublishCommentMqDTO.builder()
                .noteId(request.getNoteId())
                .content(content)
                .imageUrl(imageUrl)
                .commentId(Long.valueOf(commentId))
                .replyCommentId(request.getReplyCommentId())
                .createTime(LocalDateTime.now())
                .creatorId(creatorId)
                .build();

        // 走 MQ（包括重试机制）
        sendMqRetryHelper.asyncSend(MQConstants.TOPIC_PUBLISH_COMMENT, JsonUtils.toJsonString(mqDTO));

        return Response.success();
    }

    /**
     * 设置用户信息
     *
     * @param findCommentItemRspVO
     * @param userIdAndDTOMap
     * @param userId
     * @param commentIdAndDOMap    todo: 看是否需要删除该无用参数
     */
    private void setUserInfo(FindCommentItemRspVO findCommentItemRspVO,
                             Map<Long, FindUserByIdRspDTO> userIdAndDTOMap,
                             Long userId, Map<Long, CommentDO> commentIdAndDOMap) {
        if (CollUtil.isNotEmpty(userIdAndDTOMap)) {
            FindUserByIdRspDTO findUserByIdRspDTO = userIdAndDTOMap.get(userId);
            if (Objects.nonNull(findUserByIdRspDTO)) {
                findCommentItemRspVO.setAvatar(findUserByIdRspDTO.getAvatar());
                findCommentItemRspVO.setNickname(findUserByIdRspDTO.getNickName());
            }
        }
    }

    /**
     * 设置评论内容
     *
     * @param findCommentItemRspVO
     * @param commentUuidAndContentMap
     * @param commentDO
     */
    private void setCommentContent(FindCommentItemRspVO findCommentItemRspVO,
                                   Map<String, String> commentUuidAndContentMap,
                                   CommentDO commentDO) {
        if (CollUtil.isNotEmpty(commentUuidAndContentMap)) {
            String contentUuid = commentDO.getContentUuid();
            if (StringUtils.isNotBlank(contentUuid)) {
                findCommentItemRspVO.setContent(commentUuidAndContentMap.get(contentUuid));
            }
        }
    }

}
