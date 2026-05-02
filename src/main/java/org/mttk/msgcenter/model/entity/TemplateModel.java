package org.mttk.msgcenter.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 模板表
 */
@Data
@TableName("t_msg_template")
public class TemplateModel {
    private Long id;

    private String templateId;

    private String relTemplateId;

    private String name;

    private String signName;

    private String sourceId;

    private int channel;

    private String subject;

    private String content;

    private int status;
}
