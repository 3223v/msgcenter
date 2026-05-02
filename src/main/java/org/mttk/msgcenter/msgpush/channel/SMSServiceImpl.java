package org.mttk.msgcenter.msgpush.channel;

import org.mttk.msgcenter.msgpush.MsgPushService;
import org.mttk.msgcenter.msgpush.base.ChannelMsgBase;
import org.springframework.stereotype.Service;

@Service
public class SMSServiceImpl implements MsgPushService {
    @Override
    public void pushMsg(ChannelMsgBase msgBase){
        System.out.println(msgBase);
    }
}
