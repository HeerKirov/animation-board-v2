CREATE TABLE "user"(
    id SERIAL PRIMARY KEY,
    username VARCHAR(255) NOT NULL,
    is_staff BOOLEAN NOT NULL DEFAULT FALSE,        -- 用户在本系统中被单独设置为管理员。BS系统的管理员也是本系统的管理员
    setting JSONB NOT NULL DEFAULT '{}'             -- 用户的个性配置，比如推送开关、统计开关、时间表偏好
);
CREATE UNIQUE INDEX user__username__index ON "user"(username);

CREATE TABLE message(
    id BIGSERIAL PRIMARY KEY,
    owner_id INTEGER NOT NULL,
    type SMALLINT NOT NULL,                         -- 消息类型
    content JSONB NOT NULL,                         -- 消息体
    read BOOLEAN NOT NULL DEFAULT FALSE,            -- 已读标记
    create_time TIMESTAMP NOT NULL
);
CREATE INDEX message__owner_id__index ON message(owner_id);

CREATE TABLE animation(
    -- 基础信息
    id SERIAL PRIMARY KEY,
    title VARCHAR(128) NOT NULL,
    origin_title VARCHAR(128) DEFAULT NULL,
    other_title VARCHAR(128) DEFAULT NULL,
    cover VARCHAR(256) DEFAULT NULL,                -- 封面文件名
    -- 制作信息
    original_work_type SMALLINT,                    -- 原作类型
    -- 放送信息
    publish_type SMALLINT,                          -- 放送类型
    publish_time DATE,                              -- 放送时间，以月为最小单位
    episode_duration INTEGER,                       -- 平均单话时长
    total_episodes INTEGER NOT NULL DEFAULT 1,      -- 总集数
    published_episodes INTEGER NOT NULL DEFAULT 0,  -- 已发布的集数
    published_record JSONB NOT NULL,                -- 已发布的集数时间点
    publish_plan JSONB NOT NULL,                    -- 后续的发布计划时间点
    -- 描述信息
    introduction TEXT,                              -- 内容介绍
    keyword VARCHAR(255),                           -- 关键词
    sex_limit_level SMALLINT,                       -- 限制等级(性)
    violence_limit_level SMALLINT,                  -- 限制等级(暴力)
    -- 关联
    relations JSONB NOT NULL,                       -- 原始关联表
    relations_topology JSONB NOT NULL,              -- 拓扑后的完整关联表
    -- 元信息
    create_time TIMESTAMP NOT NULL,                 -- 条目创建时间
    update_time TIMESTAMP NOT NULL,                 -- 条目上次更新时间
    creator INTEGER NOT NULL,                       -- 条目创建者
    updater INTEGER NOT NULL                        -- 条目上次更新者
);

CREATE TABLE tag(
    id SERIAL PRIMARY KEY,
    name VARCHAR(16) NOT NULL,
    introduction TEXT,                              -- 标签简介

    create_time TIMESTAMP NOT NULL,                 -- 条目创建时间
    update_time TIMESTAMP NOT NULL,                 -- 条目上次更新时间
    creator INTEGER NOT NULL,                       -- 条目创建者
    updater INTEGER NOT NULL                        -- 条目上次更新者
);
CREATE UNIQUE INDEX tag__name__index ON tag(name);

CREATE TABLE staff(
    id SERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    origin_name VARCHAR(64),
    remark VARCHAR(64),                             -- 非正式别称
    cover VARCHAR(256) DEFAULT NULL,                -- 头像文件名称
    is_organization BOOLEAN NOT NULL,               -- 此STAFF是组织而非个人
    occupation SMALLINT DEFAULT NULL,               -- 此STAFF的职业或行业

    create_time TIMESTAMP NOT NULL,                 -- 条目创建时间
    update_time TIMESTAMP NOT NULL,                 -- 条目上次更新时间
    creator INTEGER NOT NULL,                       -- 条目创建者
    updater INTEGER NOT NULL                        -- 条目上次更新者
);
CREATE UNIQUE INDEX staff__name__index ON staff(name);

