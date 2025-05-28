package com.puxinxiaolin.xiaolinshu.kv.api.api;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.kv.api.constant.ApiConstants;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.AddNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.DeleteNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.req.FindNoteContentReqDTO;
import com.puxinxiaolin.xiaolinshu.kv.api.dto.rsp.FindNoteContentRspDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * @Description: 供外界调用的 RPC 接口
 * @Author: YCcLin
 * @Date: 2025/5/26 15:34
 */
@FeignClient(name = ApiConstants.SERVICE_NAME)
public interface KeyValueFeignApi {

    String PREFIX = "/kv";

    @PostMapping(PREFIX + "/note/content/add")
    Response<?> addNoteContent(@RequestBody AddNoteContentReqDTO request);

    @PostMapping(PREFIX + "/note/content/find")
    Response<FindNoteContentRspDTO> findNoteContent(@RequestBody FindNoteContentReqDTO request);

    @PostMapping(PREFIX + "/note/content/delete")
    Response<?> deleteNoteContent(@RequestBody DeleteNoteContentReqDTO request);

}
