package com.eyunpan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eyunpan.entity.dto.SessionUserDto;
import com.eyunpan.entity.po.UserInfo;

public interface UserInfoService extends IService<UserInfo> {


    void register(String email, String nickName, String password, String emailCode);

    SessionUserDto login(String email, String password);

    void resetPwd(String email, String password, String emailCode);

    void updateUserInfoByUserId(UserInfo userInfo, String userId);
}
