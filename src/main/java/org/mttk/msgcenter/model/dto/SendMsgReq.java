package org.mttk.msgcenter.model.dto;

import lombok.Data;

import java.util.Map;
@Data
public class SendMsgReq {
    private String to;

    private String subject;

    private int priority;

    private String templateId;

    private Map<String,String> templateData;

    private Long sendTimestamp;

    private String msgID;
}
