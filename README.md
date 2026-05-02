# MsgCenter — 消息中心服务

一个基于 Spring Boot 的统一消息推送中心，支持多渠道（邮件、短信、飞书）消息发送，具备模板管理、优先级队列、定时消息、限流控制、失败重试等能力。

---

## 技术栈

| 组件 | 版本 | 用途 |
|------|------|------|
| Spring Boot | 3.3.1 | 应用框架 |
| MyBatis-Plus | 3.5.9 | ORM 框架 |
| MySQL | 5.7+ | 消息存储 |
| Redis | 6.0+ | 缓存 & 分布式锁 |
| Apache Kafka | 3.x | 消息队列（高优先级通道） |
| Lombok | 1.18.42 | 简化代码 |
| Java | 17 | 运行环境 |

---

## 项目结构

```
msgcenter/
├── sql/
│   └── msgcenter.sql                  # 数据库建表脚本
├── src/main/java/org/mttk/msgcenter/
│   ├── MsgcenterApplication.java      # 启动类
│   ├── controller/                    # 对外 HTTP 接口
│   ├── service/                       # 业务逻辑层
│   │   └── impl/
│   ├── manager/                       # 核心管理层（消息处理 & 发送编排）
│   │   └── impl/
│   ├── mapper/                        # MyBatis-Plus Mapper
│   ├── model/
│   │   ├── dto/                       # 请求参数
│   │   ├── entity/                    # 数据库实体
│   │   └── vo/                        # 响应对象
│   ├── enums/                         # 枚举常量
│   ├── consumer/                      # Kafka / MySQL 消费者
│   ├── msgpush/                       # 渠道推送实现
│   │   └── channel/                   # Email / SMS / Lark
│   ├── redis/                         # Redis 工具 & 分布式锁
│   ├── common/conf/                   # 全局配置
│   ├── constant/                      # 常量
│   ├── exception/                     # 异常处理
│   └── utils/                         # 工具类
├── src/main/resources/
│   └── application.yml                # 配置文件
├── index.html                         # 接口调试前端页面
└── pom.xml
```

---

## 数据库表说明

执行 `sql/msgcenter.sql` 完成建表。

| 表名 | 说明 |
|------|------|
| `t_msg_template` | 消息模板表 |
| `t_msg_record` | 消息发送记录表 |
| `t_msg_queue_low` | 低优先级消息队列 |
| `t_msg_queue_middle` | 中优先级消息队列 |
| `t_msg_queue_high` | 高优先级消息队列 |
| `t_msg_queue_retry` | 重试消息队列 |
| `t_msg_tmp_queue_timer` | 定时消息队列 |
| `t_global_quota` | 全局限流配置 |
| `t_source_quota` | 业务源限流配置 |

---

## 枚举值速查

### 推送渠道 `channel`

| 值 | 含义 |
|----|------|
| 1  | 邮件（Email） |
| 2  | 短信（SMS） |
| 3  | 飞书（Lark） |

### 模板状态 `status`（t_msg_template）

| 值 | 含义 |
|----|------|
| 1  | 待审核（Pending） |
| 2  | 正常（Normal） |

### 消息状态 `status`（t_msg_record）

| 值 | 含义 |
|----|------|
| 1  | 等待中（Pending） |
| 2  | 处理中（Processing） |
| 3  | 成功（Succeed） |
| 4  | 失败（Failed） |

### 优先级 `priority`

| 值 | 含义 |
|----|------|
| 1  | 低（Low） |
| 2  | 中（Middle） |
| 3  | 高（High） |
| 4  | 重试（Retry） |

---

## 接口文档

所有接口统一前缀：`/msg`

统一返回格式：

```json
{
  "code": 200,
  "msg": "success",
  "data": ...
}
```

### 1. 创建模板

- **POST** `/msg/create_template`
- **Content-Type**: `application/json`

**请求体：**

