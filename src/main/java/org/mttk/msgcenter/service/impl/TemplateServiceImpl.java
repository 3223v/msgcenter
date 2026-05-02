package org.mttk.msgcenter.service.impl;

import org.apache.commons.lang3.StringUtils;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.mttk.msgcenter.common.conf.SendMsgConf;
import org.mttk.msgcenter.constant.Constants;
import org.mttk.msgcenter.enums.TemplateStatus;
import org.mttk.msgcenter.mapper.TemplateMapper;
import org.mttk.msgcenter.model.entity.TemplateModel;
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
        template.setStatus(TemplateStatus.TEMPLATE_STATUS_PENDING.getStatus());
        //存入数据库
        templateMapper.insert(template);
        //认为刚存入的模板会很快取用，存入缓存
        return template.getTemplateId();
    }
    @Override
    public void DeleteTemplate(String templateId) {
        LambdaQueryWrapper<TemplateModel> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TemplateModel::getTemplateId, templateId);
        templateMapper.delete(wrapper);
    }

    @Override
    public void UpdateTemplate(TemplateModel templateModel) {
        templateMapper.updateById(templateModel);
    }

    @Override
    public TemplateModel GetTemplate(String templateId) {
        String templateCacheKey = Constants.REDIS_KEY_TEMPLATE + templateId;
        String cacheTp = redisTemplate.opsForValue().get(templateCacheKey);
        TemplateModel templateModel = null;
        if (!StringUtils.isEmpty(cacheTp)&&sendMsgConf.isOpenCache()) {
            templateModel = JSONUtil.parseObject(cacheTp, TemplateModel.class);
            if(templateModel!=null) {
                return templateModel;
            }
        }
        LambdaQueryWrapper<TemplateModel> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(TemplateModel::getTemplateId, templateId);

        templateModel = templateMapper.selectOne(wrapper);

        //存入缓存
        redisTemplate.opsForValue().set(templateCacheKey,JSONUtil.toJsonString(templateModel), Duration.ofSeconds(30));
        return templateModel;

    }


}
