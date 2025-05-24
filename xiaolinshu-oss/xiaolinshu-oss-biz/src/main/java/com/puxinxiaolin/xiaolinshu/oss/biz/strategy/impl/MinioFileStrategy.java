package com.puxinxiaolin.xiaolinshu.oss.biz.strategy.impl;

import com.puxinxiaolin.xiaolinshu.oss.biz.config.MinioProperties;
import com.puxinxiaolin.xiaolinshu.oss.biz.strategy.FileStrategy;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import jakarta.annotation.Resource;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

@Slf4j
public class MinioFileStrategy implements FileStrategy {
    @Resource
    private MinioProperties minioProperties;
    @Resource
    private MinioClient minioClient;
    
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
        log.info("## 上传文件至 Minio ...");
        
        if (file == null || file.getSize() == 0) {
            log.error("==> 上传文件异常：文件大小为空 ...");
            throw new RuntimeException("文件大小不能为空");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();

        String key = UUID.randomUUID().toString()
                .replace("-", "");
        String suffix = originalFilename.substring(originalFilename.lastIndexOf("."));
        
        String finalFileName = String.format("%s%s", key, suffix);

        log.info("==> 开始上传文件至 Minio, ObjectName: {}", finalFileName);
        
        minioClient.putObject(PutObjectArgs.builder()
                        .bucket(bucketName)
                        .object(finalFileName)
                        .stream(file.getInputStream(), file.getSize(), -1)
                        .contentType(contentType)
                .build());

        String url = String.format("%s/%s/%s", minioProperties.getEndpoint(), bucketName, finalFileName);
        log.info("==> 上传文件至 Minio 成功, 访问路径: {}", url);
        return url;
    }
}
