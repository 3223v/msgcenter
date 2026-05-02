package org.mttk.msgcenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.mttk.msgcenter.model.entity.MsgQueueTimerModel;

@Mapper
public interface MsgQueueTimerMapper extends BaseMapper<MsgQueueTimerModel> {
}
