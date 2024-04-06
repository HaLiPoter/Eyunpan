package com.eyunpan.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.eyunpan.entity.po.EmailCode;

public interface EmailCodeService extends IService<EmailCode> {

    void sendEmailCode(String email, Integer type);

    void checkEmailCode(String email, String emailCode);
}