```json
{
  "name": "欢迎邮件模板",
  "signName": "MsgCenter",
  "sourceId": "user-service",
  "channel": 1,
  "subject": "欢迎注册",
  "content": "亲爱的 ${userName}，欢迎加入我们的平台！"
}
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

> 返回的 `data` 为自动生成的 `templateId`。

---

### 2. 获取单个模板

- **GET** `/msg/get_template`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| templateId | String | 是 | 模板ID |

**请求示例：**

```
GET /msg/get_template?templateId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "templateId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "relTemplateId": "yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy",
    "name": "欢迎邮件模板",
    "signName": "MsgCenter",
    "sourceId": "user-service",
    "channel": 1,
    "subject": "欢迎注册",
    "content": "亲爱的 ${userName}，欢迎加入我们的平台！",
    "status": 1
  }
}
```

---

### 3. 更新模板

- **POST** `/msg/update_template`
- **Content-Type**: `application/json`

**请求体：**

```json
{
  "id": 1,
  "templateId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "name": "欢迎邮件模板（已修改）",
  "content": "亲爱的 ${userName}，欢迎加入！新内容。",
  "status": 2
}
```

> 根据 `id` 进行更新，只传需要修改的字段即可。

---

### 4. 删除模板

- **POST** `/msg/delete_template`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| templateId | String | 是 | 模板ID |

**请求示例：**

```
POST /msg/delete_template?templateId=xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

---

### 5. 获取模板列表（分页）

- **GET** `/msg/get_template_list`

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**请求示例：**

```
GET /msg/get_template_list?pageNum=1&pageSize=10
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [
      {
        "id": 1,
        "templateId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
        "relTemplateId": "yyyyyyyy-yyyy-yyyy-yyyy-yyyyyyyyyyyy",
        "name": "欢迎邮件模板",
        "signName": "MsgCenter",
        "sourceId": "user-service",
        "channel": 1,
        "subject": "欢迎注册",
        "content": "亲爱的 ${userName}，欢迎加入我们的平台！",
        "status": 1
      }
    ],
    "total": 1,
    "pageNum": 1,
    "pageSize": 10
  }
}
```

---

### 6. 发送消息

- **POST** `/msg/send_msg`
- **Content-Type**: `application/json`

**请求体：**

```json
{
  "to": "user@example.com",
  "subject": "欢迎注册",
  "priority": 2,
  "templateId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
  "templateData": {
    "userName": "张三"
  },
  "sendTimestamp": null,
  "msgID": ""
}
```

| 字段 | 类型 | 必填 | 说明 |
|------|------|------|------|
| to | String | 是 | 接收方（邮箱/手机号/飞书ID） |
| subject | String | 否 | 消息主题 |
| priority | int | 否 | 优先级 1-4，默认 1 |
| templateId | String | 是 | 消息模板ID |
| templateData | Map | 否 | 模板变量键值对 |
| sendTimestamp | Long | 否 | 定时发送时间戳（毫秒），为空则立即发送 |
| msgID | String | 否 | 自定义消息ID，为空则自动生成 |

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": "msg-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx"
}
```

> 返回的 `data` 为消息ID，可用于查询发送状态。

---

### 7. 查询单条消息记录

- **GET** `/msg/get_msg_record`

| 参数 | 类型 | 必填 | 说明 |
|------|------|------|------|
| msgId | String | 是 | 消息ID |

**请求示例：**

```
GET /msg/get_msg_record?msgId=msg-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "id": 1,
    "msgId": "msg-xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "sourceId": "user-service",
    "channel": 1,
    "subject": "欢迎注册",
    "to": "user@example.com",
    "templateId": "xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx",
    "templateData": "{\"userName\":\"张三\"}",
    "status": 3,
    "retryCount": 0
  }
}
```

---

### 8. 获取消息记录列表（分页）

- **GET** `/msg/get_msg_record_list`

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|------|------|------|--------|------|
| pageNum | int | 否 | 1 | 页码 |
| pageSize | int | 否 | 10 | 每页条数 |

**请求示例：**

```
GET /msg/get_msg_record_list?pageNum=1&pageSize=20
```

**响应：**

```json
{
  "code": 200,
  "msg": "success",
  "data": {
    "records": [...],
    "total": 100,
    "pageNum": 1,
    "pageSize": 20
  }
}
```

---

## 配置说明

在 `src/main/resources/application.yml` 中配置：

```yaml
server:
  port: 8080                        # 服务端口

