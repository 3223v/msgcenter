package org.mttk.msgcenter.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_msg_queue_middle")
public class MsgQueueModel {
    private Long id;

    private String msgId;

    private String to;

    private String subject;

    private int priority;

    private int channel;

    private String templateId;

    private String templateData;

    private int status;
}
