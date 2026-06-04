# VoIPCalc-Core

跨境 VoIP 话务系统通话费率计算引擎 —— 基于 DDD（领域驱动设计）与 TDD（测试驱动开发）构建的核心领域库。

---

## 项目概述

VoIPCalc-Core 是一个**纯计算内核**，无 IO、无数据库、无网络依赖。职责高度单一：

> 输入通话上下文（主叫、被叫、通话时间），输出最终每分钟单价。

三条业务规则按固定顺序叠加（规则责任链）：

```
CallContext → [基础费率] → [客户折扣] → [夜间减免] → FinalUnitPrice
```

| 规则 | 描述 |
|------|------|
| **基础费率** | 中国(+86) ¥0.10 · 美国(+1) ¥0.05 · 其他 ¥0.50 |
| **客户折扣** | VIP 0.9 折 · NORMAL 无折扣 |
| **夜间减免** | 23:00~05:00 减免 ¥0.02，不低于 0 |

---

## 技术栈

- Java 17 · JUnit 5 · AssertJ · Maven · BigDecimal 精确金额

---

## 领域模型

### 整体架构（DDD 四层中的领域层）

```
CallContext → RateCalculator（领域服务）→ FinalUnitPrice
                │
            ┌───┴───────────────┐
            │   规则链（纯函数）   │
            │  BaseRate          │
            │  → DiscountRate    │
            │  → NightReduction  │
            └───────────────────┘
```

### 核心类型一览

| 类型 | 类别 | 行数 | 职责 |
|------|------|------|------|
| `CountryCode` | 枚举 | 35 | 国家代码 + 前缀 + 基础费率 + 格式校验 |
| `CalledNumber` | record | 14 | 被叫号码，含 null 防护 |
| `BaseRate` | record | 7 | 委托 CountryCode 获取基础费率 |
| `CustomerType` | 枚举 | 16 | VIP(0.9) / NORMAL(1.0) |
| `DiscountRate` | record | 8 | 委托 CustomerType 执行折扣计算 |
| `CallTime` | record | 20 | 通话时间 + 夜间判断 + null 防护 |
| `NightReduction` | record | 12 | 减免 0.02 + BigDecimal.max() 保底 |
| `CallContext` | record | 5 | 聚合输入：号码 + 身份 + 时间 |
| `FinalUnitPrice` | record | 12 | 不可变最终单价，≥0 不变量 |
| `RateCalculator` | 领域服务（无状态） | 20 | 规则链编排 |

**总计：10 个文件，154 行有效代码。**

---

## TDD 开发历程

整个项目严格遵循 **Red → Green → Refactor** 循环，分 5 个子需求迭代：

| # | 子需求 | 测试类 | 用例数 |
|---|--------|--------|--------|
| 1 | 基础费率解析 | `BaseRateResolutionTest` | 10 |
| 2 | 客户身份折扣 | `DiscountRateTest` | 5 |
| 3 | 夜间低谷福利 | `NightDiscountTest` | 10 |
| 4 | 完整叠加逻辑 | `RateCalculatorIntegrationTest` | 8 |
| 5 | 健壮性与边界 | `RobustnessBorderTest` | 9 |
| **合计** | | **5 个测试类** | **42** |

所有测试均在 Red 阶段**先写测试、目睹失败**，再在 Green 阶段编写**刚好足够**的代码使其通过，最后在 Refactor 阶段**消除重复、改善设计**。

---

## 代码洁癖细节

以下是在本项目交付过程中刻意追求的工程整洁实践：

### 1. 值对象不可变（Immutable Value Objects）

全部数据载体使用 Java `record` 或 `enum`，天然不可变，杜绝 setter、无副作用。

```java
public record CalledNumber(String rawNumber) { }   // 无 setter，不可篡改
public record FinalUnitPrice(BigDecimal value) { }  // 值不可变
```

### 2. 紧凑构造器校验（Compact Constructor Validation）

校验放在 record 的 compact constructor 中，**构造即校验，非法对象不可诞生**：

```java
public record CallTime(LocalDateTime timestamp) {
    public CallTime {
        if (timestamp == null) {
            throw new IllegalArgumentException("timestamp must not be null");
        }
    }
}
```

### 3. Tell, Don't Ask — 数据与行为内聚

`CountryCode` 枚举**同时持有前缀和基础费率**，不将自身知识泄露给调用方：

```java
public enum CountryCode {
    CHINA("+86", BigDecimal.valueOf(0.10)),
    USA("+1", BigDecimal.valueOf(0.05)),
    OTHER("", BigDecimal.valueOf(0.50));

    public BigDecimal baseRate() { return baseRate; }
    public static CountryCode fromPrefix(String raw) { /* ... */ }
}
```

`BaseRate` 和 `CalledNumber` 仅做纯委托，不重复持有费率或前缀。

### 4. 声明式 > 命令式

`NightReduction.apply()` 从 5 行 if-clamp 缩减为 1 行表达：

```java
// Before (命令式)
BigDecimal reduced = price.subtract(REDUCTION);
if (reduced.compareTo(BigDecimal.ZERO) < 0) return BigDecimal.ZERO;
return reduced;

// After (声明式)
return price.subtract(REDUCTION).max(BigDecimal.ZERO);
```

### 5. 无局部变量污染

`RateCalculator.calculate()` 避免不必要的中间变量，3 条规则清晰可读：

