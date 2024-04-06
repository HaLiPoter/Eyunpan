package com.eyunpan.mappers;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.eyunpan.entity.po.EmailCode;
import com.eyunpan.entity.qo.EmailCodeQO;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
public interface EmailCodeMapper extends IBaseMapper<EmailCode, EmailCodeQO>  {
}
