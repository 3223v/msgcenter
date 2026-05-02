package org.mttk.msgcenter.sql2mp;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.mttk.msgcenter.common.conf.MybatisPlusConfig;
import org.mttk.msgcenter.mapper.MsgQueueMapper;
import org.mttk.msgcenter.model.entity.MsgQueueModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class MsgQueueService {
    @Autowired
    private MsgQueueMapper queueMapper;
    /**
     * 动态插入
     */
    public void insert(String tableName, MsgQueueModel mqm){
        try {
            // 设置当前线程要操作的表名
            MybatisPlusConfig.TABLE_NAME_HOLDER.set(tableName);
            // 执行插入
            queueMapper.insert(mqm);
        } finally {
            // 【关键】清理 ThreadLocal，防止内存泄漏
            MybatisPlusConfig.TABLE_NAME_HOLDER.remove();
        }
    }
    // ====================== 2. getMsgById（根据 msgId 查询） ======================
    public MsgQueueModel getMsgById(String tableName, String msgId) {
        try {
            MybatisPlusConfig.TABLE_NAME_HOLDER.set(tableName);
            LambdaQueryWrapper<MsgQueueModel> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(MsgQueueModel::getMsgId, msgId);
            return queueMapper.selectOne(wrapper);
        } finally {
            MybatisPlusConfig.TABLE_NAME_HOLDER.remove();
        }
    }
    /**
     * 获取一批待处理的消息
     */
     public List<MsgQueueModel> getMsgsByStatus(String tableName, Integer status, Integer limit){
        try {
            MybatisPlusConfig.TABLE_NAME_HOLDER.set(tableName);
            LambdaQueryWrapper<MsgQueueModel> wrapper = Wrappers.lambdaQuery();
            wrapper.eq(MsgQueueModel::getStatus, status)
                    .last("LIMIT " + limit); // 拼接 LIMIT
            return queueMapper.selectList(wrapper);
        } finally {
            MybatisPlusConfig.TABLE_NAME_HOLDER.remove();
        }
     }
    // ====================== 4. setStatus（单条更新状态） ======================
    public void setStatus(String tableName, Integer status, String msgId) {
        try {
            MybatisPlusConfig.TABLE_NAME_HOLDER.set(tableName);
            LambdaUpdateWrapper<MsgQueueModel> wrapper = Wrappers.lambdaUpdate();
            wrapper.set(MsgQueueModel::getStatus, status)
                    .eq(MsgQueueModel::getMsgId, msgId);
            queueMapper.update(null, wrapper);
        } finally {
            MybatisPlusConfig.TABLE_NAME_HOLDER.remove();
        }
    }
    // ====================== 5. batchSetStatus（批量更新状态，防 SQL 注入） ======================
    public void batchSetStatus(String tableName, List<String> msgIdList, Integer status) {
        try {
            MybatisPlusConfig.TABLE_NAME_HOLDER.set(tableName);
            LambdaUpdateWrapper<MsgQueueModel> wrapper = Wrappers.lambdaUpdate();
            wrapper.set(MsgQueueModel::getStatus, status)
                    .in(MsgQueueModel::getMsgId, msgIdList); // 传 List，防注入
            queueMapper.update(null, wrapper);
        } finally {
            MybatisPlusConfig.TABLE_NAME_HOLDER.remove();
        }
    }

}
