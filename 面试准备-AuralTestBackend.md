# AuralTestBackend 面试问题预测与参考答案

---

## 一、项目整体架构

### Q1：请简单介绍一下你的项目？

**答：** 这是一个微信小程序的后端服务，叫 AuralTest（听力测试），面向学校场景。学生通过微信小程序进行听力练习，系统会基于 IRT（项目反应理论）算法评估学生的能力水平，并推荐匹配难度的教材。管理员可以上传教材、管理班级和查看学生数据。

技术栈是 Spring Boot 2.5.5 + Spring Security + JWT + MyBatis + MySQL，部署在微信云托管上，用 Docker 容器化运行。

### Q2：为什么选择 Spring Boot 而不是其他框架？

**答：**
- 生态成熟、社区资源丰富，开发效率高
- 内置自动配置，减少样板代码
- 与 Spring Security 无缝集成，方便做认证鉴权
- 微信云托管对 Java/Spring Boot 支持良好
- 团队技术栈统一，降低协作成本

### Q3：你的项目分了哪些层？每层的职责是什么？

**答：** 典型的三层架构加一些辅助层：

- **Controller 层**（7 个）：接收请求、参数校验、调用 Service、返回统一响应
- **Service 层**（8 接口 + 8 实现）：核心业务逻辑，比如练习提交、能力评估、推荐算法
- **DAO 层**（13 个 Mapper）：数据持久化，通过 MyBatis 与 MySQL 交互
- **Model 层**（13 个实体）：数据库表的映射
- **DTO 层**（27 个）：数据传输对象，解耦前端请求/响应与内部模型
- **Config 层**（7 个）：安全配置、JWT 配置、全局异常处理等
- **Security 层**：JWT 过滤器和工具类

---

## 二、安全与认证（高频考点）

### Q4：你的 JWT 认证流程是怎样的？

**答：**

1. **学生登录**：微信前端获取临时 code → 后端调用微信接口换取 OpenID → 查找是否已绑定学号
   - 已绑定：直接签发 JWT Token（有效期 7 天）
   - 未绑定：返回提示，引导调用 `/api/auth/bind` 绑定学号和姓名
2. **管理员登录**：通过 `/api/auth/adminLogin`，用账号密码登录，验证通过后签发 JWT
3. **后续请求**：客户端在请求头携带 `Authorization: Bearer <token>`，后端的 JWT 过滤器拦截请求，解析验证 Token，将用户信息注入 SecurityContext

### Q5：JWT Token 过期了怎么办？有没有考虑续期机制？

**答：** 当前设计是 7 天有效期（604800 秒）。过期后客户端需要重新走微信登录流程获取新 Token。

如果要优化，可以考虑：
- **双 Token 方案**：短期 Access Token（如 2 小时）+ 长期 Refresh Token（如 7 天），Access Token 过期时用 Refresh Token 换新的
- **滑动过期**：每次请求时检查 Token 剩余有效期，如果快过期就自动续签
- 当前场景是微信小程序，重新登录成本低（静默登录），所以单 Token 方案也够用

### Q6：JWT 的 secret 硬编码在配置文件里，有什么安全隐患？如何改进？

**答：** 配置文件中的 secret 如果泄露，攻击者可以伪造任意用户的 Token。改进方案：
- 使用环境变量注入（微信云托管支持环境变量配置）
- 使用密钥管理服务（KMS）
- 定期轮换密钥
- 使用 RSA 非对称加密替代 HMAC 对称加密

### Q7：Spring Security 中你是如何配置接口权限的？

**答：** 在 `SecurityConfig` 中：
- 登录、绑定等公开接口放行（`permitAll`）
- 学生相关接口需要学生角色
- 管理员接口需要管理员角色
- 其他接口默认需要认证
- 配置了自定义的 JWT 过滤器，在 `UsernamePasswordAuthenticationFilter` 之前执行

---

## 三、数据库与 MyBatis

### Q8：为什么选择 MyBatis 而不是 JPA/Hibernate？

**答：**
- MyBatis 对 SQL 的控制更灵活，适合复杂查询场景
- 学习成本低，XML 映射直观
- 对于这个项目的查询需求（排行榜、统计、条件筛选），手写 SQL 比 HQL/JPQL 更好优化
- 团队对 MyBatis 更熟悉

### Q9：你的数据库有哪些核心表？它们之间的关系是什么？

**答：** 核心表和关系：
- **Student**：学生表，含 theta 能力值
- **AdminUser**：管理员表
- **Classes**：班级表
- **StudentRoster**：学生与班级的多对多关联表
- **Material**：教材表，含难度等级
- **Question**：题目表，属于某个 Material（多对一）
- **QuestionOption**：选项表，属于某个 Question（多对一）
- **Attempt**：练习记录表，关联 Student 和 Material
- **AttemptAnswer**：每道题的作答记录，关联 Attempt 和 Question
- **AudioAsset**：音频资源表
- **StudentAbilityState**：学生能力快照表，记录每次练习后的能力变化

### Q10：如何防止 SQL 注入？

**答：**
- MyBatis 中使用 `#{}` 占位符（PreparedStatement），而不是 `${}` 字符串拼接
- Controller 层做参数校验
- Spring Security 过滤非法请求

---

## 四、核心业务逻辑（重点！）

### Q11：IRT 能力评估算法是什么？你是怎么实现的？

**答：** IRT（Item Response Theory，项目反应理论）是心理测量学中的经典模型，用于根据学生答题表现估算其潜在能力值（theta）。

