package com.eyunpan.mappers;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;


interface IBaseMapper<T, P> extends BaseMapper<T> {

	 Integer IinsertOrUpdate(@Param("bean") T t);
	 Integer IinsertBatch(@Param("list") List<T> list);
	 Integer IinsertOrUpdateBatch(@Param("list") List<T> list);
	 List<T> IselectList(@Param("query") P p);


	 Integer IselectCount(@Param("query") P p);
}
