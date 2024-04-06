package com.eyunpan.service;

import com.baomidou.mybatisplus.extension.service.IService;

public interface EmailCodeService{

    void sendEmailCode(String email, Integer type);

    void checkEmailCode(String email, String emailCode);
}