spring:
  datasource:                        # MySQL 连接
    url: jdbc:mysql://127.0.0.1:3306/msgcenter
    username: root
    password: root
  redis:                             # Redis 连接
    host: localhost
    port: 6379
    password:
    database: 0
  kafka:                             # Kafka 连接
    bootstrap-servers: 127.0.0.1:9092

send-msg-conf:
  mysql-as-mq: true                  # 是否使用 MySQL 作为低优先级消息队列
  open-cache: true                   # 是否开启 Redis 缓存
  max-retry-count: 5                 # 消息发送最大重试次数
  email-host: smtp.qq.com            # 邮件 SMTP 服务器
  email-port: 587                    # 邮件端口
  email-account: xxx                 # 邮件账号
  email-auth-code: xxx               # 邮件授权码
```

---

## 快速启动

### 1. 环境准备

- JDK 17+
- MySQL 5.7+
- Redis 6.0+
- Apache Kafka 3.x（可选，高优先级消息通道）
- Maven 3.8+

### 2. 初始化数据库

```bash
mysql -u root -p < sql/msgcenter.sql
```

### 3. 修改配置

编辑 `src/main/resources/application.yml`，填入实际的 MySQL、Redis、Kafka 连接信息。

### 4. 编译运行

```bash
mvn clean package -DskipTests
java -jar target/msgcenter-0.0.1-SNAPSHOT.jar
```

或在 IDE 中直接运行 `MsgcenterApplication.java`。

### 5. 接口调试

浏览器打开项目根目录下的 `index.html`，填入服务地址即可在线调试所有接口。

---

## 接口调试前端

项目自带一个单文件前端调试页面 `index.html`，功能包括：

- 可配置后端 IP/域名和端口
- 可视化调用所有 8 个接口
- 表单自动构造请求参数
- 实时展示请求响应 JSON
- 分页组件支持

直接用浏览器打开即可使用，无需任何构建工具。

---

## 技术架构详解

### 一、中转站机制（消息队列路由）

中转站是消息中心的核心调度层，负责将接收到的消息按优先级路由到不同的消息队列，再由消费者异步消费处理。系统支持 **MySQL 模式** 和 **Kafka 模式** 两种中转站后端，通过配置项 `send-msg-conf.mysql-as-mq` 切换。

#### 1.1 整体流程

```
客户端发送消息
       │
       ▼
  SendMsgServiceImpl.SendMsg()
       │
       ├── 定时消息？ ──是──▶ SendMsgManager.SendToTimer()
       │                        ├── 写入 t_msg_tmp_queue_timer
       │                        └── Redis ZSET 缓存时间点
       │
       └── 即时消息
            │
            ├── mysqlAsMq=true ──▶ SendMsgManager.SendToMysql()
            │                        ├── 按优先级设置动态表名
            │                        └── 写入对应优先级表
            │
            └── mysqlAsMq=false ──▶ SendMsgManager.SendToMq()
                                     └── 发送到对应优先级 Kafka Topic
