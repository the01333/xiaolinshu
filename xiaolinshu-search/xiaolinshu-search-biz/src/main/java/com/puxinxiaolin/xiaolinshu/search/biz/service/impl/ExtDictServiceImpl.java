package com.puxinxiaolin.xiaolinshu.search.biz.service.impl;

import com.puxinxiaolin.xiaolinshu.search.biz.service.ExtDictService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ExtDictServiceImpl implements ExtDictService {
    
    @Value("${elasticsearch.hotUpdateExtDict}")
    private String hotUpdateExtDict;
        
    /**
     * 获取热更新词典
     *
     * @return
     */
    @Override
    public ResponseEntity<String> getHotUpdateExtDict() {
        try {
            Path path = Paths.get(hotUpdateExtDict);
            // 获取文件的最后修改时间
            long lastModifiedTime = Files.getLastModifiedTime(path).toMillis();

            // 生成 ETag（使用文件内容的哈希值）
            String fileContent = Files.lines(path)
                    .collect(Collectors.joining("\n"));
            String eTag = String.valueOf(fileContent.hashCode());

            HttpHeaders headers = new HttpHeaders();
            headers.set("ETag", eTag);
            headers.setContentType(MediaType.valueOf("text/plain;charset=UTF-8"));
            
            return ResponseEntity.ok()
                    .headers(headers)
                    .lastModified(lastModifiedTime)
                    .body(fileContent);
        } catch (IOException e) {
            log.error("==> 获取热更新词典异常:{} ", e.getMessage(), e);
        }
        return null;
    }
    
}
