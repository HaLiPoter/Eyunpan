package com.eyunpan.mappers;

import com.eyunpan.entity.po.UserInfo;
import com.eyunpan.entity.qo.UserInfoQO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserInfoMapper extends IBaseMapper<UserInfo, UserInfoQO>{

}