```

#### 1.2 优先级队列

消息按优先级（1-4）路由到不同的队列：

| 优先级 | 值 | MySQL 表 | Kafka Topic |
|--------|-----|----------|-------------|
| 低（Low） | 1 | `t_msg_queue_low` | `low-topic` |
| 中（Middle） | 2 | `t_msg_queue_middle` | `middle-topic` |
| 高（High） | 3 | `t_msg_queue_high` | `high-topic` |
| 重试（Retry） | 4 | `t_msg_queue_retry` | `retry-topic` |

MySQL 模式下，4 张队列表结构完全相同，通过 MyBatis-Plus 的 **动态表名插件**（`DynamicTableNameInnerInterceptor`）实现运行时切换。核心原理：

```java
// MybatisPlusConfig.java
DynamicTableNameInnerInterceptor interceptor = new DynamicTableNameInnerInterceptor();
interceptor.setTableNameHandler((sql, tableName) -> {
    String name = TABLE_NAME_HOLDER.get();  // ThreadLocal
    if (!StringUtils.isEmpty(name)) {
        name = Constants.REDIS_KEY_MSGQUEUE_PREFIX + name;  // 拼接表名前缀
        TABLE_NAME_HOLDER.remove();
        return name;
    }
    return tableName;
});
```

发送时通过 `ThreadLocal` 设置目标表名：

```java
// SendMsgManagerImpl.SendToMysql()
MybatisPlusConfig.TABLE_NAME_HOLDER.set(priorityEnum.getName());  // 如 "low"
MsgQueueModel model = new MsgQueueModel();
model.setMsgId(UUID.randomUUID().toString());
// ... 设置其他字段
msgQueueService.save(model);  // 实际插入 t_msg_queue_low
```

#### 1.3 消费端架构

MySQL 模式和 Kafka 模式各有独立的消费者：

**MySQL 消费者**（`MysqlMsgConsumer`）：
- 每 1000ms 轮询一次，按优先级顺序消费（high → middle → low → retry）
- 使用分布式锁做 **Leader 选举**，同一优先级只有一个节点活跃消费
- 拉取批量大小：high=60、middle=30、low=10、retry=10
- 消费流程：查询 Pending 记录 → 批量更新为 Processing → 异步分发到线程池处理

**Kafka 消费者**（`KafkaMsgConsumer`）：
- 4 个 `@KafkaListener` 分别监听 4 个 Topic
- 使用手动 ACK 模式（`AckMode.MANUAL_IMMEDIATE`）
- 并发数：high=6、middle=3、low=1、retry=1

#### 1.4 消息处理流程

消费者拉取到消息后，异步分发到 `MysqlMsgPollTask` 处理：

```
MysqlMsgPollTask.run()
       │
       ▼
  DealMsgManager.DealOneMsg(req)
       │
       ├── 1. 获取模板（带缓存）
       ├── 2. 模板变量替换（${key} → 实际值）
       ├── 3. 构建 ChannelMsgBase
       ├── 4. 根据 channel 选择推送渠道策略
       └── 5. 调用渠道实现发送（Email/SMS/Lark）
       │
       ▼
  发送成功 → 更新记录状态为 Succeed
  发送失败 → 检查重试次数
            ├── 未超限 → retryCount++，发送到 retry 队列
            └── 已超限 → 更新记录状态为 Failed
```

渠道推送使用 **策略模式**，通过 `channelStrategyMap` 按 channel 枚举选择对应实现：

```java
@Autowired
private Map<String, MsgPushService> channelStrategyMap;

