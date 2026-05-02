package org.mttk.msgcenter.consumer.poll;


import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.extern.slf4j.Slf4j;
import org.mttk.msgcenter.common.conf.SendMsgConf;
import org.mttk.msgcenter.constant.Constants;
import org.mttk.msgcenter.enums.MsgStatus;
import org.mttk.msgcenter.enums.PriorityEnum;
import org.mttk.msgcenter.manager.DealMsgManager;
import org.mttk.msgcenter.manager.SendMsgManager;
import org.mttk.msgcenter.mapper.MsgQueueMapper;
import org.mttk.msgcenter.mapper.MsgRecordMapper;
import org.mttk.msgcenter.model.dto.SendMsgReq;
import org.mttk.msgcenter.model.entity.MsgQueueModel;
import org.mttk.msgcenter.model.entity.MsgRecordModel;
import org.mttk.msgcenter.sql2mp.MsgQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MysqlMsgPollTask {
    @Autowired
    DealMsgManager dealMsgManager;

    @Autowired
    MsgQueueMapper msgQueueMapper;
    @Autowired
    MsgQueueService msgQueueService;

    @Autowired
    MsgRecordMapper msgRecordMapper;

    @Autowired
    SendMsgConf sendMsgConf;

    @Autowired
    SendMsgManager sendMsgManager;

    @Async("mysqlMsgDealPoll")
    public void asyncHandleMsg(SendMsgReq req) {

        String tableName = Constants.TableNamePre_MsgQueue+ PriorityEnum.GetPriorityStr(req.getPriority());

        // 走消息发送逻辑
        try{
            dealMsgManager.DealOneMsg(req);
            // 发送成功
//            msgQueueMapper.setStatus(tableName,req.getMsgID(), MsgStatus.Succeed.getStatus());
            msgQueueService.setStatus(tableName,MsgStatus.Succeed.getStatus(), req.getMsgID());

        }catch (Exception e){
            if(req.getPriority() != PriorityEnum.PRIORITY_RETRY.getPriorty()){
//                msgQueueMapper.setStatus(tableName,req.getMsgID(),MsgStatus.Failed.getStatus());
                msgQueueService.setStatus(tableName,MsgStatus.Failed.getStatus(), req.getMsgID());
            }
            // 走重试队列
            dealRetryMysqlQueue(req);
        }
    }

    private void dealRetryMysqlQueue(SendMsgReq req){
        // 增加重试次数并检查是否达到上限
//        MsgRecordModel mrd = msgRecordMapper.getMsgById(req.getMsgID());
        LambdaQueryWrapper<MsgRecordModel> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MsgRecordModel::getMsgId,req.getMsgID());
        MsgRecordModel mrd = msgRecordMapper.selectOne(wrapper);

        String retryTableName = Constants.TableNamePre_MsgQueue+ PriorityEnum.GetPriorityStr(PriorityEnum.PRIORITY_RETRY.getPriorty());

        //检查重试次数是否到达上线

        if(mrd.getRetryCount() > 0 && mrd.getRetryCount() >= sendMsgConf.getMaxRetryCount()){
            log.info("消息"+req.getMsgID()+"已达到最大重试次数，不再重试:"+ sendMsgConf.getMaxRetryCount());
            // 更新【消息记录】状态为最终失败
//            msgRecordMapper.setStatus(req.getMsgID(), MsgStatus.Failed.getStatus());
            LambdaUpdateWrapper<MsgRecordModel> upd = new LambdaUpdateWrapper<>();
            upd.eq(MsgRecordModel::getMsgId,req.getMsgID());
            upd.set(MsgRecordModel::getStatus,MsgStatus.Failed.getStatus());
            msgRecordMapper.update(upd);
            // 更新重试队列状态为最终失败
            msgQueueService.setStatus( retryTableName, MsgStatus.Failed.getStatus(),req.getMsgID());
            return;
        }
        // 重试次数+1
        int newCount = mrd.getRetryCount()+1;

//        msgRecordMapper.incrementRetryCount(req.getMsgID(),newCount);
        LambdaUpdateWrapper<MsgRecordModel> updateWrapper = new LambdaUpdateWrapper<>();
        updateWrapper.set(MsgRecordModel::getRetryCount,newCount)
                .eq(MsgRecordModel::getMsgId,req.getMsgID());
        msgRecordMapper.update(updateWrapper);

        // 判断重试队列表中是否已经存在
        MsgQueueModel msgQueueModel = msgQueueService.getMsgById(retryTableName,req.getMsgID());
        if(msgQueueModel == null){
            // 重新发送消息到重试队列
            req.setPriority(PriorityEnum.PRIORITY_RETRY.getPriorty());
            sendMsgManager.SendToMysql(req);
        }else{
            msgQueueService.setStatus( retryTableName,  MsgStatus.Pending.getStatus(),req.getMsgID());
        }

        log.info("消息"+req.getMsgID()+"已加入MySQL重试队列，当前重试次数:", newCount);
    }
}
