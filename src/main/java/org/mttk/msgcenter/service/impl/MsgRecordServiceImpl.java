package org.mttk.msgcenter.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mttk.msgcenter.common.conf.SendMsgConf;
import org.mttk.msgcenter.constant.Constants;
import org.mttk.msgcenter.enums.MsgStatus;
import org.mttk.msgcenter.mapper.MsgRecordMapper;
import org.mttk.msgcenter.model.dto.PageReq;
import org.mttk.msgcenter.model.dto.SendMsgReq;
import org.mttk.msgcenter.model.entity.MsgRecordModel;
import org.mttk.msgcenter.model.entity.TemplateModel;
import org.mttk.msgcenter.model.vo.PageResult;
import org.mttk.msgcenter.service.MsgRecordService;
import org.mttk.msgcenter.utils.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Slf4j
@Service
public class MsgRecordServiceImpl implements MsgRecordService {
    @Autowired
    private MsgRecordMapper mapper;

    @Resource
    private RedisTemplate<String,String> redisTemplate;

    @Autowired
    private SendMsgConf sendMsgConf;
    @Override
    public MsgRecordModel GetMsgRecordWithCache(String msgId){
        String msgRecordCacheKey = Constants.REDIS_KEY_MES_RECORD+msgId;
        if(sendMsgConf.isOpenCache()){
            String cacheMr = redisTemplate.opsForValue().get(msgRecordCacheKey);
            if(!StringUtils.isEmpty(cacheMr)){
                if(Constants.REDIS_CACHE_EMPTY.equals(cacheMr)){
                    return null;
                }
                MsgRecordModel mr = JSONUtil.parseObject(cacheMr,MsgRecordModel.class);
                if(mr != null){
                    return mr;
                }
            }
        }
        LambdaQueryWrapper<MsgRecordModel> wrapper = Wrappers.lambdaQuery();
        wrapper.eq(MsgRecordModel::getMsgId, msgId);

        MsgRecordModel mr = mapper.selectOne(wrapper);

        if(sendMsgConf.isOpenCache()){
            if(mr != null){
                redisTemplate.opsForValue().set(msgRecordCacheKey, JSONUtil.toJsonString(mr), Duration.ofSeconds(30));
            }else{
                redisTemplate.opsForValue().set(msgRecordCacheKey, Constants.REDIS_CACHE_EMPTY, Duration.ofSeconds(10));
            }
        }
        return mr;
    }
    @Override
    public void CreateMsgRecord(String msgId, SendMsgReq sendMsgReq, TemplateModel tp, MsgStatus status){
        // 6. 存储消息发送记录
        MsgRecordModel msgRd = new MsgRecordModel();
        msgRd.setMsgId(msgId);
        msgRd.setTo(sendMsgReq.getTo());
        msgRd.setSubject(sendMsgReq.getSubject());
        msgRd.setTemplateId(sendMsgReq.getTemplateId());
        msgRd.setTemplateData(JSONUtil.toJsonString(sendMsgReq.getTemplateData()));
        msgRd.setSourceId(tp.getSourceId());
        msgRd.setChannel(tp.getChannel());
        msgRd.setStatus(status.getStatus());
        msgRd.setRetryCount(0);
        try{
            mapper.insert(msgRd);
        }catch (Exception e){
            log.error("存储消息发送记录失败， msgId",msgRd.getMsgId());
        }
    }
    @Override
    public void UpdateOrCreateMsgRecord(String msgId,SendMsgReq sendMsgReq, TemplateModel tp, MsgStatus status){

        MsgRecordModel msgRd = mapper.selectOne(new LambdaQueryWrapper<MsgRecordModel>().eq(MsgRecordModel::getMsgId, msgId));

        if(msgRd == null){
            msgRd = new MsgRecordModel();
            msgRd.setMsgId(msgId);
            msgRd.setTo(sendMsgReq.getTo());
            msgRd.setSubject(sendMsgReq.getSubject());
            msgRd.setTemplateId(sendMsgReq.getTemplateId());
            msgRd.setTemplateData(JSONUtil.toJsonString(sendMsgReq.getTemplateData()));
            msgRd.setSourceId(tp.getSourceId());
            msgRd.setChannel(tp.getChannel());
            msgRd.setStatus(status.getStatus());
            msgRd.setRetryCount(0);
            try{
                mapper.insert(msgRd);
            }catch (Exception e){
                log.error("存储消息发送记录失败， msgId",msgRd.getMsgId());
            }
        }else{
            try{
                // 1. 创建更新构造器
                LambdaUpdateWrapper<MsgRecordModel> wrapper = Wrappers.lambdaUpdate();

                // 2. 指定 WHERE 条件
                wrapper.eq(MsgRecordModel::getMsgId, msgId);

                // 3. 指定要更新的字段（想更几个就更几个）
                wrapper.set(MsgRecordModel::getStatus, status.getStatus());

                // 4. 执行更新
                mapper.update(null, wrapper);
            }catch (Exception e){
                log.error("更新消息发送记录状态失败， msgId,status",msgRd.getMsgId(),status);
            }
        }
    }

    @Override
    public PageResult<MsgRecordModel> GetMsgRecordList(PageReq pageReq) {
        Page<MsgRecordModel> page = new Page<>(pageReq.getPageNum(), pageReq.getPageSize());
        IPage<MsgRecordModel> result = mapper.selectPage(page, null);
        PageResult<MsgRecordModel> pageResult = new PageResult<>();
        pageResult.setRecords(result.getRecords());
        pageResult.setTotal(result.getTotal());
        pageResult.setPageNum(pageReq.getPageNum());
        pageResult.setPageSize(pageReq.getPageSize());
        return pageResult;
    }
}