CREATE TABLE animation_staff_relation(              -- staff & animation关联
    id BIGSERIAL PRIMARY KEY,
    animation_id INTEGER NOT NULL,
    staff_id INTEGER NOT NULL,
    staff_type SMALLINT NOT NULL                    -- STAFF在动画中的作用位置
);
CREATE UNIQUE INDEX animation_staff_relation__index ON animation_staff_relation(animation_id, staff_id, staff_type);

CREATE TABLE animation_tag_relation(                -- tag & animation关联
    id BIGSERIAL PRIMARY KEY,
    animation_id INTEGER NOT NULL,
    tag_id INTEGER NOT NULL
);
CREATE UNIQUE INDEX animation_tag_relation__index ON animation_tag_relation(animation_id, tag_id);

CREATE TABLE comment(
    id SERIAL PRIMARY KEY,
    owner_id INTEGER NOT NULL,
    animation_id INTEGER NOT NULL,

    score INTEGER,                                  -- 评分[1, 10]
    title TEXT,                                     -- 评论标题
    article TEXT,                                   -- 评论内容

    create_time TIMESTAMP NOT NULL,                 -- 首次评论时间
    update_time TIMESTAMP NOT NULL                  -- 上次更新时间
);
CREATE INDEX comment__owner_id__index ON comment(owner_id);
CREATE UNIQUE INDEX comment__index ON comment(owner_id, animation_id);

CREATE TABLE record(
    id BIGSERIAL PRIMARY KEY,
    owner_id INTEGER NOT NULL,
    animation_id INTEGER NOT NULL,
    seen_original BOOLEAN NOT NULL DEFAULT FALSE,  -- 看过原作
    in_diary BOOLEAN NOT NULL DEFAULT FALSE,        -- 在日记本中

    scatter_record JSONB NOT NULL DEFAULT '[]',     -- 独立的观看记录，记录每一话的观看时间
    progress_count INTEGER NOT NULL,                -- 观看的进度数

    -- 活跃记录
    last_active_time TIMESTAMP DEFAULT NULL,        -- 上次活跃的时间
    last_active_event JSONB NOT NULL DEFAULT '{}',  -- 上次活跃的事件内容

    create_time TIMESTAMP NOT NULL,                 -- 条目创建时间
    update_time TIMESTAMP NOT NULL                  -- 上次更新时间
);
CREATE INDEX record__owner_id__index ON record(owner_id);
CREATE UNIQUE INDEX record__index ON record(owner_id, animation_id);

CREATE TABLE record_progress(
    id BIGSERIAL PRIMARY KEY,
    record_id BIGINT NOT NULL,

    ordinal INTEGER NOT NULL,                       -- 此进度的序列号
    watched_episodes INTEGER NOT NULL DEFAULT 0,    -- 观看进度
    watched_record JSONB NOT NULL DEFAULT '[]',     -- 每一话的观看记录
    start_time TIMESTAMP,                           -- 进度开始时间
    finish_time TIMESTAMP                           -- 进度结束时间
);
CREATE INDEX record_progress__record_id__index ON record_progress(record_id);

CREATE TABLE statistics(
    id BIGSERIAL PRIMARY KEY,
    owner_id INTEGER NOT NULL,

    type VARCHAR(32) NOT NULL,                      -- 统计类型
    key VARCHAR(256),                               -- 统计id
    content JSONB NOT NULL,                         -- 统计内容

    create_time TIMESTAMP NOT NULL,                 -- 统计创建时间
    update_time TIMESTAMP NOT NULL                  -- 上次更新时间，可告知用户何时更新的统计数据
);
CREATE INDEX statistics__owner_id__index ON statistics(owner_id);
CREATE UNIQUE INDEX statistics__index ON statistics(owner_id, type, key);