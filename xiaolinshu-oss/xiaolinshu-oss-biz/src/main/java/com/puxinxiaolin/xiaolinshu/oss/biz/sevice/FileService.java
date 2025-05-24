package com.puxinxiaolin.xiaolinshu.oss.biz.sevice;

import com.puxinxiaolin.framework.common.response.Response;
import org.springframework.web.multipart.MultipartFile;

public interface FileService {

    /**
     * 上传文件
     *
     * @param file
     * @return
     */
    Response<?> uploadFile(MultipartFile file);

}
