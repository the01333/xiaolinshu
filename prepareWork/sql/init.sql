CREATE TABLE `t_user`
(
    `id`             bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `xiaohashu_id`   varchar(15)     NOT NULL COMMENT '小哈书号(唯一凭证)',
    `password`       varchar(64)              DEFAULT NULL COMMENT '密码',
    `nickname`       varchar(24)     NOT NULL COMMENT '昵称',
    `avatar`         varchar(120)             DEFAULT NULL COMMENT '头像',
    `birthday`       date                     DEFAULT NULL COMMENT '生日',
    `background_img` varchar(120)             DEFAULT NULL COMMENT '背景图',
    `phone`          varchar(11)     NOT NULL COMMENT '手机号',
    `sex`            tinyint                  DEFAULT '0' COMMENT '性别(0：女 1：男)',
    `status`         tinyint         NOT NULL DEFAULT '0' COMMENT '状态(0：启用 1：禁用)',
    `introduction`   varchar(100)             DEFAULT NULL COMMENT '个人简介',
    `create_time`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     bit(1)          NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_xiaohashu_id` (`xiaohashu_id`),
    UNIQUE KEY `uk_phone` (`phone`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户表';

CREATE TABLE `t_role`
(
    `id`          bigint unsigned                        NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_name`   varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色名',
    `role_key`    varchar(32) COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '角色唯一标识',
    `status`      tinyint                                NOT NULL DEFAULT '0' COMMENT '状态(0：启用 1：禁用)',
    `sort`        int unsigned                           NOT NULL DEFAULT 0 COMMENT '管理系统中的显示顺序',
    `remark`      varchar(255) COLLATE utf8mb4_unicode_ci         DEFAULT NULL COMMENT '备注',
    `create_time` datetime                               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime                               NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '最后一次更新时间',
    `is_deleted`  bit(1)                                 NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
    PRIMARY KEY (`id`) USING BTREE,
    UNIQUE KEY `uk_role_key` (`role_key`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='角色表';

CREATE TABLE `t_permission`
(
    `id`             bigint unsigned                         NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `parent_id`      bigint unsigned                         NOT NULL DEFAULT '0' COMMENT '父ID',
    `name`           varchar(16) COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '权限名称',
    `type`           tinyint unsigned                        NOT NULL COMMENT '类型(1：目录 2：菜单 3：按钮)',
    `menu_url`       varchar(32) COLLATE utf8mb4_unicode_ci  NOT NULL DEFAULT '' COMMENT '菜单路由',
    `menu_icon`      varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT '' COMMENT '菜单图标',
    `sort`           int unsigned                            NOT NULL DEFAULT 0 COMMENT '管理系统中的显示顺序',
    `permission_key` varchar(64) COLLATE utf8mb4_unicode_ci  NOT NULL COMMENT '权限标识',
    `status`         tinyint unsigned                        NOT NULL DEFAULT '0' COMMENT '状态(0：启用；1：禁用)',
    `create_time`    datetime                                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`    datetime                                NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`     bit(1)                                  NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='权限表';

CREATE TABLE `t_user_role_rel`
(
    `id`          bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `user_id`     bigint unsigned NOT NULL COMMENT '用户ID',
    `role_id`     bigint unsigned NOT NULL COMMENT '角色ID',
    `create_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`  bit(1)          NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户角色表';

CREATE TABLE `t_role_permission_rel`
(
    `id`            bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    `role_id`       bigint unsigned NOT NULL COMMENT '角色ID',
    `permission_id` bigint unsigned NOT NULL COMMENT '权限ID',
    `create_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time`   datetime        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `is_deleted`    bit(1)          NOT NULL DEFAULT b'0' COMMENT '逻辑删除(0：未删除 1：已删除)',
    PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8mb4
  COLLATE = utf8mb4_unicode_ci COMMENT ='用户权限表';