// 使用
MsgPushService service = channelStrategyMap.get(
    ChannelEnum.getChanneEnumByCode(channel).getChannelStrategy()
);
service.pushMsg(channelMsgBase);
```

#### 1.5 关键配置

| 配置项 | 说明 | 默认值 |
|--------|------|--------|
| `send-msg-conf.mysql-as-mq` | 是否使用 MySQL 作为中转站 | `true` |
| `send-msg-conf.max-retry-count` | 消息最大重试次数 | `5` |

---

### 二、定时消息机制

定时消息允许指定一个未来时间点发送消息。系统使用 **Redis ZSET + MySQL** 双存储实现。

#### 2.1 存储设计

**MySQL 表** `t_msg_tmp_queue_timer`：

| 字段 | 类型 | 说明 |
|------|------|------|
| id | bigint | 主键 |
| msgId | varchar(128) | 消息ID |
| req | varchar(1024) | 完整请求 JSON（SendMsgReq 序列化） |
| sendTimestamp | bigint | 预定发送时间戳（毫秒） |
| status | int | 状态：1=Pending, 3=Succeed |

**Redis ZSET** `Timer_Msgs`：
- member：时间戳（毫秒）
- score：时间戳（毫秒）
- 作用：快速检索到期时间点，避免全表扫描

#### 2.2 发送流程

```java
// SendMsgManagerImpl.SendToTimer()
public void SendToTimer(SendMsgReq req) {
    String msgId = UUID.randomUUID().toString();
    req.setMsgID(msgId);
    
    MsgQueueTimerModel model = new MsgQueueTimerModel();
    model.setMsgId(msgId);
    model.setReq(JSONUtil.toJsonString(req));      // 存储完整请求
    model.setSendTimestamp(req.getSendTimestamp());  // 目标发送时间
    model.setStatus(MsgStatus.PENDING.getStatus());
    
    msgQueueTimerService.save(model);                    // 写 MySQL
    timerMsgCache.cacheSaveMsgTimePoint(req.getSendTimestamp());  // 写 Redis ZSET
}
```

Redis ZSET 写入使用 `ZADD`：

```java
// TimerMsgCache.cacheSaveMsgTimePoint()
public void cacheSaveMsgTimePoint(Long sendTimestamp) {
    redisTemplate.opsForZSet().add(TIMER_MSGS_CACHE, 
        String.valueOf(sendTimestamp), sendTimestamp);
}
```

#### 2.3 消费流程

`TimerMsgConsumer` 每 100ms 执行一次，流程如下：

```
TimerMsgConsumer (每100ms)
       │
       ├── 1. 获取分布式锁（Leader 选举）
       │      └── 非 Leader → sleep 10s 后重试
       │
       ├── 2. 从 Redis ZSET 获取到期时间点
       │      Lua 脚本原子操作：
       │      ZRANGEBYSCORE Timer_Msgs 0 {当前时间戳}
       │      对每个命中的 member 执行 ZREM
       │      → 防止多节点重复消费
       │
       ├── 3. 根据时间点从 MySQL 查询到期记录
       │      WHERE sendTimestamp <= now AND status = Pending
       │
       ├── 4. 批量更新状态为 Processing
       │
       └── 5. 异步分发到 timerMsgPoll 线程池
              └── TimerMsgResendPollTask
                  ├── 重新校验模板状态
                  ├── 根据配置路由到 MySQL 或 Kafka 队列
                  ├── 更新 MsgRecord 状态
                  └── 标记定时记录为 Succeed
```

Redis ZSET 的原子弹出使用 Lua 脚本保证线程安全：

```java
// TimerMsgCache.getOnTimePointsFromCache()
String lua = "local result = redis.call('ZRANGEBYSCORE', KEYS[1], 0, ARGV[1])\n" +
    "for i, v in ipairs(result) do\n" +
    "    redis.call('ZREM', KEYS[1], v)\n" +
    "end\n" +
    "return result";
// KEYS[1] = "Timer_Msgs", ARGV[1] = 当前时间戳
```

#### 2.4 定时消息的二次路由

定时消息到期后，`TimerMsgResendPollTask` 会将其重新投入普通消息队列：

```java
if (sendMsgConf.isMysqlAsMq()) {
    sendMsgManager.SendToMysql(req);   // → MySQL 优先级队列
} else {
    sendMsgManager.SendToMq(req);      // → Kafka 优先级 Topic
}
```

这意味着定时消息的最终发送走的是和即时消息完全相同的消费链路。

---

### 三、限额机制（Rate Limiting）

限额机制用于控制消息发送频率，防止业务方滥用。采用 **两级配额 + Redis 滑动窗口计数器** 实现。

#### 3.1 两级配额模型

```
请求到达
    │
    ├── 1. 查询业务源配额（t_source_quota）
    │      WHERE sourceId = ? AND channel = ?
    │      └── 命中 → 使用源级配额
    │
    └── 2. 未命中 → 查询全局限额（t_global_quota）
           WHERE channel = ?
           └── 命中 → 使用全局配额
           └── 仍未命中 → 不限流
