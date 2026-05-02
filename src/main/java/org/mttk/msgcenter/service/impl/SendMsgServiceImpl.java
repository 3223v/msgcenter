package org.mttk.msgcenter.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mttk.msgcenter.common.conf.SendMsgConf;
import org.mttk.msgcenter.enums.MsgStatus;
import org.mttk.msgcenter.enums.TemplateStatus;
import org.mttk.msgcenter.exception.BusinessException;
import org.mttk.msgcenter.exception.ErrorCode;
import org.mttk.msgcenter.manager.SendMsgManager;
import org.mttk.msgcenter.model.dto.SendMsgReq;
import org.mttk.msgcenter.model.entity.TemplateModel;
import org.mttk.msgcenter.service.MsgRecordService;
import org.mttk.msgcenter.service.RateLimitService;
import org.mttk.msgcenter.service.SendMsgService;
import org.mttk.msgcenter.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class SendMsgServiceImpl implements SendMsgService {
    @Autowired
    private TemplateService templateService;

    @Autowired
    private SendMsgConf sendMsgConf;

    @Autowired
    private SendMsgManager sendMsgManager;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private MsgRecordService msgRecordService;

    @Override
    public String SendMsg(SendMsgReq sendMsgReq) {
        // 1.校验参数

        // 2.查询模板
        TemplateModel tp = templateService.GetTemplate(sendMsgReq.getTemplateId());
        if(tp.getStatus() != TemplateStatus.TEMPLATE_STATUS_NORMAL.getStatus()){
            throw new BusinessException(ErrorCode.TEMPLATE_STATUS_ERROR,"模板异常");
        }
        // 3.查询是否是定时消息
        boolean isTimerMsg = false;
        if(sendMsgReq.getSendTimestamp() != null){
            isTimerMsg = true;
        }
        // 4.校验配额
        boolean allowed = rateLimitService.isRequestAllowed(tp.getSourceId(),tp.getChannel(),isTimerMsg);
        if(!allowed){
            throw new BusinessException(ErrorCode.RateLimit_ERROR,"限流了");
        }
        // 5.发送到缓冲区 定时｜Mysql 缓冲｜MQ 缓冲
        if (isTimerMsg) {
            return sendMsgManager.SendToTimer(sendMsgReq);
        }

        String msgId = null;

        if(sendMsgConf.isMysqlAsMq()){
            msgId = sendMsgManager.SendToMysql(sendMsgReq);
        }else {
            msgId = sendMsgManager.SendToMq(sendMsgReq);
        }
        if(!StringUtils.isEmpty(msgId)){
            //字符串不空
            msgRecordService.CreateMsgRecord(msgId,sendMsgReq,tp, MsgStatus.Pending);
        }
        return msgId;
    }
}
