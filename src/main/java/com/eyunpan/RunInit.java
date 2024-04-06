package com.eyunpan;

import com.eyunpan.component.RedisComponent;
import com.eyunpan.exception.CustomException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.sql.DataSource;

@Component("runInit")
public class RunInit implements ApplicationRunner {


    private static final Logger logger = LoggerFactory.getLogger(RunInit.class);

    @Resource
    private DataSource dataSource;

    @Resource
    private RedisComponent redisComponent;

    @Override
    public void run(ApplicationArguments args) {
        try {
            //连接数据库
            dataSource.getConnection();
            redisComponent.getSysSettingsDto();
            logger.error("服务启动成功...........");
        } catch (Exception e) {
            logger.error("数据库或者redis设置失败");
            throw new CustomException("服务启动失败");
        }
    }
}
