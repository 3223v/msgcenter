package org.mttk.msgcenter.manager.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mttk.msgcenter.constant.Constants;
import org.mttk.msgcenter.enums.MsgStatus;
import org.mttk.msgcenter.enums.PriorityEnum;
import org.mttk.msgcenter.manager.SendMsgManager;
import org.mttk.msgcenter.mapper.MsgQueueTimerMapper;
import org.mttk.msgcenter.model.dto.SendMsgReq;
import org.mttk.msgcenter.model.entity.MsgQueueModel;
import org.mttk.msgcenter.model.entity.MsgQueueTimerModel;
import org.mttk.msgcenter.redis.TimerMsgCache;
import org.mttk.msgcenter.sql2mp.MsgQueueService;
import org.mttk.msgcenter.utils.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Slf4j
@Service
public class SendMsgManagerImpl implements SendMsgManager {
    @Autowired
    MsgQueueTimerMapper msgQueueTimerMapper;

    @Autowired
    MsgQueueService msgQueueService;

    @Autowired
    private KafkaTemplate<String, Object> kafkaTemplate;

    @Autowired
    TimerMsgCache timerMsgCache;
    @Override
    public String SendToMysql(SendMsgReq sendMsgReq) {
        // 1. 生成消息ID
        MsgQueueModel mqm = new MsgQueueModel();
        if(StringUtils.isEmpty(sendMsgReq.getMsgID())){
            sendMsgReq.setMsgID(UUID.randomUUID().toString());
        }

        // 2. 构建中转实体类
        mqm.setMsgId(sendMsgReq.getMsgID());
        mqm.setSubject(sendMsgReq.getSubject());
        mqm.setTo(sendMsgReq.getTo());
        mqm.setPriority(sendMsgReq.getPriority());
        mqm.setTemplateId(sendMsgReq.getTemplateId());
        mqm.setTemplateData(JSONUtil.toJsonString(sendMsgReq.getTemplateData()));
        mqm.setStatus(MsgStatus.Pending.getStatus());

        // 3. 根据优先级入表

        String tableName = Constants.TableNamePre_MsgQueue + PriorityEnum.GetPriorityStr(sendMsgReq.getPriority());

        // 4. 入库
        try{
            msgQueueService.insert(tableName, mqm);

        }catch (Exception e){
            log.error(e.getMessage(), e);
            log.error("入优先级数据库失败，msgid:" + sendMsgReq.getMsgID());
        }
        return sendMsgReq.getMsgID();
    }

    @Override
    public String SendToMq(SendMsgReq sendMsgReq) {
        // 1. 生成 MsgID
        if(StringUtils.isEmpty(sendMsgReq.getMsgID())) {
            sendMsgReq.setMsgID(UUID.randomUUID().toString());
        }

        // 2. 序列化请求为 一条 String 消息
        String mqData = JSONUtil.toJsonString(sendMsgReq);

        // 3.根据消息优先级，确定要投递的 Topic    low-topic|middel-topic|high-topic
        String topic =PriorityEnum.GetPriorityStr(sendMsgReq.getPriority())+Constants.Topic_Tail_MsgQueue;

        //4. 发送消息到消息队列中转
        kafkaTemplate.send(topic,mqData);

        // 5. 返回消息Id
        return  sendMsgReq.getMsgID();
    }

    @Override
    public String SendToTimer(SendMsgReq sendMsgReq) {
        // 生成消息 ID
        String msgId = UUID.randomUUID().toString();
        sendMsgReq.setMsgID(msgId);

        //序列化整个请求为 String
        String mqData = JSONUtil.toJsonString(sendMsgReq);

        // 构建MsgQueueTimerModel，数据库存入的参数模型
        MsgQueueTimerModel newMsgModel = new MsgQueueTimerModel();
        newMsgModel.setMsgId(msgId);
        newMsgModel.setReq(mqData);
        newMsgModel.setSendTimestamp(sendMsgReq.getSendTimestamp());
        newMsgModel.setStatus(MsgStatus.Pending.getStatus());

        // 存入数据库
        msgQueueTimerMapper.insert(newMsgModel);

        // 时间点，存入 ZSET；
        timerMsgCache.cacheSaveMsgTimePoint(newMsgModel.getSendTimestamp());

        return msgId;
    }
}
