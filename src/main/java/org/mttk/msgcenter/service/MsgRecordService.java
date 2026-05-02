package org.mttk.msgcenter.service;

import org.mttk.msgcenter.enums.MsgStatus;
import org.mttk.msgcenter.model.dto.PageReq;
import org.mttk.msgcenter.model.dto.SendMsgReq;
import org.mttk.msgcenter.model.entity.MsgRecordModel;
import org.mttk.msgcenter.model.entity.TemplateModel;
import org.mttk.msgcenter.model.vo.PageResult;


public interface MsgRecordService {

    MsgRecordModel GetMsgRecordWithCache(String msgId);

    void CreateMsgRecord(String msgId, SendMsgReq sendMsgReq, TemplateModel tp, MsgStatus status);

    void UpdateOrCreateMsgRecord(String msgId,SendMsgReq sendMsgReq, TemplateModel tp, MsgStatus status);

    PageResult<MsgRecordModel> GetMsgRecordList(PageReq pageReq);
}
