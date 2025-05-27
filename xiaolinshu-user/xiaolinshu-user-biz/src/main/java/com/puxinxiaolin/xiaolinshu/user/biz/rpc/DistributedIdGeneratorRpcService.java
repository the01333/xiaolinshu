package com.puxinxiaolin.xiaolinshu.user.biz.rpc;

import com.puxinxiaolin.xiaolinshu.distributed.id.generator.api.DistributedIdGeneratorFeignApi;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

@Component
public class DistributedIdGeneratorRpcService {

    @Resource
    private DistributedIdGeneratorFeignApi distributedIdGeneratorFeignApi;

    // Leaf 号段模式：小林书 ID 业务标识
    private static final String BIZ_TAG_XIAOLINSHU_ID = "leaf-segment-xiaolinshu-id";
    // Leaf 号段模式：用户 ID 业务标识
    private static final String BIZ_TAG_USER_ID = "leaf-segment-user-id";
    
    public String getXiaolinshuId() {
        return distributedIdGeneratorFeignApi.getSegmentId(BIZ_TAG_XIAOLINSHU_ID);
    }
    
    public String getUserId() {
        return distributedIdGeneratorFeignApi.getSegmentId(BIZ_TAG_USER_ID);
    }

}
