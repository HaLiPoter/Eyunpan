package com.eyunpan.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.eyunpan.entity.constants.Constants;
import com.eyunpan.entity.dto.SessionShareDto;
import com.eyunpan.entity.enums.PageSize;
import com.eyunpan.entity.enums.ResponseCodeEnum;
import com.eyunpan.entity.enums.ShareExpireTimeEnums;
import com.eyunpan.entity.po.FileInfo;
import com.eyunpan.entity.po.FileShareInfo;
import com.eyunpan.entity.po.UserInfo;
import com.eyunpan.entity.qo.FileShareQO;
import com.eyunpan.entity.qo.SimplePage;
import com.eyunpan.entity.vo.PaginationResultVO;
import com.eyunpan.exception.CustomException;
import com.eyunpan.mappers.FileShareMapper;
import com.eyunpan.mappers.UserInfoMapper;
import com.eyunpan.service.FileShareService;
import com.eyunpan.service.UserInfoService;
import com.eyunpan.utils.DateUtil;
import com.eyunpan.utils.StringTools;
import com.eyunpan.utils.WrapperFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
public class FileShareServiceImpl extends ServiceImpl<FileShareMapper, FileShareInfo> implements FileShareService {

    @Autowired
    private FileShareMapper fileShareMapper;
    @Override
    public PaginationResultVO findListByPage(FileShareQO param) {
        Integer count = findCountByParam(param);
        int pageSize = param.getPageSize() == null ? PageSize.SIZE15.getSize() : param.getPageSize();

        SimplePage page = new SimplePage(param.getPageNo(), count, pageSize);
        param.setSimplePage(page);
        List<FileShareInfo> list = this.findListByParam(param);
        PaginationResultVO<FileShareInfo> result = new PaginationResultVO(count, page.getPageSize(), page.getPageNo(), page.getPageTotal(), list);
        return result;
    }

    @Override
    public List<FileShareInfo> findListByParam(FileShareQO param) {
        return fileShareMapper.IselectList(param);
    }

    @Override
    public void deleteFileShareBatch(String[] split, String userId) {
        LambdaQueryWrapper<FileShareInfo> queryWrapper = WrapperFactory.fileShareInfoQueryWrapper();
        queryWrapper.eq(FileShareInfo::getUserId,userId);
        queryWrapper.in(FileShareInfo::getShareId,Arrays.asList(split));
        remove(queryWrapper);
    }

    @Override
    public SessionShareDto checkShareCode(String shareId, String code) {
        FileShareInfo share = getById(shareId);
        if (null == share || (share.getExpireTime() != null && new Date().after(share.getExpireTime()))) {
            throw new CustomException(ResponseCodeEnum.CODE_902);
        }
        if (!share.getCode().equals(code)) {
            throw new CustomException("提取码错误");
        }

        //更新文件的浏览次数
        LambdaUpdateWrapper<FileShareInfo> updateWrapper = WrapperFactory.fileShareInfoUpdateWrapper();
        updateWrapper.setSql("show_count=show_count+1");
        updateWrapper.eq(FileShareInfo::getShareId,shareId);
        update(updateWrapper);

        SessionShareDto shareSessionDto = new SessionShareDto();
        shareSessionDto.setShareId(shareId);
        shareSessionDto.setShareUserId(share.getUserId());
        shareSessionDto.setFileId(share.getFileId());
        shareSessionDto.setExpireTime(share.getExpireTime());
        return shareSessionDto;
    }

    @Override
    public void saveShareFile(FileShareInfo share) {
        ShareExpireTimeEnums type = ShareExpireTimeEnums.getByType(share.getValidType());
        if (null==type){
            throw new CustomException(ResponseCodeEnum.CODE_600);
        }
        if (type!=ShareExpireTimeEnums.FOREVER){
            share.setExpireTime(DateUtil.getAfterDate(type.getDays()));
        }
        Date date = new Date();
        share.setShareTime(date);
        if (StringTools.isEmpty(share.getCode())){
            share.setCode(StringTools.getRandomString(Constants.LENGTH_5));
        }
        share.setShareId(StringTools.getRandomNumber(Constants.LENGTH_15));
        save(share);
    }

    public Integer findCountByParam(FileShareQO param){
        return fileShareMapper.IselectCount(param);
    }
}
