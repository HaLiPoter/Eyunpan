package com.eyunpan.annotation;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.web.bind.annotation.Mapping;

import java.lang.annotation.*;

@Target({ElementType.METHOD,ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Mapping
public @interface MInterceptor {

    boolean checkLogin() default true;
    boolean checkParams() default false;
    boolean checkAdmin() default false;
}
