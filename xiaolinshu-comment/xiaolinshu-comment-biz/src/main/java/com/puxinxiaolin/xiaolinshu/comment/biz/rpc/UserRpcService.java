package com.puxinxiaolin.xiaolinshu.comment.biz.rpc;

import cn.hutool.core.collection.CollUtil;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.user.api.api.UserFeignApi;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.FindUserByIdReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.FindUsersByIdsReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByIdRspDTO;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class UserRpcService {

    @Resource
    private UserFeignApi userFeignApi;

    /**
     * 批量查询用户信息
     *
     * @param userIds
     * @return
     */
    public List<FindUserByIdRspDTO> findByIds(List<Long> userIds) {
        if (CollUtil.isEmpty(userIds)) {
            return null;
        }

        FindUsersByIdsReqDTO dto = new FindUsersByIdsReqDTO();
        // 去重处理
        dto.setIds(userIds.stream().distinct().toList());

        Response<List<FindUserByIdRspDTO>> response = userFeignApi.findByIds(dto);
        if (!response.isSuccess() || Objects.isNull(response.getData())
                || CollUtil.isEmpty(response.getData())) {
            return null;
        }

        return response.getData();
    }

}
