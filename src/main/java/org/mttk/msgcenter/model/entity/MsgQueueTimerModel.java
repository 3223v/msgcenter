package org.mttk.msgcenter.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_msg_tmp_queue_timer")
public class MsgQueueTimerModel {
    private Long id;

    private String msgId;

    private String req;

    private Long sendTimestamp;

    private int status;
}
