package org.mttk.msgcenter.msgpush.base;

import lombok.Data;

import java.util.Map;

@Data
public class ChannelMsgBase {
    private String to;

    private String subject;

    private String content;

    private int priority;

    private String templateId;

    private Map<String,String> templateData;

    private String notifyURL;
}
