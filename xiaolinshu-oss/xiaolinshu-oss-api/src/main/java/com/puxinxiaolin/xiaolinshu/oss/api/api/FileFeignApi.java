package com.puxinxiaolin.xiaolinshu.oss.api.api;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.oss.api.config.FeignFormConfig;
import com.puxinxiaolin.xiaolinshu.oss.api.constant.ApiConstants;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Description: 提供给外部调用的 rpc 接口
 * @Author: YCcLin
 * @Date: 2025/5/25 15:52
 */
@FeignClient(name = ApiConstants.SERVICE_NAME, configuration = {FeignFormConfig.class})
public interface FileFeignApi {

    String PREFIX = "/file";

    @PostMapping(value = PREFIX + "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    Response<?> uploadFile(@RequestPart(value = "file") MultipartFile file);

}
