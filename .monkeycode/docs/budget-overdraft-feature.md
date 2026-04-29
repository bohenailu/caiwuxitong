# 预算管理超支检查功能实现说明

## 功能概述

在预算管理模块中添加了两个新属性：
- **是否超支（shifouzhichao）**：标识当前预算是否已超支
- **当月累计金额（dangyueleiji）**：当月收入和支出的累计总和

当添加或修改收入/支出记录时，系统会自动检查该用户的预算是否超支，并更新预算记录。

## 数据库变更

**文件位置**: `db/add_budget_fields.sql`

执行以下 SQL 语句添加新字段：

```sql
ALTER TABLE `xiaofeiyusuan` 
ADD COLUMN `shifouzhichao` varchar(10) DEFAULT NULL COMMENT '是否超支 (是/否)',
ADD COLUMN `dangyueleiji` float DEFAULT NULL COMMENT '当月累计金额';
```

**重要**: 在部署前必须先执行此 SQL 脚本更新数据库表结构。

## 代码修改清单

### 1. 实体类修改

#### 1.1 XiaofeiyusuanEntity.java
- 路径：`src/main/java/com/entity/XiaofeiyusuanEntity.java`
- 新增属性：
  - `private String shifouzhichao;` - 是否超支
  - `private Float dangyueleiji;` - 当月累计金额
- 新增 getter/setter 方法

#### 1.2 XiaofeiyusuanVO.java
- 路径：`src/main/java/com/entity/vo/XiaofeiyusuanVO.java`
- 新增属性及 getter/setter 方法（与 Entity 相同）

#### 1.3 XiaofeiyusuanModel.java
- 路径：`src/main/java/com/entity/model/XiaofeiyusuanModel.java`
- 新增属性及 getter/setter 方法（与 Entity 相同）

**注意**: `XiaofeiyusuanView` 继承自`XiaofeiyusuanEntity`，因此会自动包含新字段，无需修改。

### 2. Mapper 配置修改

#### XiaofeiyusuanDao.xml
- 路径：`src/main/resources/mapper/XiaofeiyusuanDao.xml`
- 在 `resultMap` 中添加字段映射：
  ```xml
  <result property="shifouzhichao" column="shifouzhichao"/>
  <result property="dangyueleiji" column="dangyueleiji"/>
  ```

### 3. Service 层修改

#### 3.1 XiaofeiyusuanService.java（接口）
- 路径：`src/main/java/com/service/XiaofeiyusuanService.java`
- 新增方法声明：
  ```java
  void checkBudgetOverdraft(String zhanghao);
  ```

#### 3.2 XiaofeiyusuanServiceImpl.java（实现类）
- 路径：`src/main/java/com/service/impl/XiaofeiyusuanServiceImpl.java`
- 新增依赖注入：
  - `ZhichuDao zhichuDao`
  - `ShouruDao shouruDao`
- 实现 `checkBudgetOverdraft` 方法，逻辑如下：
  1. 获取当前年月
  2. 查询用户当月的所有支出记录
  3. 查询用户当月的所有收入记录
  4. 计算收入和 + 支出和 = 当月累计金额
  5. 查询用户的预算记录
  6. 比较：当月累计 > 预算金额 → 设置"是否超支"为"是"，否则为"否"
  7. 更新预算记录的"当月累计金额"和"是否超支"字段

### 4. Controller 层修改

#### 4.1 ZhichuController.java
- 路径：`src/main/java/com/controller/ZhichuController.java`
- 注入 `XiaofeiyusuanService`
- 修改以下方法，在保存/更新后调用预算检查：
  - `save()` - 后端保存
  - `add()` - 前端保存
  - `update()` - 修改

#### 4.2 ShouruController.java
- 路径：`src/main/java/com/controller/ShouruController.java`
- 注入 `XiaofeiyusuanService`
- 修改以下方法，在保存/更新后调用预算检查：
  - `save()` - 后端保存
  - `add()` - 前端保存
  - `update()` - 修改

## 业务逻辑说明

### 超支检查逻辑

```
当用户添加/修改收入或支出记录时：
1. 从 Session 获取当前用户账号（zhanghao）
2. 查询该用户当月的所有收入记录，计算总收入
3. 查询该用户当月的所有支出记录，计算总支出
4. 当月累计金额 = 总收入 + 总支出
5. 查询用户的预算金额
6. 如果 当月累计金额 > 预算金额：
   - 设置 是否超支 = "是"
   否则：
   - 设置 是否超支 = "否"
7. 更新预算表的 dangyueleiji 和 shifouzhichao 字段
```

### 触发时机

- 保存新的收入记录时（`/shouru/save`、`/shouru/add`）
- 修改收入记录时（`/shouru/update`）
- 保存新的支出记录时（`/zhichu/save`、`/zhichu/add`）
- 修改支出记录时（`/zhichu/update`）

### 超支判断规则

**当月累计金额 = 当月收入总和 + 当月支出总和**

如果 **当月累计金额 > 预算金额**，则标记为超支（是），否则为未超支（否）。

## 前端使用建议

前端在显示预算列表时，可以根据 `shifouzhichao` 字段：
- 显示超支状态（如红色高亮超支记录）
- 显示当月累计金额，让用户了解已使用金额
- 可以计算剩余额度：预算金额 - 当月累计金额

示例显示逻辑：
```javascript
// Vue.js 示例
<div :class="{'over-budget': item.shifouzhichao === '是'}">
  <span>预算金额：{{ item.yusuanjine }}</span>
  <span>当月累计：{{ item.dangyueleiji }}</span>
  <span v-if="item.shifouzhichao === '是'" class="warning">已超支</span>
  <span v-else>未超支</span>
</div>
```

## 注意事项

1. **必须先执行数据库迁移脚本** `db/add_budget_fields.sql`，否则会出现字段不存在的错误。

2. **会话管理**：超支检查依赖于 Session 中的 `username` 属性，确保用户已登录。

3. **事务处理**：`checkBudgetOverdraft` 方法使用了 `@Transactional` 注解，确保数据一致性。

4. **性能考虑**：每次保存收支记录都会查询当月所有记录并计算总和。如果数据量很大，可以考虑：
   - 添加日期范围索引
   - 使用数据库的 SUM 聚合函数代替循环累加
   - 添加缓存机制

5. **预算记录存在性**：如果用户没有创建预算记录，超支检查不会报错但也不会更新任何数据。

## 测试建议

1. **基础功能测试**：
   - 添加一条收入记录，检查预算的当月累计是否正确更新
   - 添加一条支出记录，检查预算的当月累计是否正确更新
   - 修改收入/支出金额，检查累计金额是否正确更新

2. **超支判断测试**：
   - 创建预算，设置预算金额为 1000
   - 添加收支记录，使累计金额为 800，检查是否超支为"否"
   - 继续添加收支记录，使累计金额达到 1200，检查是否超支为"是"

3. **边界情况测试**：
   - 累计金额等于预算金额时的状态
   - 不同月份的收支记录不应影响当月预算
   - 删除收支记录后的处理

4. **多用户测试**：
   - 不同用户的预算应该独立计算，互不影响

## 后续优化建议

1. 支持按不同预算周期（每周、每月）检查超支
2. 添加超支预警功能（如达到预算 80% 时提醒）
3. 提供预算管理首页统计图表，直观展示超支情况
4. 支持多个预算类别（如餐饮预算、交通预算等）
