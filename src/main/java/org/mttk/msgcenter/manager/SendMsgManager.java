package org.mttk.msgcenter.manager;

import org.mttk.msgcenter.model.dto.SendMsgReq;


public interface SendMsgManager {
    String SendToMysql(SendMsgReq sendMsgReq);
    String SendToMq(SendMsgReq sendMsgReq);
    String SendToTimer(SendMsgReq sendMsgReq);
}
