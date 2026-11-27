-- ==========================================================
-- 基于 Flowable 的企业固定资产审批系统 - 数据库初始化脚本
-- 说明：本脚本包含全部 6 张业务表及初始测试数据；
--       Flowable 工作流引擎自带的表（ACT_*）会在后端启动时自动创建。
-- ==========================================================

SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS asset_approval
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE asset_approval;

-- 1. 部门表
DROP TABLE IF EXISTS sys_dept;
CREATE TABLE sys_dept (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '部门ID',
  dept_name VARCHAR(64) NOT NULL COMMENT '部门名称',
  leader_name VARCHAR(64) DEFAULT NULL COMMENT '部门负责人姓名',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='部门表';

-- 2. 用户表
DROP TABLE IF EXISTS sys_user;
CREATE TABLE sys_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
  username VARCHAR(64) NOT NULL UNIQUE COMMENT '登录账号',
  password VARCHAR(128) NOT NULL COMMENT '登录密码（后端启动自动升级为BCrypt密文）',
  real_name VARCHAR(64) NOT NULL COMMENT '真实姓名',
  dept_id BIGINT DEFAULT NULL COMMENT '所属部门ID',
  role VARCHAR(32) NOT NULL COMMENT '角色标识',
  role_name VARCHAR(32) NOT NULL COMMENT '角色中文名称',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 3. 固定资产台账表
DROP TABLE IF EXISTS asset_info;
CREATE TABLE asset_info (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '资产ID',
  asset_code VARCHAR(64) NOT NULL UNIQUE COMMENT '唯一资产编码（用于生成条形码/二维码）',
  asset_name VARCHAR(128) NOT NULL COMMENT '资产名称',
  category VARCHAR(64) NOT NULL COMMENT '资产分类',
  spec VARCHAR(128) DEFAULT NULL COMMENT '规格型号',
  price DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '采购单价（元）',
  status TINYINT NOT NULL DEFAULT 1 COMMENT '资产状态：1-在库闲置，2-在用，3-借出，4-维修中，5-已报废',
  user_id BIGINT DEFAULT NULL COMMENT '当前使用人ID',
  dept_id BIGINT DEFAULT NULL COMMENT '当前所属部门ID',
  inbound_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '入库时间',
  remark VARCHAR(255) DEFAULT NULL COMMENT '备注',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='固定资产台账表';

-- 4. 资产业务申请单表
DROP TABLE IF EXISTS asset_apply;
CREATE TABLE asset_apply (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请单ID',
  apply_no VARCHAR(64) NOT NULL UNIQUE COMMENT '申请单号',
  apply_type VARCHAR(32) NOT NULL COMMENT '申请类型：PURCHASE-采购，USE-领用，BORROW-借用，REPAIR-报修，SCRAP-报废',
  applicant_id BIGINT NOT NULL COMMENT '申请人ID',
  applicant_name VARCHAR(64) NOT NULL COMMENT '申请人姓名',
  dept_id BIGINT NOT NULL COMMENT '申请人部门ID',
  asset_id BIGINT DEFAULT NULL COMMENT '关联资产ID',
  asset_name VARCHAR(128) NOT NULL COMMENT '申请资产名称',
  category VARCHAR(64) DEFAULT NULL COMMENT '资产分类',
  spec VARCHAR(128) DEFAULT NULL COMMENT '规格型号',
  quantity INT NOT NULL DEFAULT 1 COMMENT '申请数量',
  amount DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT '预计总金额（元）',
  reason VARCHAR(255) DEFAULT NULL COMMENT '申请原因',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '审批状态：PENDING-审批中，REJECTED-已驳回，FINISHED-已办结',
  process_instance_id VARCHAR(64) DEFAULT NULL COMMENT 'Flowable流程实例ID',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '提交时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-正常，1-已删除'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产业务申请单表';

-- 5. 资产盘点记录表
DROP TABLE IF EXISTS asset_inventory;
CREATE TABLE asset_inventory (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '盘点ID',
  inventory_no VARCHAR(64) NOT NULL UNIQUE COMMENT '盘点单号',
  title VARCHAR(128) NOT NULL COMMENT '盘点任务名称',
  checker_id BIGINT NOT NULL COMMENT '盘点人ID',
  checker_name VARCHAR(64) NOT NULL COMMENT '盘点人姓名',
  expected_count INT NOT NULL DEFAULT 0 COMMENT '账面应盘数量',
  actual_count INT NOT NULL DEFAULT 0 COMMENT '实际盘点数量',
  result VARCHAR(32) NOT NULL DEFAULT 'NORMAL' COMMENT '盘点结果：NORMAL-账实相符，ABNORMAL-存在差异',
  remark VARCHAR(255) DEFAULT NULL COMMENT '盘点说明',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '盘点时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='资产盘点记录表';

-- 6. 系统只读审计日志表
DROP TABLE IF EXISTS sys_log;
CREATE TABLE sys_log (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '日志ID',
  user_id BIGINT DEFAULT NULL COMMENT '操作人用户ID',
  operator VARCHAR(64) NOT NULL COMMENT '操作人姓名',
  module VARCHAR(64) NOT NULL COMMENT '所属模块',
  action VARCHAR(64) NOT NULL COMMENT '操作类型',
  detail VARCHAR(255) DEFAULT NULL COMMENT '操作详情',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统只读审计日志表';

-- 7. 忘记密码逐级重置审批表
DROP TABLE IF EXISTS sys_pwd_reset;
CREATE TABLE sys_pwd_reset (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '申请ID',
  user_id BIGINT NOT NULL COMMENT '申请人ID',
  username VARCHAR(64) NOT NULL COMMENT '申请人账号',
  real_name VARCHAR(64) NOT NULL COMMENT '申请人姓名',
  dept_id BIGINT DEFAULT NULL COMMENT '所属部门ID',
  status VARCHAR(32) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/APPROVED/REJECTED',
  reviewer_name VARCHAR(64) DEFAULT NULL COMMENT '审批人姓名',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '申请时间',
  update_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='忘记密码重置审批表';

-- ==========================================================
-- 初始化测试数据（所有账号初始密码均为：123456）
-- ==========================================================

INSERT INTO sys_dept (id, dept_name, leader_name) VALUES
(1, '研发部', '张主管'),
(2, '行政资产部', '刘部长'),
(3, '财务部', '陈总监');

INSERT INTO sys_user (id, username, password, real_name, dept_id, role, role_name) VALUES
(1, 'emp1', '123456', '小陈（员工）', 1, 'EMPLOYEE', '普通员工'),
(2, 'manager1', '123456', '张主管', 1, 'DEPT_MANAGER', '部门主管'),
(3, 'admin1', '123456', '老王', 2, 'ASSET_ADMIN', '资产管理员'),
(4, 'finance1', '123456', '王财务', 3, 'FINANCE_MANAGER', '财务主管'),
(5, 'emp2', '123456', '林小雨', 1, 'EMPLOYEE', '普通员工'),
(6, 'emp3', '123456', '赵子豪', 1, 'EMPLOYEE', '普通员工'),
(7, 'emp4', '123456', '周晓梅', 2, 'EMPLOYEE', '普通员工'),
(8, 'emp5', '123456', '孙浩然', 3, 'EMPLOYEE', '普通员工'),
(9, 'manager2', '123456', '刘部长', 2, 'DEPT_MANAGER', '部门主管'),
(10, 'manager3', '123456', '陈总监', 3, 'DEPT_MANAGER', '部门主管');

INSERT INTO asset_info (asset_code, asset_name, category, spec, price, status, user_id, dept_id, remark) VALUES
('FA-202611-0001', 'MacBook Pro 14寸', '办公设备', 'M3 / 16G / 512G', 12999.00, 2, 1, 1, '研发日常办公使用'),
('FA-202611-0002', 'Dell PowerEdge 服务器', '服务器', 'R750 / 64G / 2T', 28500.00, 2, 2, 1, '测试环境部署'),
('FA-202611-0003', 'ThinkPad X1 Carbon', '办公设备', 'i7 / 16G / 1T', 9800.00, 1, NULL, 2, '在库待领用'),
('FA-202611-0004', '示波器测试仪', '测试仪器', 'DSO-X 2002A', 6500.00, 3, 1, 1, '临时借用测试'),
('FA-202611-0005', '华为千兆交换机', '网络设备', 'S5720-28P', 3200.00, 1, NULL, 2, '在库备用'),
('FA-202611-0006', '老旧台式主机', '办公设备', 'i5-8400 / 8G', 3500.00, 5, NULL, 2, '已到达使用年限报废'),
('FA-202611-1001', 'Dell 27寸 4K显示器', '办公设备', 'U2723QE / 4K', 3699.00, 2, 1, 1, '小陈双屏办公'),
('FA-202611-1002', 'ThinkPad P16 移动工作站', '办公设备', 'i9 / 32G / 1T', 15800.00, 2, 2, 1, '张主管日常审批用机'),
('FA-202611-1003', '惠普激光多功能一体机', '办公设备', 'LaserJet M428fdw', 3200.00, 2, 3, 2, '老王资产标签打印专用'),
('FA-202611-1004', '联想 ThinkBook 16+', '办公设备', 'i7 / 16G / 1T', 6499.00, 2, 3, 2, '资产管理日常盘点用机'),
('FA-202611-1005', 'MacBook Air 15寸', '办公设备', 'M3 / 16G / 512G', 10499.00, 2, 4, 3, '王财务预算核算专用'),
('FA-202611-1006', '财务票据高速扫描仪', '办公设备', 'Canon DR-C225', 2800.00, 2, 4, 3, '财务报销单据归档'),
('FA-202611-1008', 'MacBook Pro 16寸', '办公设备', 'M3 Pro / 36G / 1T', 19999.00, 2, 5, 1, '林小雨前端开发用机'),
('FA-202611-1011', '联想拯救者 Y9000P', '办公设备', 'i9 / 32G / RTX4060', 10999.00, 2, 6, 1, '赵子豪主力开发机'),
('FA-202611-1012', '斑马条码标签打印机', '办公设备', 'ZD888T', 1650.00, 2, 7, 2, '周晓梅固定资产贴标'),
('FA-202611-1014', 'Dell Latitude 5440', '办公设备', 'i5 / 16G / 512G', 5899.00, 2, 8, 3, '孙浩然财务核算用机'),
('FA-202611-1015', '华为 MateBook X Pro', '办公设备', 'Ultra7 / 32G / 1T', 11999.00, 2, 9, 2, '刘部长移动办公本'),
('FA-202611-1016', 'iPad Pro 12.9寸', '办公设备', 'M2 / 256G', 8299.00, 2, 10, 3, '陈总监移动审批平板'),
('FA-202611-1017', '群晖 NAS 企业存储服务器', '服务器', 'DS1621+ / 32T', 14500.00, 1, NULL, 2, '在库待部署'),
('FA-202611-1018', '思科企业级核心路由器', '网络设备', 'ISR 4331', 8900.00, 1, NULL, 2, '在库备用');
