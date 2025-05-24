package com.puxinxiaolin.xiaolinshu.user.biz.service.impl;

import com.google.common.base.Preconditions;
import com.puxinxiaolin.framework.biz.context.holder.LoginUserContextHolder;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.framework.common.util.ParamUtils;
import com.puxinxiaolin.xiaolinshu.user.biz.domain.dataobject.UserDO;
import com.puxinxiaolin.xiaolinshu.user.biz.domain.mapper.UserDOMapper;
import com.puxinxiaolin.xiaolinshu.user.biz.enums.ResponseCodeEnum;
import com.puxinxiaolin.xiaolinshu.user.biz.enums.SexEnum;
import com.puxinxiaolin.xiaolinshu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.puxinxiaolin.xiaolinshu.user.biz.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    
    @Resource
    private UserDOMapper userDOMapper;
    
    /**
     * 更新用户信息
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> updateUserInfo(UpdateUserInfoReqVO request) {
        UserDO userDO = new UserDO();
        userDO.setId(LoginUserContextHolder.getUserId());
        boolean needUpdate = false;

        MultipartFile avatar = request.getAvatar();
        if (Objects.nonNull(avatar)) {
            // TODO [YCcLin 2025/5/24]: 调用存储服务上传文件 
        }

        String nickname = request.getNickname();
        if (StringUtils.isNotBlank(nickname)) {
            Preconditions.checkArgument(ParamUtils.checkNickName(nickname), ResponseCodeEnum.NICK_NAME_VALID_FAIL.getErrorMessage());
            
            userDO.setNickname(nickname);
            needUpdate = true;
        }
        
        String xiaolinshuId = request.getXiaohashuId();
        if (StringUtils.isNotBlank(xiaolinshuId)) {
            Preconditions.checkArgument(ParamUtils.checkXiaolinshuId(xiaolinshuId), ResponseCodeEnum.XIAOLINSHU_ID_VALID_FAIL.getErrorMessage());
            userDO.setXiaolinshuId(xiaolinshuId);
            needUpdate = true;
        }
        
        Integer sex = request.getSex();
        if (Objects.nonNull(sex)) {
            Preconditions.checkArgument(SexEnum.isValid(sex), ResponseCodeEnum.SEX_VALID_FAIL.getErrorMessage());
            userDO.setSex(sex);
            needUpdate = true;
        }
        
        LocalDate birthday = request.getBirthday();
        if (Objects.nonNull(birthday)) {
            userDO.setBirthday(birthday);
            needUpdate = true;
        }
        
        String introduction = request.getIntroduction();
        if (StringUtils.isNotBlank(introduction)) {
            Preconditions.checkArgument(ParamUtils.checkLength(introduction, 100), ResponseCodeEnum.INTRODUCTION_VALID_FAIL.getErrorMessage());
            userDO.setIntroduction(introduction);
            needUpdate = true;
        }
        
        MultipartFile backgroundImgFile = request.getBackgroundImg();
        if (Objects.nonNull(backgroundImgFile)) {
            // TODO [YCcLin 2025/5/24]: 调用存储服务上传文件 
        }

        if (needUpdate) {
            userDO.setUpdateTime(LocalDateTime.now());
            userDOMapper.updateByPrimaryKeySelective(userDO);
        }
        return Response.success();
    }
    
}
