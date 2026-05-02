package org.mttk.msgcenter.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_msg_record")
public class MsgRecordModel {

    private Long id;

    private String msgId;

    private String sourceId;

    private int channel;

    private String subject;

    private String to;

    private String templateId;

    private String templateData;

    private int status;

    private int retryCount; //重试次数，默认为 0

}
