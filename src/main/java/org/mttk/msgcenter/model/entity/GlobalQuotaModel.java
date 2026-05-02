package org.mttk.msgcenter.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_global_quota")
public class GlobalQuotaModel {
    private Long id;

    private int num;

    private int unit;

    private int channel;
}
