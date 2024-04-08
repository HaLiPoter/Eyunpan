package com.eyunpan.controller;

import com.eyunpan.annotation.CheckParam;
import com.eyunpan.entity.dto.SessionUserDto;
import com.eyunpan.entity.enums.DateTimePatternEnum;
import com.eyunpan.entity.po.TestPO;
import com.eyunpan.entity.po.UserInfo;
import com.eyunpan.utils.DateUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

@RestController
@RequestMapping("/test")
public class TestController extends BaseFileController{
    private static Logger logger = LoggerFactory.getLogger(TestController.class);

    @RequestMapping("/t1")
    public String test1(){
        logger.info("test/t1: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa-----------------------");
        return "This is test/t1";
    }
    @RequestMapping("/t2")
    public UserInfo test2(@RequestBody UserInfo userInfo){
        System.out.println(userInfo);
        return userInfo;
    }

    @RequestMapping("/image")
    public void test3(HttpServletRequest request, HttpServletResponse response){

        response.setContentType("image/jpg");
        response.setHeader("Cache-Control", "max-age=2592000");
        readFile(response,"C:/Users/laofang/Desktop/图片/Zombatar_1.jpg");
    }
    @RequestMapping("/t3")
    public void test4(UserInfo userInfo,String car){
        System.out.println(userInfo);
        System.out.println(car);

    }

    @RequestMapping("/t4")
    public void test4(TestPO testPO){

        System.out.println(testPO);
        long time = testPO.getJoinTime().getTime();
        System.out.println(time);
        String format = DateUtil.format(testPO.getJoinTime(), DateTimePatternEnum.YYYYMM.getPattern());
        System.out.println(format);
    }

    @RequestMapping("/ts/getVideoInfo/{fileId}")
    @CrossOrigin
    public void getVideoInfo(HttpServletResponse response, HttpSession session, @PathVariable("fileId") @CheckParam(required = true) String fileId) {
        super.getFile(response, fileId, "6261524511");
    }
}
