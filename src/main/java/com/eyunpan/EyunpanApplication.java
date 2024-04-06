package com.eyunpan;

import com.eyunpan.component.RedisUtils;
import com.eyunpan.config.AppConfig;
import com.eyunpan.config.RedisConfig;
import com.eyunpan.entity.constants.Constants;
import com.eyunpan.entity.po.UserInfo;
import org.apache.ibatis.annotations.Mapper;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import javax.servlet.MultipartConfigElement;

@EnableAsync
@SpringBootApplication(scanBasePackages = {"com.eyunpan"})
@MapperScan(basePackages = {"com.eyunpan.mappers"})
@EnableTransactionManagement
@EnableScheduling
public class EyunpanApplication {

    public static void main(String[] args) {
        SpringApplication.run(EyunpanApplication.class, args);

        RedisUtils redisUtils = ApplicationContextProvider.getBean(RedisUtils.class);
        UserInfo userInfo = new UserInfo();
        userInfo.setEmail("1441414846@qq.com");
    }

    @Bean
    @DependsOn({"applicationContextProvider"})
    MultipartConfigElement multipartConfigElement() {
        AppConfig appConfig = (AppConfig) ApplicationContextProvider.getBean("appConfig");
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setLocation(appConfig.getProjectFolder() + Constants.FILE_FOLDER_TEMP);
        return factory.createMultipartConfig();
    }
}