```

配额配置表结构：

**`t_source_quota`**（业务源级）：

| 字段 | 说明 | 示例 |
|------|------|------|
| sourceId | 业务来源标识 | `user-service` |
| channel | 推送渠道 | `1`（邮件） |
| num | 窗口内允许的最大请求数 | `100` |
| unit | 窗口大小（毫秒） | `60000`（1分钟） |

**`t_global_quota`**（全局级）：

| 字段 | 说明 | 示例 |
|------|------|------|
| channel | 推送渠道 | `1`（邮件） |
| num | 窗口内允许的最大请求数 | `500` |
| unit | 窗口大小（毫秒） | `60000`（1分钟） |

#### 3.2 限流算法实现

使用 Redis `INCR` 实现固定窗口计数器：

```java
// RateLimitServiceImpl.checkAllowed()
private boolean checkAllowed(String keyId, int limit, int div) {
    // keyId = 限流 key（如 "XMSG_rate_limit_count_user-service_1"）
    // limit = 允许的最大请求数
    // div = 窗口大小（毫秒）
    
    Long count = redisBase.incr(keyId, div);  // INCR + EXPIRE
    if (count > limit) {
        return false;  // 超限
    }
    return true;
}
```

`redisBase.incr()` 的实现保证了原子性：

```java
// RedisBase.incr()
public Long incr(String key, long expireTime) {
    Long count = redisTemplate.opsForValue().increment(key);
    if (count != null && count == 1) {
        redisTemplate.expire(key, expireTime, TimeUnit.MILLISECONDS);
    }
    return count;
}
```

#### 3.3 限流 Key 设计

| 场景 | Redis Key 格式 | 示例 |
|------|---------------|------|
| 普通消息 | `XMSG_rate_limit_count_{sourceId}_{channel}` | `XMSG_rate_limit_count_user-service_1` |
| 定时消息 | `XMSG_rate_limit_count_timer_{sourceId}_{channel}` | `XMSG_rate_limit_count_timer_user-service_1` |

普通消息和定时消息使用不同的 Key 前缀，分别独立计数。

#### 3.4 配额缓存

配额配置缓存在 Redis 中，避免每次请求都查库：

```
Key:    XMSG_source_quota_{sourceId}{channel}
Value:  {num}_{unit}   （如 "100_60000"）
TTL:    30 秒
```

#### 3.5 限流流程

```
SendMsgServiceImpl.SendMsg()
       │
       ├── 1. 获取模板，校验状态
       │
       ├── 2. rateLimitService.isRequestAllowed(sourceId, channel, isTimer)
       │      │
       │      ├── 查 Redis 缓存的配额配置
       │      ├── 未缓存 → 查 DB（source → global）→ 写缓存
       │      ├── 无配置 → 放行
       │      └── 有配置 → checkAllowed(key, num, unit)
       │           ├── count <= num → 放行
       │           └── count > num → 抛出 RateLimit_ERROR
       │
       └── 3. 通过限流 → 继续路由到中转站
