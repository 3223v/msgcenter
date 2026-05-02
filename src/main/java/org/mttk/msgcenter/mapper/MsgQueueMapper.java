package org.mttk.msgcenter.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.mttk.msgcenter.model.entity.MsgQueueModel;

@Mapper
public interface MsgQueueMapper extends BaseMapper<MsgQueueModel> {
}
