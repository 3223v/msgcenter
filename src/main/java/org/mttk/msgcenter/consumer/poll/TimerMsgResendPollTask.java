package org.mttk.msgcenter.consumer.poll;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.mttk.msgcenter.common.conf.SendMsgConf;
import org.mttk.msgcenter.enums.MsgStatus;
import org.mttk.msgcenter.enums.TemplateStatus;
import org.mttk.msgcenter.exception.BusinessException;
import org.mttk.msgcenter.exception.ErrorCode;
import org.mttk.msgcenter.manager.SendMsgManager;
import org.mttk.msgcenter.mapper.MsgQueueTimerMapper;
import org.mttk.msgcenter.model.dto.SendMsgReq;
import org.mttk.msgcenter.model.entity.MsgQueueTimerModel;
import org.mttk.msgcenter.model.entity.TemplateModel;
import org.mttk.msgcenter.service.MsgRecordService;
import org.mttk.msgcenter.service.TemplateService;
import org.mttk.msgcenter.utils.JSONUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class TimerMsgResendPollTask {

    @Autowired
    MsgQueueTimerMapper msgQueueTimerMapper;


    @Autowired
    SendMsgManager sendMsgManager;

    @Autowired
    SendMsgConf sendMsgConf;

    @Autowired
    MsgRecordService msgRecordService;

    @Autowired
    TemplateService templateService;

    @Async("timerMsgPoll")
    public void asyncHandleMsg(String  reqStr) {
        SendMsgReq sendMsgReq = JSONUtil.parseObject(reqStr,SendMsgReq.class);
        if (sendMsgReq == null){
            return;
        }
        TemplateModel tp = templateService.GetTemplate(sendMsgReq.getTemplateId());
        if(tp.getStatus() != TemplateStatus.TEMPLATE_STATUS_NORMAL.getStatus()){
            throw new BusinessException(ErrorCode.TEMPLATE_STATUS_ERROR, "模板尚未准备好，检查模板状态");
        }
        boolean success = false;
        try {
            if(sendMsgConf.isMysqlAsMq()){
                // 发送到 Mysql
                sendMsgManager.SendToMysql(sendMsgReq);
            }else{
                // 发送到 MQ
                sendMsgManager.SendToMq(sendMsgReq);
            }
            success = true;
        }catch (Exception e){
            // 重试一次
            if(sendMsgConf.isMysqlAsMq()){
                // 发送到 Mysql
                sendMsgManager.SendToMysql(sendMsgReq);
            }else{
                // 发送到 MQ
                sendMsgManager.SendToMq(sendMsgReq);
            }
            success = true;
        }

        // 2.更新消息记录状态
        if (success) {
            msgRecordService.UpdateOrCreateMsgRecord(sendMsgReq.getMsgID(),sendMsgReq,tp, MsgStatus.Pending);
        }else{
            msgRecordService.UpdateOrCreateMsgRecord(sendMsgReq.getMsgID(),sendMsgReq,tp,MsgStatus.Failed);
        }

        // 将msgId消息变为处理中Succeed
//        msgQueueTimerMapper.setStatus(sendMsgReq.getMsgID(), MsgStatus.Succeed.getStatus());
        LambdaUpdateWrapper<MsgQueueTimerModel> wrapper = new LambdaUpdateWrapper<>();
        wrapper.set(MsgQueueTimerModel::getStatus, MsgStatus.Succeed.getStatus())
                .eq(MsgQueueTimerModel::getMsgId, sendMsgReq.getMsgID());
        msgQueueTimerMapper.update(wrapper);
    }
}
