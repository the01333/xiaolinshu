package com.puxinxiaolin.xiaolinshu.note.biz.rpc;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.kv.api.api.KeyValueFeignApi;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.AddNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.DeleteNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.FindNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.rsp.FindNoteContentRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class KeyValueRpcService {

    @Resource
    private KeyValueFeignApi keyValueFeignApi;

    /**
     * 新增笔记内容
     *
     * @param uuid
     * @param content
     * @return
     */
    public boolean saveNoteContent(String uuid, String content) {
        AddNoteContentReqDTO dto = new AddNoteContentReqDTO();
        dto.setUuid(uuid);
        dto.setContent(content);

        Response<?> response = keyValueFeignApi.addNoteContent(dto);
        return response != null && response.isSuccess();
    }

    /**
     * 删除笔记内容
     *
     * @param uuid
     * @return
     */
    public boolean deleteNoteContent(String uuid) {
        DeleteNoteContentReqDTO dto = new DeleteNoteContentReqDTO();
        dto.setUuid(uuid);

        Response<?> response = keyValueFeignApi.deleteNoteContent(dto);
        return response != null && response.isSuccess();
    }

    /**
     * 查询笔记内容
     *
     * @param uuid
     * @return
     */
    public String findNoteContent(String uuid) {
        FindNoteContentReqDTO findNoteContentReqDTO = new FindNoteContentReqDTO();
        findNoteContentReqDTO.setUuid(uuid);

        Response<FindNoteContentRspDTO> response = keyValueFeignApi.findNoteContent(findNoteContentReqDTO);

        if (Objects.isNull(response) || !response.isSuccess() || Objects.isNull(response.getData())) {
            return null;
        }

        return response.getData().getContent();
    }

}
