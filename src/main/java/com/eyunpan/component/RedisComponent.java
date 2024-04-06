package com.eyunpan.component;

import com.eyunpan.entity.constants.Constants;
import com.eyunpan.entity.dto.DownloadFileDto;
import com.eyunpan.entity.dto.SystemSettingDto;
import com.eyunpan.entity.dto.UserSpaceDto;
import com.eyunpan.entity.po.UserInfo;
import com.eyunpan.mappers.FileMapper;
import com.eyunpan.mappers.UserInfoMapper;
import com.eyunpan.service.FileInfoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component("redisComponent")
public class RedisComponent {

    @Resource
    private RedisUtils redisUtils;

    @Autowired
    private FileMapper fileMapper;

    @Autowired
    private UserInfoMapper userInfoMapper;

    public void setEmailCode(String key,String value){

        redisUtils.setex(Constants.REDIS_EMAIL_CODE+key,value, Constants.REDIS_KEY_EXPIRES_FIFTEEN_MIN);
    }

    public String getEmailCode(String key){
        return (String) redisUtils.get(Constants.REDIS_EMAIL_CODE+key);
    }
    public void deleteEmailCode(String key){
        redisUtils.delete(Constants.REDIS_EMAIL_CODE+key);
    }

    public SystemSettingDto getSysSettingsDto() {
        SystemSettingDto systemSettingDto = (SystemSettingDto) redisUtils.get(Constants.REDIS_KEY_SYS_SETTING);
        if(systemSettingDto==null){
            systemSettingDto = new SystemSettingDto();
            redisUtils.set(Constants.REDIS_KEY_SYS_SETTING,systemSettingDto);
        }
        return systemSettingDto;
    }

    public void setSysSettingsDto(SystemSettingDto sysSettingsDto){
        redisUtils.set(Constants.REDIS_KEY_SYS_SETTING,sysSettingsDto);
    }

    public void saveUserSpaceUse(String userId, UserSpaceDto userSpaceDto) {
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, userSpaceDto, Constants.REDIS_KEY_EXPIRES_DAY);

    }

    public UserSpaceDto getUserSpaceUse(String userId) {
        UserSpaceDto spaceDto = (UserSpaceDto) redisUtils.get(Constants.REDIS_KEY_USER_SPACE_USE + userId);
        if (null == spaceDto) {
            spaceDto = new UserSpaceDto();
            Long useSpace = this.fileMapper.selectUseSpace(userId);
            spaceDto.setUseSpace(useSpace);
            spaceDto.setTotalSpace(getSysSettingsDto().getUserInitUseSpace() * Constants.MB);
            redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
        }
        return spaceDto;
    }
    public UserSpaceDto resetUserSpaceUse(String userId) {
        UserSpaceDto spaceDto = new UserSpaceDto();
        Long useSpace = this.fileMapper.selectUseSpace(userId);
        spaceDto.setUseSpace(useSpace);

        UserInfo userInfo = this.userInfoMapper.selectById(userId);
        spaceDto.setTotalSpace(userInfo.getTotalSpace());
        redisUtils.setex(Constants.REDIS_KEY_USER_SPACE_USE + userId, spaceDto, Constants.REDIS_KEY_EXPIRES_DAY);
        return spaceDto;
    }

    public void saveDownloadCode(String code, DownloadFileDto downloadFileDto) {
        redisUtils.setex(Constants.REDIS_KEY_DOWNLOAD + code, downloadFileDto, Constants.REDIS_KEY_EXPIRES_FIVE_MIN);
    }

    public DownloadFileDto getDownloadCode(String code) {
        return (DownloadFileDto) redisUtils.get(Constants.REDIS_KEY_DOWNLOAD + code);
    }

    /**
     * 根据用户id和文件id 获取临时文件的大小
     * @param userId
     * @param fileId
     * @return
     */
    public Long getFileTempSize(String userId, String fileId) {
        Long currentSize = getFileSizeFromRedis(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId);
        return currentSize;
    }

    /**
     * 从redis获取文件临时空间
     * @param key
     * @return
     */
    private Long getFileSizeFromRedis(String key) {
        Object sizeObj = redisUtils.get(key);
        if (sizeObj == null) {
            return 0L;
        }
        if (sizeObj instanceof Integer) {
            return ((Integer) sizeObj).longValue();
        } else if (sizeObj instanceof Long) {
            return (Long) sizeObj;
        }

        return 0L;
    }

    //修改临时文件大小，增量式
    public void saveFileTempSize(String userId, String fileId, Long fileSize) {
        Long currentSize = getFileTempSize(userId, fileId);
        redisUtils.setex(Constants.REDIS_KEY_USER_FILE_TEMP_SIZE + userId + fileId, currentSize + fileSize, Constants.REDIS_KEY_EXPIRES_ONE_HOUR);
    }

    //文件分享信息

}
