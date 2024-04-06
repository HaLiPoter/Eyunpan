package com.eyunpan.controller;

import com.eyunpan.annotation.CheckParam;
import com.eyunpan.annotation.MInterceptor;
import com.eyunpan.component.RedisComponent;
import com.eyunpan.config.AppConfig;
import com.eyunpan.entity.constants.Constants;
import com.eyunpan.entity.dto.ImageCodeCreater;
import com.eyunpan.entity.dto.SessionUserDto;
import com.eyunpan.entity.po.UserInfo;
import com.eyunpan.entity.vo.ResponseVO;
import com.eyunpan.exception.CustomException;
import com.eyunpan.service.EmailCodeService;
import com.eyunpan.service.FileInfoService;
import com.eyunpan.service.UserInfoService;
import com.eyunpan.utils.StringTools;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

@RestController("accountController")
public class AccountController extends IBaseController{

    private static Logger logger= LoggerFactory.getLogger(AccountController.class);

    @Autowired
    private UserInfoService userInfoService;

    @Autowired
    private FileInfoService fileInfoService;

    @Autowired
    private EmailCodeService emailCodeService;

    @Autowired
    private AppConfig appConfig;

    @Autowired
    private RedisComponent redisComponent;

    @RequestMapping("/checkCode")
    public void checkCode(HttpServletResponse response, HttpSession session, Integer type) throws
            IOException {
        ImageCodeCreater vCode = new ImageCodeCreater(130, 38, 5, 10);
        response.setHeader("Pragma", "no-cache");
        response.setHeader("Cache-Control", "no-cache");
        response.setDateHeader("Expires", 0);
        response.setContentType("image/jpeg");
        String code = vCode.getCode();
        if (type == null || type == 0) {
            session.setAttribute(Constants.CHECK_CODE_KEY, code);
        } else {
            session.setAttribute(Constants.CHECK_CODE_KEY_EMAIL, code);
        }
        vCode.write(response.getOutputStream());
    }

    @RequestMapping("/sendEmailCode")
    @MInterceptor(checkLogin = false,checkParams = true)
    public ResponseVO sendEmailCode(HttpSession session,
                                    @CheckParam(required = true) String email,
                                    @CheckParam(required = true) String checkCode,
                                    @CheckParam(required = true) Integer type){
        try {
            if(!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY_EMAIL))){
                throw new CustomException("图片验证码错误");
            }
            emailCodeService.sendEmailCode(email,type);
            return getSuccessResponseVO(null);
        }finally {

            session.removeAttribute(Constants.CHECK_CODE_KEY_EMAIL);
        }
    }

    @RequestMapping("/register")
    @MInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO register(HttpSession session,
                               @CheckParam(required = true) String email,
                               @CheckParam(required = true) String nickName,
                               @CheckParam(required = true) String password,
                               @CheckParam(required = true) String checkCode,
                               @CheckParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new CustomException("图片验证码不正确");
            }
            userInfoService.register(email, nickName, password, emailCode);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @RequestMapping("/login")
    @MInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO login(HttpSession session, HttpServletRequest request,
                            @CheckParam(required = true) String email,
                            @CheckParam(required = true) String password,
                            @CheckParam(required = true) String checkCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new CustomException("图片验证码不正确");
            }
            SessionUserDto sessionWebUserDto = userInfoService.login(email, password);
            session.setAttribute(Constants.SESSION_KEY, sessionWebUserDto);
            return getSuccessResponseVO(sessionWebUserDto);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @RequestMapping("/resetPwd")
    @MInterceptor(checkLogin = false, checkParams = true)
    public ResponseVO resetPwd(HttpSession session,
                               @CheckParam(required = true) String email,
                               @CheckParam(required = true) String password,
                               @CheckParam(required = true) String checkCode,
                               @CheckParam(required = true) String emailCode) {
        try {
            if (!checkCode.equalsIgnoreCase((String) session.getAttribute(Constants.CHECK_CODE_KEY))) {
                throw new CustomException("图片验证码不正确");
            }
            userInfoService.resetPwd(email, password, emailCode);
            return getSuccessResponseVO(null);
        } finally {
            session.removeAttribute(Constants.CHECK_CODE_KEY);
        }
    }

    @RequestMapping("/getAvatar/{userId}")
    @MInterceptor(checkLogin = false, checkParams = true)
    public void getAvatar(HttpServletResponse response, @CheckParam(required = true) @PathVariable("userId") String userId) {
        String avatarFolderName = Constants.FILE_FOLDER_FILE + Constants.FILE_FOLDER_AVATAR_NAME;
        String avatarFolderNameReal=appConfig.getProjectFolder() + avatarFolderName;
        File folder = new File(avatarFolderNameReal);
        if (!folder.exists()) {
            folder.mkdirs();
        }

        String avatarPath = avatarFolderNameReal + userId + Constants.AVATAR_SUFFIX;
        File file = new File(avatarPath);
        if (!file.exists()) {
            if (!new File(appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT).exists()) {
                printNoDefaultImage(response);
                return;
            }
            avatarPath = appConfig.getProjectFolder() + avatarFolderName + Constants.AVATAR_DEFUALT;
        }
        response.setContentType("image/jpg");
        readFile(response, avatarPath);
    }

    @RequestMapping("/updateUserAvatar")
    @MInterceptor
    public ResponseVO updateUserAvatar(HttpSession session, MultipartFile avatar) {
        SessionUserDto sessionUserDto = getUserInfoFromSession(session);
        String baseFolder = appConfig.getProjectFolder() + Constants.FILE_FOLDER_FILE;
        File targetFileFolder = new File(baseFolder + Constants.FILE_FOLDER_AVATAR_NAME);
        if (!targetFileFolder.exists()) {
            targetFileFolder.mkdirs();
        }
        File targetFile = new File(targetFileFolder.getPath() + "/" + sessionUserDto.getUserId() + Constants.AVATAR_SUFFIX);
        try {
            avatar.transferTo(targetFile);
        } catch (Exception e) {
            logger.error("上传头像失败", e);
        }

        return getSuccessResponseVO(null);
    }

    private void printNoDefaultImage(HttpServletResponse response) {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpStatus.OK.value());
        PrintWriter writer = null;
        try {
            writer = response.getWriter();
            writer.print("请在头像目录下放置默认头像default_avatar.jpg");
            writer.close();
        } catch (Exception e) {
            logger.error("printNoDefaultImage error", e);
        } finally {
            writer.close();
        }
    }

    @RequestMapping("/getUseSpace")
    @MInterceptor
    public ResponseVO getUseSpace(HttpSession session) {
        SessionUserDto sessionWebUserDto = getUserInfoFromSession(session);
        return getSuccessResponseVO(redisComponent.getUserSpaceUse(sessionWebUserDto.getUserId()));
    }

    @RequestMapping("/logout")
    public ResponseVO logout(HttpSession session) {
        session.invalidate();
        return getSuccessResponseVO(null);
    }

    @RequestMapping("/updatePassword")
    @MInterceptor(checkParams = true)
    public ResponseVO updatePassword(HttpSession session, @CheckParam(required = true) String password) {
        SessionUserDto sessionWebUserDto = getUserInfoFromSession(session);
        UserInfo userInfo = new UserInfo();
        userInfo.setPassword(StringTools.encodeByMD5(password));
        userInfoService.updateUserInfoByUserId(userInfo, sessionWebUserDto.getUserId());
        return getSuccessResponseVO(null);
    }
}
