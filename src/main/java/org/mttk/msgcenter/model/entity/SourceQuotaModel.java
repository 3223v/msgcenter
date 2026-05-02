package org.mttk.msgcenter.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_source_quota")
public class SourceQuotaModel {
    private Long id;

    private int num;

    private int unit;

    private int channel;

    private String sourceId;
}
