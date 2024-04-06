package com.eyunpan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eyunpan.component.RedisComponent;
import com.eyunpan.config.AppConfig;
import com.eyunpan.entity.constants.Constants;
import com.eyunpan.entity.dto.SessionUserDto;
import com.eyunpan.entity.dto.SystemSettingDto;
import com.eyunpan.entity.dto.UserSpaceDto;
import com.eyunpan.entity.enums.PageSize;
import com.eyunpan.entity.enums.UserStatusEnum;
import com.eyunpan.entity.po.UserInfo;
import com.eyunpan.entity.qo.SimplePage;
import com.eyunpan.entity.qo.UserInfoQO;
import com.eyunpan.entity.vo.PaginationResultVO;
import com.eyunpan.exception.CustomException;
import com.eyunpan.mappers.UserInfoMapper;
import com.eyunpan.service.EmailCodeService;
import com.eyunpan.service.FileInfoService;
import com.eyunpan.service.UserInfoService;
import com.eyunpan.utils.StringTools;
import com.eyunpan.utils.WrapperFactory;
import org.apache.commons.lang3.ArrayUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.*;
import java.util.Date;
import java.util.List;

@Service
public class UserInfoServiceImpl extends ServiceImpl<UserInfoMapper, UserInfo> implements UserInfoService {

    @Autowired
    private EmailCodeService emailCodeService;

    @Autowired
    private RedisComponent redisComponent;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    @Lazy
    private FileInfoService fileInfoService;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void register(String email, String nickName, String password, String emailCode) {
        UserInfo userInfo = getOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getEmail, email));
        if (null!=userInfo){
            throw new CustomException("账号已存在");
        }
        emailCodeService.checkEmailCode(email,emailCode);
        String user_id = StringTools.getRandomNumber(Constants.LENGTH_10);
        userInfo= new UserInfo();
        userInfo.setUserId(user_id);
        userInfo.setNickName(nickName);
        userInfo.setEmail(email);
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfo.setJoinTime(new Date());//注册时间
        userInfo.setStatus(UserStatusEnum.ENABLE.getStatus());
        SystemSettingDto sysSettingsDto = redisComponent.getSysSettingsDto();//获取系统设置
        userInfo.setTotalSpace(sysSettingsDto.getUserInitUseSpace() * Constants.MB);//设置默认最大空间
        userInfo.setUseSpace(0L);//设置使用空间
        save(userInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SessionUserDto login(String email, String password) {
        UserInfo userInfo = getOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getEmail, email));
        if (null==userInfo||!userInfo.getPassword().equals(password)){
            throw new CustomException("账号或密码错误");
        }
        if (UserStatusEnum.DISABLE.getStatus().equals(userInfo.getStatus())) {
            throw new CustomException("账号已被禁用");
        }
        userInfo.setLastLoginTime(new Date());
        updateById(userInfo);
        SessionUserDto sessionUserDto = new SessionUserDto();
        sessionUserDto.setNickName(userInfo.getNickName());
        sessionUserDto.setUserId(userInfo.getUserId());
        if (ArrayUtils.contains(appConfig.getAdminEmails().split(","), email)) {
            sessionUserDto.setIsAdmin(true);
        } else {
            sessionUserDto.setIsAdmin(false);
        }
        //每次登录重新设置redis的用户空间信息，从mysql中计算获得
        UserSpaceDto userSpaceDto = new UserSpaceDto();
        userSpaceDto.setUseSpace(fileInfoService.getUserUseSpace(userInfo.getUserId()));
        userSpaceDto.setTotalSpace(userInfo.getTotalSpace());
        redisComponent.saveUserSpaceUse(userInfo.getUserId(), userSpaceDto);
        return sessionUserDto;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPwd(String email, String password, String emailCode) {
        UserInfo userInfo = getOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getEmail, email));
        if (null == userInfo) {
            throw new CustomException("邮箱账号不存在");
        }
        //校验邮箱验证码
        emailCodeService.checkEmailCode(email, emailCode);

        UserInfo updateInfo = new UserInfo();
        updateInfo.setPassword(StringTools.encodeByMD5(password));
        update(updateInfo,new LambdaUpdateWrapper<UserInfo>().eq(UserInfo::getEmail,email));
    }

    @Override
    public void updateUserInfoByUserId(UserInfo userInfo, String userId) {
        update(userInfo,new LambdaUpdateWrapper<UserInfo>().eq(UserInfo::getUserId,userId));
    }

    @Override
    public List<UserInfo> getListByQO(UserInfoQO userInfoQO) {
        return userInfoMapper.IselectList(userInfoQO);
    }

    @Override
    public PaginationResultVO<UserInfo> getListByPage(UserInfoQO qo) {
        int count = userInfoMapper.IselectCount(qo);
        int pageSize = qo.getPageSize() == null ? PageSize.SIZE15.getSize() : qo.getPageSize();

        SimplePage page = new SimplePage(qo.getPageNo(), count, pageSize);
        qo.setSimplePage(page);
        List<UserInfo> list = userInfoMapper.IselectList(qo);
        PaginationResultVO<UserInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    @Override
    public void updateUserStatus(String userId, Integer status) {
        LambdaUpdateWrapper<UserInfo> updateWrapper = WrapperFactory.userInfoUpdateWrapper();
        updateWrapper.eq(UserInfo::getUserId,userId);
        updateWrapper.set(UserInfo::getStatus,status);
        update(updateWrapper);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeUserSpace(String userId, Integer changeSpace) {
        long space = changeSpace*Constants.MB;
        UserInfo userInfo = getById(userId);
        LambdaUpdateWrapper<UserInfo> updateWrapper = WrapperFactory.userInfoUpdateWrapper();
        long nxtTotal=space+userInfo.getTotalSpace();
        if (nxtTotal<userInfo.getUseSpace()){
            throw new CustomException("修改用户空间失败");
        }
        updateWrapper.set(UserInfo::getTotalSpace,nxtTotal);
        updateWrapper.eq(UserInfo::getUserId,userId);
        update(updateWrapper);
        redisComponent.resetUserSpaceUse(userId);
    }
}
