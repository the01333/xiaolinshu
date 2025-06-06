package com.puxinxiaolin.xiaolinshu.user.relation.biz.rpc;

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
     * 根据用户 ID 查询
     *
     * @param userId
     * @return
     */
    public FindUserByIdRspDTO findById(Long userId) {
        FindUserByIdReqDTO findUserByIdReqDTO = new FindUserByIdReqDTO();
        findUserByIdReqDTO.setId(userId);

        Response<FindUserByIdRspDTO> response = userFeignApi.findById(findUserByIdReqDTO);

        if (!response.isSuccess() || Objects.isNull(response.getData())) {
            return null;
        }

        return response.getData();
    }

    /**
     * 批量获取用户信息
     *
     * @param userIds
     * @return
     */
    public List<FindUserByIdRspDTO> findByIds(List<Long> userIds) {
        FindUsersByIdsReqDTO dto = new FindUsersByIdsReqDTO();
        dto.setIds(userIds);

        Response<List<FindUserByIdRspDTO>> response = userFeignApi.findByIds(dto);

        if (!response.isSuccess() || Objects.isNull(response.getData())
                || CollUtil.isEmpty(response.getData())) {
            return null;
        }

        return response.getData();
    }

}

