package com.eyunpan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eyunpan.entity.dto.SessionUserDto;
import com.eyunpan.entity.po.UserInfo;
import com.eyunpan.entity.qo.UserInfoQO;
import com.eyunpan.entity.vo.PaginationResultVO;

import java.util.List;

public interface UserInfoService extends IService<UserInfo> {


    void register(String email, String nickName, String password, String emailCode);

    SessionUserDto login(String email, String password);

    void resetPwd(String email, String password, String emailCode);

    void updateUserInfoByUserId(UserInfo userInfo, String userId);

    List<UserInfo> getListByQO(UserInfoQO userInfoQO);

    PaginationResultVO<UserInfo> getListByPage(UserInfoQO qo);

    void updateUserStatus(String userId, Integer status);

    void changeUserSpace(String userId, Integer changeSpace);

}
