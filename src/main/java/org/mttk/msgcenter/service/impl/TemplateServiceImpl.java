package org.mttk.msgcenter.service.impl;

import org.apache.commons.lang3.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.mttk.msgcenter.common.conf.SendMsgConf;
import org.mttk.msgcenter.constant.Constants;
import org.mttk.msgcenter.enums.TemplateStatus;
import org.mttk.msgcenter.mapper.TemplateMapper;
import org.mttk.msgcenter.model.dto.PageReq;
import org.mttk.msgcenter.model.entity.TemplateModel;
import org.mttk.msgcenter.model.vo.PageResult;
import org.mttk.msgcenter.service.TemplateService;
import org.mttk.msgcenter.utils.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Service
public class TemplateServiceImpl implements TemplateService {
    @Autowired
    private TemplateMapper templateMapper;

    @Autowired
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private SendMsgConf sendMsgConf;

    @Override
    public String CreateTemplate(TemplateModel template) {
        //校验参数
        //生成模板ID
        template.setTemplateId(UUID.randomUUID().toString());
        template.setRelTemplateId(UUID.randomUUID().toString());
        //设置状态
        template.setStatus(TemplateStatus.TEMPLATE_STATUS_NORMAL.getStatus());
        //存入数据库
        templateMapper.insert(template);
        //认为刚存入的模板会很快取用，存入缓存
        if (sendMsgConf.isOpenCache()) {
            String templateCacheKey = Constants.REDIS_KEY_TEMPLATE + template.getTemplateId();
            redisTemplate.opsForValue().set(templateCacheKey, JSONUtil.toJsonString(template), Duration.ofSeconds(30));
        }
        return template.getTemplateId();
    }
    @Override
    public void DeleteTemplate(String templateId) {
        LambdaQueryWrapper<TemplateModel> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TemplateModel::getTemplateId, templateId);
        templateMapper.delete(wrapper);
        if (sendMsgConf.isOpenCache()) {
            String templateCacheKey = Constants.REDIS_KEY_TEMPLATE + templateId;
            redisTemplate.delete(templateCacheKey);
        }
    }

    @Override
    public void UpdateTemplate(TemplateModel templateModel) {
        templateMapper.updateById(templateModel);
        if (sendMsgConf.isOpenCache() && templateModel.getTemplateId() != null) {
            String templateCacheKey = Constants.REDIS_KEY_TEMPLATE + templateModel.getTemplateId();
            redisTemplate.delete(templateCacheKey);
        }
    }

    @Override
    public TemplateModel GetTemplate(String templateId) {
        String templateCacheKey = Constants.REDIS_KEY_TEMPLATE + templateId;
        if (sendMsgConf.isOpenCache()) {
            String cacheTp = redisTemplate.opsForValue().get(templateCacheKey);
            if (!StringUtils.isEmpty(cacheTp)) {
                if (Constants.REDIS_CACHE_EMPTY.equals(cacheTp)) {
                    return null;
                }
                TemplateModel templateModel = JSONUtil.parseObject(cacheTp, TemplateModel.class);
                if (templateModel != null) {
                    return templateModel;
                }
            }
        }
        LambdaQueryWrapper<TemplateModel> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TemplateModel::getTemplateId, templateId);
        TemplateModel templateModel = templateMapper.selectOne(wrapper);

        if (sendMsgConf.isOpenCache()) {
            if (templateModel != null) {
                redisTemplate.opsForValue().set(templateCacheKey, JSONUtil.toJsonString(templateModel), Duration.ofSeconds(30));
            } else {
                redisTemplate.opsForValue().set(templateCacheKey, Constants.REDIS_CACHE_EMPTY, Duration.ofSeconds(10));
            }
        }
        return templateModel;
    }

    @Override
    public PageResult<TemplateModel> GetTemplateList(PageReq pageReq) {
        Page<TemplateModel> page = new Page<>(pageReq.getPageNum(), pageReq.getPageSize());
        IPage<TemplateModel> result = templateMapper.selectPage(page, null);
        PageResult<TemplateModel> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setPageNum(pageReq.getPageNum());
        pageResult.setPageSize(pageReq.getPageSize());
        return pageResult;
    }


}
