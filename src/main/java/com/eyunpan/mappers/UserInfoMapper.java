package com.eyunpan.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eyunpan.entity.po.UserInfo;
import com.eyunpan.entity.qo.UserInfoQO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
public interface UserInfoMapper extends IBaseMapper<UserInfo, UserInfoQO>{

}
