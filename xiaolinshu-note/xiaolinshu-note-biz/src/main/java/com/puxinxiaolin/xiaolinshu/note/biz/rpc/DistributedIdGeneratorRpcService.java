package com.puxinxiaolin.xiaolinshu.note.biz.rpc;

import com.puxinxiaolin.xiaolinshu.distributed.id.generator.api.DistributedIdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DistributedIdGeneratorRpcService {

    @Resource
    private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

    /**
     * 生成雪花算法 ID
     *
     * @param key
     * @return
     */
    public String getSnowflakeId(String key) {
        return distributedIdGeneratorFeignApi.getSnowflakeId(key);
    }

}
