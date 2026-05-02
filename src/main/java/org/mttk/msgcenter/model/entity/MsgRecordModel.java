package org.mttk.msgcenter.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_msg_record")
public class MsgRecordModel {

    private Long id;

    private String msgId;

    private String sourceId;

    private Integer channel;

    private String subject;

    @TableField("`to`")
    private String to;

    private String templateId;

    private String templateData;

    private Integer status;

    private Integer retryCount;

}
