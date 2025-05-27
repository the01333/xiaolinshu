package com.puxinxiaolin.xiaolinshu.distributed.id.generator.api;

import com.puxinxiaolin.xiaolinshu.distributed.id.generator.constant.ApiConstant;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = ApiConstant.SERVICE_NAME)
public interface DistributedIdGeneratorFeignApi {
    
    String PREFIX = "/id";

    @GetMapping(value = PREFIX + "/segment/get/{key}")
    String getSegmentId(@PathVariable("key") String key);

    @GetMapping(value = PREFIX + "/snowflake/get/{key}")
    String getSnowflakeId(@PathVariable("key") String key);
    
}
