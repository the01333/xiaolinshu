package com.puxinxiaolin.xiaolinshu.oss.biz.sevice.impl;

import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.oss.biz.sevice.FileService;
import com.puxinxiaolin.xiaolinshu.oss.biz.strategy.FileStrategy;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileServiceImpl implements FileService {
    @Resource
    private FileStrategy fileStrategy;
    
    private static final String BUCKET_NAME = "xiaolinshu";

    /**
     * 上传文件
     *
     * @param file
     * @return
     */
    @Override
    public Response<?> uploadFile(MultipartFile file) {
        String url = fileStrategy.uploadFile(file, BUCKET_NAME);

        return Response.success(url);
    }
    
}
