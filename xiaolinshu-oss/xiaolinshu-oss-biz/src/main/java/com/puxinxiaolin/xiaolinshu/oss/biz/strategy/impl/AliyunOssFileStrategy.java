package com.puxinxiaolin.xiaolinshu.oss.biz.strategy.impl;

import com.aliyun.oss.OSS;
import com.puxinxiaolin.xiaolinshu.oss.biz.config.AliyunOSSProperties;
import com.puxinxiaolin.xiaolinshu.oss.biz.strategy.FileStrategy;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.util.UUID;

@Slf4j
public class AliyunOssFileStrategy implements FileStrategy {
    @Resource
    private AliyunOSSProperties aliyunOSSProperties;
    @Resource
    private OSS ossClient;
    
    /**
     * 文件上传
     *
     * @param file
     * @param bucketName
     * @return
     */
    @SneakyThrows
    @Override
    public String uploadFile(MultipartFile file, String bucketName) {
        log.info("## 上传文件至阿里云 OSS ...");
        
        if (file == null || file.getSize() == 0) {
            log.error("==> 上传文件异常：文件大小为空 ...");
            throw new RuntimeException("文件大小不能为空");
        }
        
        String originalFileName = file.getOriginalFilename();
        
        String key = UUID.randomUUID().toString().replace("-", "");
        String suffix = originalFileName.substring(originalFileName.lastIndexOf("."));
        
        String objectName = String.format("%s%s", key, suffix);

        log.info("==> 开始上传文件至阿里云 OSS, ObjectName: {}", objectName);
        
        ossClient.putObject(bucketName, objectName, new ByteArrayInputStream(file.getInputStream().readAllBytes()));
        
        String url = String.format("https://%s.%s/%s", bucketName, aliyunOSSProperties.getEndpoint(), objectName);
        log.info("==> 上传文件至阿里云 OSS 成功, 访问路径: {}", url);
        
        return url;
    }
    
}
