package org.mttk.msgcenter.msgpush;

import org.mttk.msgcenter.msgpush.base.ChannelMsgBase;

public interface MsgPushService {
    void pushMsg(ChannelMsgBase msgBase);
}
