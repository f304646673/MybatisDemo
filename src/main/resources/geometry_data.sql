CREATE TABLE `geometry_data` (
    `id` BIGINT(20) unsigned NOT NULL AUTO_INCREMENT COMMENT '自增主键',
    `geometry` GEOMETRY NOT NULL COMMENT '几何信息',
    `create_time` timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    `update_time` timestamp  NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '更新时间',
    `deleted` boolean DEFAULT false COMMENT '是否已被删除',
    PRIMARY KEY (`id`),
    SPATIAL INDEX `spatial_geometry` (`geometry`)
) COMMENT='几何数据表';