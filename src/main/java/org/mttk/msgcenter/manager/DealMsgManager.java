package org.mttk.msgcenter.manager;

import org.mttk.msgcenter.model.dto.SendMsgReq;

public interface DealMsgManager {
    void DealOneMsg(SendMsgReq sendMsgReq);
}
