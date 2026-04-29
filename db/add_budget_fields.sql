-- 为消费预算表添加超支检查相关字段
ALTER TABLE `xiaofeiyusuan` 
ADD COLUMN `shifouzhichao` varchar(10) DEFAULT NULL COMMENT '是否超支 (是/否)',
ADD COLUMN `dangyueleiji` float DEFAULT NULL COMMENT '当月累计金额';