核心思路：
- 每道题有一个**难度参数**
- 学生有一个**能力值 theta**
- 答对难题说明能力高，答错简单题说明能力低
- 使用数学模型（通常是 Logistic 函数）计算答对概率：`P(correct) = 1 / (1 + e^(-(theta - difficulty)))`
- 根据实际作答结果，用最大似然估计或贝叶斯方法更新 theta

实现在 `AbilityEstimator.java` 中，每次学生提交练习后调用，计算新的 theta 并记录 `thetaBefore` 和 `thetaAfter`，用于追踪成长轨迹。

### Q12：推荐教材的逻辑是什么？

**答：** `GET /practice/get-recommend` 根据学生当前的 theta 值，匹配难度等级接近的教材。思路是：
- 教材有难度等级标签
- 学生 theta 映射到对应的难度区间
- 推荐略高于当前能力的教材（"最近发展区"理念），既有挑战性又不会太难
- 排除已完成的教材

### Q13：学生提交练习的完整流程是什么？

**答：**
1. 前端调用 `POST /practice/submit`，携带 materialId 和所有答案
2. Controller 接收请求，校验参数，调用 Service
3. Service 层：
   - 创建 Attempt 记录
   - 逐题比对答案，记录 AttemptAnswer（对/错）
   - 调用 `AbilityEstimator` 计算新 theta
   - 更新 Student 表的 theta 字段
   - 生成 StudentAbilityState 快照
4. 返回练习结果（得分、能力变化等）

---

## 五、API 设计

### Q14：你的 API 响应格式是怎么设计的？

**答：** 使用统一响应包装类 `ApiResponse`，结构大致如下：
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```
异常情况通过 `GlobalExceptionHandler` 统一捕获，返回对应的错误码和消息，保证前端拿到的格式一致。

### Q15：为什么用 DTO 层？直接返回 Model 不行吗？

**答：** 用 DTO 的好处：
- **安全**：避免暴露敏感字段（比如密码、内部 ID）
- **解耦**：前端需求和数据库结构可以独立变化
- **灵活**：不同接口可以返回同一实体的不同视图
- **校验**：请求 DTO 上可以加参数校验注解
- 项目中有 27 个 DTO，覆盖了 auth、admin、practice、quest 等模块

---

## 六、部署与运维

### Q16：微信云托管的部署流程是怎样的？

**答：**
- 项目根目录有 `Dockerfile` 和 `container.config.json`
- Dockerfile 定义了 Java 17 运行环境，打包 Spring Boot 的 jar
- 数据库连接信息通过环境变量注入（MYSQL_ADDRESS 等），由云托管平台管理
- 推送代码到微信云托管后自动构建部署
- 容器监听 80 端口

### Q17：你在项目中遇到过什么困难？怎么解决的？

**答（参考方向，请结合实际经历调整）：**
- IRT 算法的参数调优：初始 theta 和步长设定需要实验验证
- 微信登录的静默授权和手动授权的兼容处理
- JWT 过滤器与 Spring Security 的集成调试
- 并发提交练习时的数据一致性问题

---

## 七、扩展与优化（加分项）

### Q18：如果用户量增长，系统有哪些瓶颈？你会怎么优化？

**答：**
- **数据库**：加索引（theta、studentId、materialId 等高频查询字段）；读写分离；分表
- **缓存**：热点数据（推荐教材列表、排行榜）可以引入 Redis
- **API**：排行榜等重计算接口可以做定时任务预计算，而不是实时查询
- **并发**：练习提交可以加分布式锁或乐观锁，避免重复提交

### Q19：你有没有考虑过接口幂等性？

**答：** 比如练习提交接口 `POST /practice/submit`，如果网络抖动导致重复提交：
- 可以在前端加防重复点击
- 后端可以基于 studentId + materialId + 时间窗口做去重
- 或者引入唯一请求 ID（幂等键），重复请求直接返回上次结果

### Q20：如何保证接口的安全性？

**答：**
- JWT 认证 + 角色鉴权
- 接口参数校验（防止非法输入）
- 全局异常处理（不暴露堆栈信息）
- SQL 注入防护（MyBatis 参数化查询）
- HTTPS 传输加密（微信云托管默认支持）
- Rate Limiting（可通过 Nginx 或中间件实现，当前未做，可以作为优化方向）

---

## 八、Java / Spring 基础知识（常配合项目问）

### Q21：Spring Boot 的自动配置原理是什么？

**答：** 基于 `@EnableAutoConfiguration` 注解，Spring Boot 在启动时扫描 `META-INF/spring.factories` 中注册的自动配置类，根据 `@Conditional` 系列注解判断是否满足条件（比如 classpath 中有某个类、某个 Bean 不存在等），满足则自动注册对应的 Bean。

### Q22：`@Component`、`@Service`、`@Repository`、`@Controller` 的区别？

**答：** 本质都是 `@Component` 的语义化别名，功能上基本相同，但语义不同：
- `@Controller`：标识控制层
- `@Service`：标识业务层
- `@Repository`：标识持久层，额外有数据访问异常转换功能
- `@Component`：通用组件

### Q23：Spring 的 IoC 和 DI 是什么？

**答：**
- **IoC（控制反转）**：对象的创建和管理交给 Spring 容器，而不是手动 new
- **DI（依赖注入）**：容器自动将依赖的 Bean 注入到需要它的地方
- 项目中通过 `@Autowired` 或构造器注入的方式，让 Controller 依赖 Service，Service 依赖 DAO

---

> 准备面试时，建议对照项目代码，能够随时跳到具体实现讲解细节。面试官喜欢追问"为什么这么设计"和"如果改进你会怎么做"。
