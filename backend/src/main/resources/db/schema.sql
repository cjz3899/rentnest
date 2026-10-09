-- ============================================================
-- RentNest 全量建表脚本
-- 用法：
--   mysql -u root -p rentnest < schema.sql
-- （需先建库：CREATE DATABASE rentnest DEFAULT CHARACTER SET utf8mb4;）
-- 幂等：可重复执行，已存在的表会跳过
-- ============================================================

-- ---------- 用户与审计 ----------

CREATE TABLE IF NOT EXISTS users (
    id             BIGINT       PRIMARY KEY,
    phone          VARCHAR(20)  NOT NULL,
    password_hash  VARCHAR(100) NOT NULL,
    nickname       VARCHAR(50)  NOT NULL,
    avatar_file_id BIGINT       NULL,
    role           VARCHAR(20)  NOT NULL DEFAULT 'USER' COMMENT 'USER/LANDLORD/ADMIN',
    status         VARCHAR(20)  NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE / BANNED',
    real_name      VARCHAR(50)  NULL,
    email          VARCHAR(100) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_users_phone (phone)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户表';

CREATE TABLE IF NOT EXISTS admin_audit_log (
    id            BIGINT       PRIMARY KEY,
    operator_id   BIGINT       NOT NULL,
    operator_role VARCHAR(20)  NOT NULL,
    action        VARCHAR(100) NOT NULL,
    target_type   VARCHAR(50)  NULL,
    target_id     VARCHAR(64)  NULL,
    detail        JSON         NULL,
    ip            VARCHAR(50)  NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_audit_operator (operator_id),
    KEY idx_audit_created (created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '管理端关键操作审计日志';

-- ---------- 房源 ----------

CREATE TABLE IF NOT EXISTS house (
    id                  BIGINT         PRIMARY KEY,
    owner_user_id       BIGINT         NOT NULL COMMENT '房东/运营人员 user_id',
    title               VARCHAR(100)   NOT NULL COMMENT '房间名称',
    building            VARCHAR(50)    NOT NULL COMMENT '楼栋编号',
    floor_no            VARCHAR(20)    NULL COMMENT '所在楼层',
    orientation         VARCHAR(20)    NULL COMMENT '朝向',
    layout              VARCHAR(50)    NOT NULL COMMENT '房型，如 一室一厅',
    area                DECIMAL(8, 2)  NOT NULL COMMENT '面积（㎡）',
    rent                DECIMAL(10, 2) NOT NULL COMMENT '月租金（元）',
    deposit             DECIMAL(10, 2) NULL COMMENT '押金（元）',
    status              VARCHAR(20)    NOT NULL DEFAULT 'RENTABLE' COMMENT 'RENTABLE/RESERVED/RENTED/SUSPENDED（暂停出租不对用户端展示）',
    description         TEXT           NULL COMMENT '房源介绍',
    lease_term          VARCHAR(100)   NULL COMMENT '租赁周期说明',
    move_in_requirement VARCHAR(500)   NULL COMMENT '入住要求',
    notes               VARCHAR(1000)  NULL COMMENT '注意事项',
    is_recommended      TINYINT(1)     NOT NULL DEFAULT 0 COMMENT '首页推荐',
    created_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_house_status (status),
    KEY idx_house_owner (owner_user_id),
    KEY idx_house_rent (rent)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '房源表';

CREATE TABLE IF NOT EXISTS house_facility (
    house_id  BIGINT      NOT NULL,
    facility  VARCHAR(30) NOT NULL COMMENT 'BED/AC/WATER_HEATER/WASHER/NETWORK/OTHER',
    PRIMARY KEY (house_id, facility)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '房源配套设施';

CREATE TABLE IF NOT EXISTS house_media (
    id         BIGINT      PRIMARY KEY,
    house_id   BIGINT      NOT NULL,
    file_id    BIGINT      NOT NULL COMMENT 'file_record.id',
    media_type VARCHAR(10) NOT NULL COMMENT 'IMAGE / VIDEO',
    sort_order INT         NOT NULL DEFAULT 0,
    KEY idx_house_media_house (house_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '房源媒体';

-- ---------- 租赁预约（六态状态机）----------
-- PENDING 待审核 → CONTACTED 已联系 → VIEWING 看房安排中 → CONFIRMED 已确认 → COMPLETED 已完成
-- 任意非终态可 → CANCELLED 已取消

CREATE TABLE IF NOT EXISTS rental_appointment (
    id                    BIGINT       PRIMARY KEY,
    house_id              BIGINT       NOT NULL,
    user_id               BIGINT       NOT NULL COMMENT '提交预约的用户',
    contact_name          VARCHAR(50)  NOT NULL COMMENT '姓名',
    contact_phone         VARCHAR(20)  NOT NULL COMMENT '联系方式',
    expected_move_in_date DATE         NULL COMMENT '预计入住时间',
    lease_term            VARCHAR(50)  NULL COMMENT '租赁周期，如 12个月',
    remark                VARCHAR(500) NULL COMMENT '其他备注',
    status                VARCHAR(20)  NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/CONTACTED/VIEWING/CONFIRMED/COMPLETED/CANCELLED',
    handler_id            BIGINT       NULL COMMENT '处理人（房东/管理员）',
    handler_remark        VARCHAR(500) NULL COMMENT '处理备注',
    handled_at            DATETIME     NULL,
    created_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_appt_house (house_id),
    KEY idx_appt_user (user_id),
    KEY idx_appt_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '租赁预约';

CREATE TABLE IF NOT EXISTS rental_appointment_log (
    id             BIGINT       PRIMARY KEY,
    appointment_id BIGINT       NOT NULL,
    from_status    VARCHAR(20)  NULL,
    to_status      VARCHAR(20)  NOT NULL,
    operator_id    BIGINT       NOT NULL,
    remark         VARCHAR(500) NULL,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_appt_log (appointment_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '预约状态流转记录';

-- ---------- 公共服务（配送商品/订单，鲜奶示例）----------

CREATE TABLE IF NOT EXISTS product (
    id            BIGINT         PRIMARY KEY,
    name          VARCHAR(100)   NOT NULL,
    description   VARCHAR(1000)  NULL COMMENT '商品介绍',
    spec          VARCHAR(100)   NULL COMMENT '商品规格，如 250ml×12盒',
    price         DECIMAL(10, 2) NOT NULL COMMENT '单价（元）',
    unit          VARCHAR(20)    NULL COMMENT '计价单位，如 盒/箱',
    cover_file_id BIGINT         NULL,
    delivery_note VARCHAR(500)   NULL COMMENT '配送说明',
    status        VARCHAR(20)    NOT NULL DEFAULT 'ON_SALE' COMMENT 'ON_SALE / OFF_SALE',
    created_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_product_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '配送商品';

CREATE TABLE IF NOT EXISTS product_order (
    id               BIGINT         PRIMARY KEY,
    order_no         VARCHAR(32)    NOT NULL,
    user_id          BIGINT         NOT NULL,
    product_id       BIGINT         NOT NULL,
    quantity         INT            NOT NULL,
    total_amount     DECIMAL(10, 2) NOT NULL COMMENT '下单时单价×数量快照',
    receiver_name    VARCHAR(50)    NOT NULL COMMENT '收货人',
    receiver_phone   VARCHAR(20)    NOT NULL COMMENT '收货电话',
    delivery_address VARCHAR(200)   NOT NULL COMMENT '配送地址',
    remark           VARCHAR(500)   NULL,
    status           VARCHAR(20)    NOT NULL DEFAULT 'CREATED' COMMENT 'CREATED/DELIVERING/COMPLETED/CANCELLED',
    created_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_order_no (order_no),
    KEY idx_order_user (user_id),
    KEY idx_order_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '配送订单';

CREATE TABLE IF NOT EXISTS product_order_log (
    id          BIGINT       PRIMARY KEY,
    order_id    BIGINT       NOT NULL,
    from_status VARCHAR(20)  NULL,
    to_status   VARCHAR(20)  NOT NULL,
    operator_id BIGINT       NOT NULL,
    remark      VARCHAR(500) NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_porder_log (order_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '订单状态流转记录';

-- ---------- 租客服务（报修/反馈）----------

CREATE TABLE IF NOT EXISTS repair_ticket (
    id             BIGINT        PRIMARY KEY,
    user_id        BIGINT        NOT NULL COMMENT '提交用户（租客）',
    house_id       BIGINT        NULL COMMENT '关联房源（可空，允许未绑定）',
    location       VARCHAR(100)  NOT NULL COMMENT '问题发生位置',
    description    VARCHAR(1000) NOT NULL COMMENT '报修问题描述',
    contact_phone  VARCHAR(20)   NOT NULL COMMENT '联系方式',
    supplement     VARCHAR(500)  NULL COMMENT '补充说明',
    status         VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/PROCESSING/COMPLETED/CLOSED',
    handler_id     BIGINT        NULL,
    handler_remark VARCHAR(500)  NULL COMMENT '维修结果记录',
    handled_at     DATETIME      NULL,
    created_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_repair_user (user_id),
    KEY idx_repair_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '报修工单';

CREATE TABLE IF NOT EXISTS repair_media (
    id         BIGINT PRIMARY KEY,
    ticket_id  BIGINT NOT NULL,
    file_id    BIGINT NOT NULL,
    sort_order INT    NOT NULL DEFAULT 0,
    KEY idx_repair_media (ticket_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '报修图片';

CREATE TABLE IF NOT EXISTS feedback (
    id            BIGINT        PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    content       VARCHAR(1000) NOT NULL COMMENT '反馈内容',
    problem_desc  VARCHAR(500)  NULL COMMENT '问题描述',
    contact_phone VARCHAR(20)   NULL COMMENT '联系方式',
    status        VARCHAR(20)   NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/REPLIED/CLOSED',
    reply         VARCHAR(1000) NULL COMMENT '处理意见',
    replier_id    BIGINT        NULL,
    replied_at    DATETIME      NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_feedback_user (user_id),
    KEY idx_feedback_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '用户反馈';

CREATE TABLE IF NOT EXISTS feedback_media (
    id          BIGINT PRIMARY KEY,
    feedback_id BIGINT NOT NULL,
    file_id     BIGINT NOT NULL,
    sort_order  INT    NOT NULL DEFAULT 0,
    KEY idx_feedback_media (feedback_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '反馈图片附件';

-- ---------- 公告/FAQ 与社区 ----------

CREATE TABLE IF NOT EXISTS announcement (
    id           BIGINT        PRIMARY KEY,
    title        VARCHAR(200)  NOT NULL,
    content      TEXT          NOT NULL,
    type         VARCHAR(20)   NOT NULL DEFAULT 'NOTICE' COMMENT 'NOTICE 公告 / FAQ 常见问题（复用本表，供租客服务与 AI 助手共用）',
    status       VARCHAR(20)   NOT NULL DEFAULT 'PUBLISHED' COMMENT 'PUBLISHED / UNPUBLISHED',
    is_pinned    TINYINT(1)    NOT NULL DEFAULT 0,
    publisher_id BIGINT        NOT NULL,
    published_at DATETIME      NULL,
    created_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_ann_status_type (status, type)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '公告与FAQ';

CREATE TABLE IF NOT EXISTS post (
    id            BIGINT        PRIMARY KEY,
    user_id       BIGINT        NOT NULL,
    title         VARCHAR(200)  NOT NULL,
    content       VARCHAR(5000) NOT NULL,
    status        VARCHAR(20)   NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / REMOVED（管理端下架）',
    like_count    INT           NOT NULL DEFAULT 0 COMMENT '冗余计数，点赞服务内同事务维护',
    comment_count INT           NOT NULL DEFAULT 0 COMMENT '冗余计数，评论服务内同事务维护',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_post_user (user_id),
    KEY idx_post_status_created (status, created_at)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '社区帖子';

CREATE TABLE IF NOT EXISTS post_media (
    id         BIGINT PRIMARY KEY,
    post_id    BIGINT NOT NULL,
    file_id    BIGINT NOT NULL,
    sort_order INT    NOT NULL DEFAULT 0,
    KEY idx_post_media (post_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '帖子图片';

CREATE TABLE IF NOT EXISTS post_comment (
    id         BIGINT        PRIMARY KEY,
    post_id    BIGINT        NOT NULL,
    user_id    BIGINT        NOT NULL,
    parent_id  BIGINT        NULL COMMENT '被回复的评论 id，NULL 为一级评论',
    content    VARCHAR(1000) NOT NULL,
    status     VARCHAR(20)   NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL / REMOVED',
    created_at DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_comment_post (post_id),
    KEY idx_comment_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '帖子评论';

CREATE TABLE IF NOT EXISTS post_like (
    id         BIGINT   PRIMARY KEY,
    post_id    BIGINT   NOT NULL,
    user_id    BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_post_like (post_id, user_id),
    KEY idx_like_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '帖子点赞';

-- ---------- 聊天（Netty WS）----------
-- 幂等键：UNIQUE(sender_id, client_msg_id)，重连重发插入冲突按已投递处理，返回原消息 ACK
-- 未读数：conversation_member.last_read_msg_id 位点，count(msg.id > last_read) 计算

CREATE TABLE IF NOT EXISTS conversation (
    id         BIGINT       PRIMARY KEY,
    conv_type  VARCHAR(10)  NOT NULL COMMENT 'SINGLE / GROUP',
    name       VARCHAR(100) NULL COMMENT '群名（单聊为空）',
    owner_id   BIGINT       NULL COMMENT '群主（单聊为空）',
    single_key VARCHAR(40)  NULL COMMENT '单聊查找键：{minUserId}:{maxUserId}，唯一',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conv_single (single_key)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '聊天会话';

CREATE TABLE IF NOT EXISTS conversation_member (
    id               BIGINT      PRIMARY KEY,
    conversation_id  BIGINT      NOT NULL,
    user_id          BIGINT      NOT NULL,
    member_role      VARCHAR(20) NOT NULL DEFAULT 'MEMBER' COMMENT 'OWNER / MEMBER',
    last_read_msg_id BIGINT      NOT NULL DEFAULT 0 COMMENT '已读位点',
    banned_at        DATETIME    NULL COMMENT '禁言时间（非空即被禁言）',
    joined_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_conv_member (conversation_id, user_id),
    KEY idx_member_user (user_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '会话成员';

CREATE TABLE IF NOT EXISTS chat_message (
    id              BIGINT        PRIMARY KEY,
    conversation_id BIGINT        NOT NULL,
    sender_id       BIGINT        NOT NULL,
    client_msg_id   VARCHAR(64)   NOT NULL COMMENT '客户端生成 UUID，幂等键',
    content_type    VARCHAR(20)   NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT / IMAGE',
    content         VARCHAR(4000) NOT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_msg_client (sender_id, client_msg_id),
    KEY idx_msg_conv (conversation_id, id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '聊天消息（先落库后 ACK，历史以此为准）';

-- ---------- 知识库（RAG）----------
-- 向量本体存 Redis（RedisVectorStore），索引丢失可从 kb_chunk 重新 embed 重建

CREATE TABLE IF NOT EXISTS kb_document (
    id            BIGINT        PRIMARY KEY,
    file_id       BIGINT        NOT NULL COMMENT 'file_record.id（原始文件）',
    original_name VARCHAR(200)  NOT NULL,
    doc_type      VARCHAR(20)   NOT NULL COMMENT 'pdf / txt / md / docx',
    title         VARCHAR(200)  NULL COMMENT '展示标题（默认取文件名）',
    status        VARCHAR(20)   NOT NULL DEFAULT 'PARSING' COMMENT 'PARSING / INDEXED / INDEX_FAILED',
    error_msg     VARCHAR(1000) NULL COMMENT '摄取/索引失败原因，支持重试',
    chunk_count   INT           NOT NULL DEFAULT 0,
    uploader_id   BIGINT        NOT NULL,
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_kb_doc_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '知识库文档';

CREATE TABLE IF NOT EXISTS kb_chunk (
    id          BIGINT  PRIMARY KEY,
    document_id BIGINT  NOT NULL,
    seq_no      INT     NOT NULL COMMENT '文档内片段序号',
    content     TEXT    NOT NULL,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    KEY idx_kb_chunk_doc (document_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '知识库文档片段';

-- ---------- 文件记录 ----------
-- uuid 对外暴露（不可猜测）；storage_path 仅存储实现内部使用，不对外

CREATE TABLE IF NOT EXISTS file_record (
    id            BIGINT       PRIMARY KEY,
    uuid          VARCHAR(36)  NOT NULL COMMENT '对外标识',
    original_name VARCHAR(255) NOT NULL,
    mime_type     VARCHAR(100) NOT NULL,
    size_bytes    BIGINT       NOT NULL,
    sha256        CHAR(64)     NOT NULL,
    storage_type  VARCHAR(10)  NOT NULL COMMENT 'MINIO',
    storage_path  VARCHAR(500) NOT NULL COMMENT '存储实现内部的 key/路径',
    visibility    VARCHAR(10)  NOT NULL DEFAULT 'PRIVATE' COMMENT 'PUBLIC / PRIVATE（私有文件仅 owner 或 ADMIN 可读）',
    owner_id      BIGINT       NOT NULL,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_file_uuid (uuid),
    KEY idx_file_owner (owner_id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COMMENT '文件记录';
