package org.mttk.msgcenter.service;

import org.mttk.msgcenter.model.dto.SendMsgReq;

public interface SendMsgService {
    String SendMsg(SendMsgReq sendMsgReq);
}
