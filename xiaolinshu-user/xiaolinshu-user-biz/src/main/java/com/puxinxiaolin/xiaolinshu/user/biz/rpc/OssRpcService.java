package com.puxinxiaolin.xiaolinshu.user.biz.rpc;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.oss.api.api.FileFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * @Description: 通过 rpc 调用 OSS 模块
 * @Author: YCcLin
 * @Date: 2025/5/25 16:10
 */
@Component
public class OssRpcService {

    @Resource
    private FileFeignApi fileFeignApi;

    public String uploadFile(MultipartFile file) {
        Response<?> response = fileFeignApi.uploadFile(file);
        if (!response.isSuccess()) {
            return null;
        }

        return response.getData().toString();
    }

}