```

#### 3.6 错误码

限流触发时返回 `40002` 错误码（`RateLimit_ERROR`），消息不会进入队列。

---

### 四、分布式锁机制

分布式锁用于多节点部署时的 Leader 选举和资源互斥，基于 Redis 实现，支持可重入和自动续期。

#### 4.1 核心实现类

`ReentrantDistributeLock` 提供三种操作：

| 方法 | 说明 |
|------|------|
| `lock(key, token, expireSeconds)` | 获取锁（阻塞） |
| `lockWithDog(key, token, expireSeconds)` | 获取锁 + 启动看门狗自动续期 |
| `unlock(key, token)` | 释放锁 |

#### 4.2 加锁流程

```java
public boolean lock(String key, String token, int expireSeconds) {
    // 1. 可重入检查：如果当前线程已持有该锁，直接返回
    String currentValue = redisTemplate.opsForValue().get(key);
    if (currentValue != null && currentValue.equals(token)) {
        return true;  // 可重入
    }
    
    // 2. 尝试 SETNX
    Boolean result = redisTemplate.opsForValue()
        .setIfAbsent(key, token, expireSeconds, TimeUnit.SECONDS);
    
    return result != null && result;
}
```

关键设计点：
- **可重入**：同一 token（通常是线程标识）重复加锁不会死锁
- **自动过期**：使用 `setIfAbsent(key, value, ttl)` 原子操作，防止进程崩溃后锁无法释放
- **token 标识**：每次加锁使用唯一 token，确保只释放自己持有的锁

#### 4.3 看门狗自动续期

`lockWithDog` 在获取锁成功后，启动一个定时任务定期续期：

```java
public boolean lockWithDog(String key, String token, int expireSeconds) {
    boolean locked = lock(key, token, expireSeconds);
    if (locked) {
        // 每隔 expireSeconds/3 秒续期一次
        Timer timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                expireLock(key, token, expireSeconds);  // Lua 脚本续期
            }
        }, expireSeconds * 1000L / 3, expireSeconds * 1000L / 3);
    }
    return locked;
}
```

续期使用 Lua 脚本保证原子性：

```lua
-- expireLock Lua 脚本
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('expire', KEYS[1], ARGV[2])
else
    return 0
end
```

只有锁的持有者才能续期，防止误续其他节点的锁。

#### 4.4 释放锁

释放锁同样使用 Lua 脚本保证原子性：

```lua
-- unlock Lua 脚本
if redis.call('get', KEYS[1]) == ARGV[1] then
    return redis.call('del', KEYS[1])
else
    return 0
end
```

只有 token 匹配时才删除锁，防止误删其他节点的锁。

#### 4.5 应用场景：Leader 选举

MySQL 消费者和定时消息消费者都使用分布式锁做 Leader 选举：

```java
// MysqlMsgConsumer.consumeMySQLMsgWithLeaderCheck()
String lockKey = Constants.REDIS_KEY_MYSQL_MSG_CONSUMER + "HIGH_LEADER_CONSUMER_JAVA";
String token = UUID.randomUUID().toString();

boolean isLeader = distributeLock.lock(lockKey, token, 30);
if (isLeader) {
    try {
        consumeHighMsg();  // Leader 执行消费
    } finally {
        distributeLock.unlock(lockKey, token);  // 释放锁
    }
} else {
    Thread.sleep(10000);  // 非 Leader 等待 10s 后重试
}
```

每个优先级有独立的锁 Key，允许不同优先级并行消费：

| 优先级 | 锁 Key |
|--------|--------|
| High | `XMSG_mysql_msg_consumer_HIGH_LEADER_CONSUMER_JAVA` |
| Middle | `XMSG_mysql_msg_consumer_MIDDLE_LEADER_CONSUMER_JAVA` |
| Low | `XMSG_mysql_msg_consumer_LOW_LEADER_CONSUMER_JAVA` |
| Retry | `XMSG_mysql_msg_consumer_RETRY_LEADER_CONSUMER_JAVA` |
| Timer | `TIMER_MSG_LEADER_CONSUMER_JAVA` |

这种设计保证了：
- 同一优先级只有一个节点活跃消费，避免重复处理
- 不同优先级可以并行消费，提高吞吐量
- 节点故障时锁自动过期，其他节点自动接管

---

### 五、整体架构图

```
┌─────────────────────────────────────────────────────────────────┐
│                        客户端 (HTTP)                             │
│  POST /msg/send_msg                                             │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    SendMsgServiceImpl                            │
│  1. 校验模板状态                                                  │
│  2. 限额检查 (RateLimitService)                                   │
│  3. 路由决策                                                      │
└──────┬───────────────────┬──────────────────────┬───────────────┘
       │                   │                      │
       ▼                   ▼                      ▼
