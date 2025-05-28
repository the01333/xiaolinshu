package com.puxinxiaolin.xiaolinshu.note.biz.rpc;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.kv.api.api.KeyValueFeignApi;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.AddNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.DeleteNoteContentReqDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

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

}
