package org.mttk.msgcenter.msgpush.channel;

import lombok.extern.slf4j.Slf4j;
import org.mttk.msgcenter.msgpush.MsgPushService;
import org.mttk.msgcenter.msgpush.base.ChannelMsgBase;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class LarkServiceImpl implements MsgPushService {
    @Override
    public void pushMsg(ChannelMsgBase msgBase){
        log.info(msgBase.toString());
    }
}