```java
BigDecimal price = new BaseRate(context.calledNumber().countryCode()).pricePerMinute();
price = new DiscountRate(context.customerType()).applyTo(price);
if (context.callTime().isNightPeriod()) {
    price = new NightReduction().apply(price);
}
return new FinalUnitPrice(price);
```

### 6. BigDecimal 精确金额

全场使用 `BigDecimal` 而非 `float/double`，折扣系数也使用 `BigDecimal` 存储以避免 `double × BigDecimal` 的隐式精度损失。

### 7. 魔法数字提取为命名常量

```java
private static final int NIGHT_START_HOUR = 23;
private static final int NIGHT_END_HOUR = 5;
```

### 8. 异常消息包含上下文

每个 `IllegalArgumentException` 的消息都提供诊断信息：

```java
throw new IllegalArgumentException("rawNumber must start with '+', got: " + rawNumber);
throw new IllegalArgumentException("timestamp must not be null");
throw new IllegalArgumentException("单价不能为负");
```

### 9. 空字符串降级策略

空号码不抛异常，降级为 `OTHER`，符合业务默认行为，避免过度防御：

```java
// "" → CountryCode.OTHER（降级，不阻断流程）
// "abc" → IllegalArgumentException（明确非法）
```

### 10. YAGNI — 不引入未需要的抽象

仅 3 条规则时，不使用策略模式或责任链框架。`RateCalculator` 中直接顺序编排，有新增规则时再考虑重构。**先用最简单的方式满足需求，测试保护下重构。**

---

## 如何主导 AI 完成高质量交付

本项目通过与 AI 的协作完成交付，以下是协作策略的核心要点：

### 1. 设计先行，代码后行

交付的首个指令是「先编写 DDD 分析设计文档，不用进行实现」。这迫使 AI 在写代码之前建立完整的领域心智模型（通用语言、限界上下文、值对象、领域服务），后续所有代码实现都对齐这份设计文档。

**产物**：[`DESIGN.md`](./DESIGN.md) — 涵盖战略设计、战术设计、架构分层、测试策略、关键决策。

### 2. TDD 五子需求拆分

将整体需求拆分为 5 个原子子需求，逐个子需求执行 Red-Green-Refactor：

```
子需求 1 → 子需求 2 → 子需求 3 → 子需求 4 → 子需求 5
   │          │          │          │          │
   R-G-R     R-G-R     R-G-R     R-G-R     R-G-R
```

每个子需求**独立完成三轮循环**后才进入下一子需求，确保每个增量都有测试保护，不会出现大爆炸式集成。

### 3. Red 阶段严格执行

每个子需求的 Red 阶段指令都是「不实现任何代码，预期结果失败」或「编写刚好足够的代码让编译通过」。AI 必须先运行 `mvnw test` 看到 **BUILD FAILURE**，才被允许进入 Green 阶段。

### 4. Green 阶段最小化

Green 阶段指令明确：「编写**刚好足够**的代码使测试通过，不考虑任何额外的复杂度」。这阻止了 AI 提前引入抽象、过度工程化。

### 5. Refactor 阶段审查而非盲改

Refactor 阶段 AI 必须先审查现有代码，确认**是否真的有重复或可改善的设计**，而非机械地「总要改点什么」。本项目 5 个子需求中有 2 个（子需求 2、子需求 5）审查后结论为「无需重构」，避免了无意义的代码扰动。

### 6. 全量回归每次必跑

每个子需求的 Green 和 Refactor 阶段结束后，都运行 `mvnw test`（全量）而非单独测试类。确保增量修改不破坏已有功能。最终交付时 42 个测试全绿。

### 7. 对话留痕

每次回答末尾自动追加留痕区块到 [`PROMPTs.md`](./PROMPTs.md)（共 18 个条目），完整记录每次交互的类型、输入、输出和关键决策，形成可追溯的开发日志。

---

## 项目结构

```
VoIPCalc-Core/
├── DESIGN.md                        # DDD 分析与设计文档
├── PROMPTs.md                       # 对话留痕记录（18 条）
├── pom.xml                          # Maven 配置（JUnit 5 + AssertJ）
└── src/
    ├── main/java/com/voipcalc/core/domain/
    │   ├── CountryCode.java         # 枚举：国家代码 + 前缀 + 费率
    │   ├── CalledNumber.java        # record：被叫号码
    │   ├── BaseRate.java            # record：基础费率
    │   ├── CustomerType.java        # 枚举：客户身份
    │   ├── DiscountRate.java        # record：折扣率
    │   ├── CallTime.java            # record：通话时间
    │   ├── NightReduction.java      # record：夜间减免
    │   ├── CallContext.java         # record：通话上下文
    │   ├── FinalUnitPrice.java      # record：最终单价
    │   └── RateCalculator.java      # 领域服务：规则链编排
    └── test/java/com/voipcalc/core/domain/
        ├── BaseRateResolutionTest.java          # 子需求 1：10 用例
        ├── DiscountRateTest.java                # 子需求 2：5 用例
        ├── NightDiscountTest.java               # 子需求 3：10 用例
        ├── RateCalculatorIntegrationTest.java   # 子需求 4：8 用例
        └── RobustnessBorderTest.java            # 子需求 5：9 用例
```

---

## 运行测试

```bash
./mvnw test
```

预期输出：**Tests run: 42, Failures: 0, Errors: 0, Skipped: 0 — BUILD SUCCESS**