┌──────────────┐  ┌──────────────────┐  ┌─────────────────┐
│ 定时消息      │  │ MySQL 中转站      │  │ Kafka 中转站     │
│ SendToTimer  │  │ SendToMysql      │  │ SendToMq        │
│              │  │                  │  │                 │
│ t_msg_queue  │  │ t_msg_queue_low  │  │ low-topic       │
│ _timer       │  │ t_msg_queue_mid  │  │ middle-topic    │
│ + Redis ZSET │  │ t_msg_queue_high │  │ high-topic      │
│              │  │ t_msg_queue_retry│  │ retry-topic     │
└──────┬───────┘  └────────┬─────────┘  └────────┬────────┘
       │                   │                      │
       ▼                   ▼                      ▼
┌──────────────┐  ┌──────────────────┐  ┌─────────────────┐
│ TimerMsg     │  │ MysqlMsgConsumer │  │ KafkaMsgConsumer│
│ Consumer     │  │ (Leader选举)     │  │ (手动ACK)       │
│ (100ms轮询)  │  │ (1000ms轮询)     │  │                 │
│ (Leader选举) │  │                  │  │                 │
└──────┬───────┘  └────────┬─────────┘  └────────┬────────┘
       │                   │                      │
       └───────────────────┴──────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    DealMsgManager                                │
│  1. 获取模板（Redis 缓存 → DB）                                    │
│  2. 模板变量替换 ${key} → value                                   │
│  3. 策略模式选择渠道                                               │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MsgPushService (策略模式)                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────┐             │
│  │ Email       │  │ SMS         │  │ Lark        │             │
│  │ SMTP/TLS    │  │ (待实现)     │  │ (待实现)     │             │
│  └─────────────┘  └─────────────┘  └─────────────┘             │
└──────────────────────────┬──────────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────────┐
│                    MsgRecordService                              │
│  更新消息状态：Pending → Processing → Succeed/Failed              │
│  失败重试：retryCount++ → 发送到 retry 队列                       │
└─────────────────────────────────────────────────────────────────┘
```

---

### 六、线程池配置

系统使用两个独立的异步线程池，隔离定时消息和普通消息的处理：

| 线程池 | Bean 名称 | 核心线程 | 最大队列 | 用途 |
|--------|-----------|----------|----------|------|
| 定时消息池 | `timerMsgPoll` | 10 | 1000 | TimerMsgResendPollTask |
| 消息处理池 | `mysqlMsgDealPoll` | 10 | 5000 | MysqlMsgPollTask |

配置位于 `MsgCenterAsyncPool.java`，使用 `ThreadPoolTaskExecutor`。

---

### 七、Redis Key 总览

| Key 格式 | 类型 | TTL | 用途 |
|----------|------|-----|------|
| `XMSG_template_{templateId}` | String | 30s | 模板缓存 |
| `XMSG_msgrecord_{msgId}` | String | 30s | 消息记录缓存 |
| `XMSG_source_quota_{sourceId}{channel}` | String | 30s | 配额配置缓存 |
| `XMSG_rate_limit_count_{sourceId}_{channel}` | String | 窗口大小 | 普通消息限流计数 |
| `XMSG_rate_limit_count_timer_{sourceId}_{channel}` | String | 窗口大小 | 定时消息限流计数 |
| `Timer_Msgs` | ZSET | 永久 | 定时消息时间点缓存 |
| `XMSG_mysql_msg_consumer_{PRIORITY}_LEADER_CONSUMER_JAVA` | String | 30s | MySQL 消费者 Leader 锁 |
| `TIMER_MSG_LEADER_CONSUMER_JAVA` | String | 30s | 定时消息消费者 Leader 锁 |
| `EMPTY` 值的缓存 | String | 10s | 防缓存穿透空值标记 |
