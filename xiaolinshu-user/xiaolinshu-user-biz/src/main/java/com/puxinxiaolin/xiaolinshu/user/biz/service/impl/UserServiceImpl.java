package com.puxinxiaolin.xiaolinshu.user.biz.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.google.common.base.Preconditions;
import com.puxinxiaolin.framework.biz.context.holder.LoginUserContextHolder;
import com.puxinxiaolin.framework.common.enums.DeletedEnum;
import com.puxinxiaolin.framework.common.enums.StatusEnum;
import com.puxinxiaolin.framework.common.exception.BizException;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.framework.common.util.JsonUtils;
import com.puxinxiaolin.framework.common.util.ParamUtils;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.FindUserByIdReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.FindUserByPhoneReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.RegisterUserReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.req.UpdateUserPasswordReqDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByIdRspDTO;
import com.puxinxiaolin.xiaolinshu.user.api.dto.resp.FindUserByPhoneRspDTO;
import com.puxinxiaolin.xiaolinshu.user.biz.constant.RedisKeyConstants;
import com.puxinxiaolin.xiaolinshu.user.biz.constant.RoleConstants;
import com.puxinxiaolin.xiaolinshu.user.biz.domain.dataobject.RoleDO;
import com.puxinxiaolin.xiaolinshu.user.biz.domain.dataobject.UserDO;
import com.puxinxiaolin.xiaolinshu.user.biz.domain.dataobject.UserRoleDO;
import com.puxinxiaolin.xiaolinshu.user.biz.domain.mapper.RoleDOMapper;
import com.puxinxiaolin.xiaolinshu.user.biz.domain.mapper.UserDOMapper;
import com.puxinxiaolin.xiaolinshu.user.biz.domain.mapper.UserRoleDOMapper;
import com.puxinxiaolin.xiaolinshu.user.biz.enums.ResponseCodeEnum;
import com.puxinxiaolin.xiaolinshu.user.biz.enums.SexEnum;
import com.puxinxiaolin.xiaolinshu.user.biz.model.vo.UpdateUserInfoReqVO;
import com.puxinxiaolin.xiaolinshu.user.biz.rpc.DistributedIdGeneratorRpcService;
import com.puxinxiaolin.xiaolinshu.user.biz.rpc.OssRpcService;
import com.puxinxiaolin.xiaolinshu.user.biz.service.UserService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class UserServiceImpl implements UserService {
    @Resource
    private UserDOMapper userDOMapper;
    @Resource
    private OssRpcService ossRpcService;
    @Resource
    private UserRoleDOMapper userRoleDOMapper;
    @Resource
    private RoleDOMapper roleDOMapper;
    @Resource
    private RedisTemplate<String, Object> redisTemplate;
    @Resource(name = "taskExecutor")
    private ThreadPoolTaskExecutor threadPoolTaskExecutor;
    @Resource
    private DistributedIdGeneratorRpcService distributedIdGeneratorRpcService;

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
            // 调用存储服务上传文件 
            String avatarUrl = ossRpcService.uploadFile(avatar);

            log.info("==> 调用 oss 服务成功, 上传头像, url: {}", avatarUrl);
            if (StringUtils.isBlank(avatarUrl)) {
                throw new BizException(ResponseCodeEnum.UPLOAD_AVATAR_FAIL);
            }

            userDO.setAvatar(avatarUrl);
            needUpdate = true;
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
            // 调用存储服务上传文件 
            String backgroundImgUrl = ossRpcService.uploadFile(backgroundImgFile);

            log.info("==> 调用 oss 服务成功, 上传背景图, url: {}", backgroundImgUrl);
            if (StringUtils.isBlank(backgroundImgUrl)) {
                throw new BizException(ResponseCodeEnum.UPLOAD_BACKGROUND_IMG_FAIL);
            }

            userDO.setBackgroundImg(backgroundImgUrl);
            needUpdate = true;
        }

        if (needUpdate) {
            userDO.setUpdateTime(LocalDateTime.now());
            userDOMapper.updateByPrimaryKeySelective(userDO);
        }
        return Response.success();
    }

    /**
     * 用户注册
     *
     * @param request
     * @return
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Response<Long> register(RegisterUserReqDTO request) {
        String phone = request.getPhone();

        UserDO exist = userDOMapper.selectByPhone(phone);
        log.info("==> 用户是否注册, phone: {}, userDO: {}", phone, JsonUtils.toJsonString(exist));

        if (Objects.nonNull(exist)) {
            return Response.success(exist.getId());
        }

        // 走 rpc 拿分布式 ID, 替换原有的 redis 的 ID 自增器
        String xiaolinshuId = distributedIdGeneratorRpcService.getXiaolinshuId();
        String userIdStr = distributedIdGeneratorRpcService.getUserId();

        UserDO userDO = UserDO.builder()
                .id(Long.valueOf(userIdStr))
                .phone(phone)
                .xiaolinshuId(xiaolinshuId)
                .nickname("小林薯" + xiaolinshuId)
                .status(StatusEnum.ENABLE.getValue())
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDeleted(DeletedEnum.NO.getValue())
                .build();
        userDOMapper.insert(userDO);

        Long userId = userDO.getId();
        UserRoleDO userRoleDO = UserRoleDO.builder()
                .userId(userId)
                .roleId(RoleConstants.COMMON_USER_ROLE_ID)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .isDeleted(DeletedEnum.NO.getValue())
                .build();
        userRoleDOMapper.insert(userRoleDO);

        RoleDO roleDO = roleDOMapper.selectByPrimaryKey(RoleConstants.COMMON_USER_ROLE_ID);

        List<String> roles = new ArrayList<>(1);
        roles.add(roleDO.getRoleKey());

        String userRolesKey = RedisKeyConstants.buildUserRoleKey(userId);
        redisTemplate.opsForValue()
                .set(userRolesKey, JsonUtils.toJsonString(roles));

        return Response.success(userId);
    }

    /**
     * 根据手机号查找用户
     *
     * @param request
     * @return
     */
    @Override
    public Response<FindUserByPhoneRspDTO> findByPhone(FindUserByPhoneReqDTO request) {
        String phone = request.getPhone();

        UserDO exist = userDOMapper.selectByPhone(phone);
        if (Objects.isNull(exist)) {
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        FindUserByPhoneRspDTO result = FindUserByPhoneRspDTO.builder()
                .id(exist.getId())
                .password(exist.getPassword())
                .build();
        return Response.success(result);
    }

    /**
     * 修改密码
     *
     * @param request
     * @return
     */
    @Override
    public Response<?> updatePassword(UpdateUserPasswordReqDTO request) {
        Long userId = LoginUserContextHolder.getUserId();

        UserDO userDO = UserDO.builder()
                .id(userId)
                .password(request.getEncodePassword())
                .updateTime(LocalDateTime.now())
                .build();

        userDOMapper.updateByPrimaryKeySelective(userDO);
        return Response.success();
    }

    /**
     * 根据用户 ID 查询用户信息
     *
     * @param request
     * @return
     * @result 550ms左右 -> 10ms以内
     */
    @Override
    public Response<FindUserByIdRspDTO> findById(FindUserByIdReqDTO request) {
        Long userId = request.getId();

        String key = RedisKeyConstants.buildUserInfoKey(userId);
        String value = (String) redisTemplate.opsForValue().get(key);
        if (StringUtils.isNotBlank(value)) {
            FindUserByIdRspDTO findUserByIdRspDTO = JsonUtils.parseObject(value, FindUserByIdRspDTO.class);
            return Response.success(findUserByIdRspDTO);
        }

        UserDO userDO = userDOMapper.selectByPrimaryKey(userId);
        if (Objects.isNull(userDO)) {
            throw new BizException(ResponseCodeEnum.USER_NOT_FOUND);
        }

        FindUserByIdRspDTO findUserByIdRspDTO = FindUserByIdRspDTO.builder()
                .id(userDO.getId())
                .nickName(userDO.getNickname())
                .avatar(userDO.getAvatar())
                .build();

        // 走异步存 redis
        threadPoolTaskExecutor.submit(() -> {
            // 过期时间（保底1天 + 随机秒数, 将缓存过期时间打散, 防止同一时间大量缓存失效）
            long expireSeconds = 60 * 60 * 24 + RandomUtil.randomInt(60 * 60 * 24);

            redisTemplate.opsForValue()
                    .set(key, JsonUtils.toJsonString(findUserByIdRspDTO), expireSeconds, TimeUnit.SECONDS);
        });

        return Response.success(findUserByIdRspDTO);
    }

}
