package org.mttk.msgcenter.manager.impl;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.mttk.msgcenter.enums.ChannelEnum;
import org.mttk.msgcenter.enums.MsgStatus;
import org.mttk.msgcenter.manager.DealMsgManager;
import org.mttk.msgcenter.model.dto.SendMsgReq;
import org.mttk.msgcenter.model.entity.TemplateModel;
import org.mttk.msgcenter.msgpush.MsgPushService;
import org.mttk.msgcenter.msgpush.base.ChannelMsgBase;
import org.mttk.msgcenter.service.MsgRecordService;
import org.mttk.msgcenter.service.TemplateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class DealMsgManagerImpl implements DealMsgManager {
    public static Map<Integer, MsgPushService> channelStrategyMap = new HashMap<>();

    @Autowired
    MsgPushService emailServiceImpl;
    @Autowired
    MsgPushService larkServiceImpl;
    @Autowired
    MsgPushService SMSServiceImpl;
    // 初始化各种推送策略服务 Email|Lark|SMS
    @PostConstruct
    public void initChannelStrategyMap() {
        channelStrategyMap.put(ChannelEnum.Channel_EMAIL.getChannel(), emailServiceImpl);
        channelStrategyMap.put(ChannelEnum.Channel_LARK.getChannel(), larkServiceImpl);
        channelStrategyMap.put(ChannelEnum.Channel_SMS.getChannel(), SMSServiceImpl);
    }
    @Autowired
    TemplateService templateService;

    @Autowired
    MsgRecordService msgRecordService;
    @Override
    public void DealOneMsg(SendMsgReq sendMsgReq) {
        // 1. 查找模板
        TemplateModel tp = templateService.GetTemplate(sendMsgReq.getTemplateId());

        // 2.替换模板中的变量
        String msgContent = replaceStr(tp.getContent(),sendMsgReq.getTemplateData());

        // 3. 构建推送消息的基本参数
        ChannelMsgBase base = new ChannelMsgBase();
        base.setTo(sendMsgReq.getTo());
        base.setSubject(sendMsgReq.getSubject());
        base.setContent(msgContent);
        base.setPriority(sendMsgReq.getPriority());
        base.setTemplateId(sendMsgReq.getTemplateId());
        base.setTemplateData(sendMsgReq.getTemplateData());

        // 4. 根据渠道，获取具体的推送策略 Email|Lark|SMS
        MsgPushService msgService = channelStrategyMap.get(tp.getChannel());

        // 5. 调用具体策略服务去推送消息
        msgService.pushMsg(base);

        // 6. 存储消息发送记录
        try{
            msgRecordService.UpdateOrCreateMsgRecord(sendMsgReq.getMsgID(),sendMsgReq,tp, MsgStatus.Succeed);
        }catch (Exception e){
            log.error("存储消息发送记录失败， msgId",sendMsgReq.getMsgID());
        }

    }
    private String replaceStr(String template,Map<String,String> paramsMap) {
        String remark = template;
        for (Map.Entry<String, String> entry : paramsMap.entrySet()) {
            remark = StringUtils.replace(remark, "${" + entry.getKey() + "}", entry.getValue());
        }
        return remark;
    }
}
