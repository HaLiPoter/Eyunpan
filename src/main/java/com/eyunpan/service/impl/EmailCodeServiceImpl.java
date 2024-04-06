package com.eyunpan.service.impl;

import com.baomidou.mybatisplus.core.conditions.AbstractWrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eyunpan.component.RedisComponent;
import com.eyunpan.config.AppConfig;
import com.eyunpan.entity.constants.Constants;
import com.eyunpan.entity.dto.SystemSettingDto;
import com.eyunpan.entity.po.EmailCode;
import com.eyunpan.entity.po.UserInfo;
import com.eyunpan.exception.CustomException;
import com.eyunpan.mappers.EmailCodeMapper;
import com.eyunpan.mappers.UserInfoMapper;
import com.eyunpan.service.EmailCodeService;
import com.eyunpan.service.UserInfoService;
import com.eyunpan.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import javax.mail.internet.MimeMessage;
import java.time.OffsetDateTime;
import java.util.Date;

@Service
public class EmailCodeServiceImpl extends ServiceImpl<EmailCodeMapper, EmailCode> implements EmailCodeService {

    private static Logger logger= LoggerFactory.getLogger(EmailCodeServiceImpl.class);

    @Resource
    private AppConfig appConfig;
    @Autowired
    private EmailCodeMapper emailCodeMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    @Autowired
    private RedisComponent redisComponent;

    @Resource
    private JavaMailSender javaMailSender;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sendEmailCode(String email, Integer type) {
        if (type== Constants.ZERO){
            UserInfo userInfo = userInfoMapper.selectOne(new LambdaQueryWrapper<UserInfo>().eq(UserInfo::getEmail, email));
            if (null!=userInfo){
                throw new CustomException("账号已存在");
            }
        }
        String code = StringTools.getRandomNumber(Constants.LENGTH_5);
        sendEmailCode(email,code);
        redisComponent.setEmailCode(email,code);
    }

    @Override
    public void checkEmailCode(String email, String emailCode) {
        String code = redisComponent.getEmailCode(email);
        if (null==code){
            throw new CustomException("验证码失效或过期");
        }
        if (!code.equals(emailCode)){
            throw new CustomException("邮箱验证码错误");
        }
        redisComponent.deleteEmailCode(email);
    }

    private void sendEmailCode(String email,String code){
        try {
            MimeMessage message = javaMailSender.createMimeMessage();

            MimeMessageHelper helper = new MimeMessageHelper(message, true);
            //邮件发件人
            helper.setFrom(appConfig.getSendUserName());
            //邮件收件人 1或多个
            helper.setTo(email);

            SystemSettingDto sysSettingsDto = redisComponent.getSysSettingsDto();

            //邮件主题
            helper.setSubject(sysSettingsDto.getRegisterEmailTitle());
            //邮件内容
            helper.setText(String.format(sysSettingsDto.getRegisterEmailContent(), code));
            //邮件发送时间
            helper.setSentDate(new Date());
            javaMailSender.send(message);
        } catch (Exception e) {
            logger.error("邮件发送失败", e);
            throw new CustomException("邮件发送失败");
        }
    }
}
