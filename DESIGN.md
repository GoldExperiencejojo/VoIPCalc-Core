# VoIPCalc-Core DDD 分析与设计文档

---

## 1. 业务领域概览

### 1.1 领域愿景

VoIPCalc-Core 是跨境 VoIP 话务系统的核心费率计算引擎。其职责高度单一：接收一次通话的上下文信息（主叫、被叫、通话时间），依据目的地国家、客户身份与通话时段，计算出本次通话的最终每分钟单价。

### 1.2 核心业务流程

```
CallContext → [基础费率] → [客户折扣] → [夜间减免] → FinalUnitPrice
```

三条规则按固定顺序叠加，不可颠倒。每一条规则的输入依赖前一条规则的输出，形成一条**规则责任链**。

---

## 2. 战略设计

### 2.1 限界上下文（Bounded Context）

- **计费上下文（Billing Context）**：VoIPCalc-Core 所属的核心上下文，负责通话费率计算。与 CRM（客户身份）、路由（被叫号码解析）上下文通过 ACL（防腐层）解耦。

### 2.2 通用语言（Ubiquitous Language）

| 术语 | 英文 | 定义 |
|------|------|------|
| 被叫号码 | CalledNumber | 值的国际号码，可从中解析国家代码 |
| 客户身份 | CustomerType | 主叫用户的身份类型（VIP / NORMAL） |
| 通话时间 | CallTime | 通话发起时刻，含时区无关的时间戳 |
| 基础费率 | BaseRate | 由被叫国家代码确定的原始单价（元/分钟） |
| 折扣率 | DiscountRate | 根据客户身份给出的折扣系数 |
| 夜间减免 | NightReduction | 固定 0.02 元/分钟的夜间时段减免 |
| 最终单价 | FinalUnitPrice | 经所有规则计算后的最终每分钟单价 |
| 通话上下文 | CallContext | 持有被叫号码、客户身份、通话时间的输入载体 |
| 费率计算器 | RateCalculator | 无状态领域服务，编排规则链 |

---

## 3. 战术设计

### 3.1 领域模型总览

```
┌─────────────────────────────────────────────────┐
│                  CallContext                     │
│  - calledNumber: CalledNumber                   │
│  - customerType: CustomerType                   │
│  - callTime: CallTime                           │
└──────────────────────┬──────────────────────────┘
                       │ 输入
                       ▼
┌─────────────────────────────────────────────────┐
│                RateCalculator                   │  ◄── 领域服务
│  + calculate(CallContext): FinalUnitPrice       │
│                                                 │
│  规则链:                                        │
│    BaseRate ←→ DiscountRate ←→ NightReduction   │
└──────────────────────┬──────────────────────────┘
                       │ 输出
                       ▼
┌─────────────────────────────────────────────────┐
│              FinalUnitPrice                     │
│  - value: BigDecimal (元/分钟)                  │
└─────────────────────────────────────────────────┘
```

### 3.2 值对象（Value Objects）

#### 3.2.1 CalledNumber（被叫号码）

```java
// 值对象：不可变、自校验、富含行为
public record CalledNumber(String rawNumber) {
    // 解析国家代码
    // +86 → CountryCode.CHINA
    // +1  → CountryCode.USA
    // 其他 → CountryCode.OTHER
    public CountryCode countryCode();
}
```

| 属性 | 类型 | 说明 |
|------|------|------|
| rawNumber | String | 原始被叫号码，如 "+8613800138000" |
| countryCode | CountryCode | 解析后的国家代码枚举 |

**不变量**：rawNumber 必须以 "+" 开头，后跟数字。

---

#### 3.2.2 CustomerType（客户身份）

```java
// 枚举值对象
public enum CustomerType {
    VIP(0.9),      // 海外留学生/华人卡，享受 9 折
    NORMAL(1.0);   // 普通用户，无折扣

    private final double discountFactor;
}
```

| 枚举值 | 折扣系数 | 说明 |
|--------|----------|------|
| VIP | 0.9 | 海外留学生/华人卡 |
| NORMAL | 1.0 | 普通用户，无折扣 |

---

#### 3.2.3 CallTime（通话时间）

```java
// 值对象：封装时间戳与夜间判断
public record CallTime(LocalDateTime timestamp) {
    // 判断是否处于夜间低谷时段（23:00 – 次日 05:00）
    public boolean isNightPeriod() {
        int hour = timestamp.getHour();
        return hour >= 23 || hour < 5;
    }
}
```

| 属性 | 类型 | 说明 |
|------|------|------|
| timestamp | LocalDateTime | 通话发起时间（服务端系统时间） |
| isNightPeriod() | boolean | 是否 23:00~05:00 |

**不变量**：timestamp 不可为 null，不可为未来时间（可选的防御性校验）。

---

#### 3.2.4 BaseRate（基础费率）

```java
// 由 CountryCode → 单价 的纯映射
public record BaseRate(CountryCode countryCode) {
    public BigDecimal pricePerMinute() {
        return switch (countryCode) {
            case CHINA  -> BigDecimal.valueOf(0.10);
            case USA    -> BigDecimal.valueOf(0.05);
            case OTHER  -> BigDecimal.valueOf(0.50);
        };
    }
}
```

| 国家代码 | 单价（元/分钟） |
|----------|-----------------|
| CHINA (+86) | 0.10 |
| USA (+1) | 0.05 |
| OTHER | 0.50 |

---

#### 3.2.5 DiscountRate（折扣率）

```java
// 由 CustomerType → 折扣系数 的纯映射
public record DiscountRate(CustomerType customerType) {
    public double factor() {
        return customerType.getDiscountFactor();
    }
}
```

| 客户身份 | 折扣系数 |
|----------|----------|
| VIP | 0.9 |
| NORMAL | 1.0 |

---

#### 3.2.6 NightReduction（夜间减免）

```java
// 固定值对象：减免 0.02 元/分钟
public record NightReduction() {
    private static final BigDecimal REDUCTION = BigDecimal.valueOf(0.02);

    // 应用减免，保证不低于 0
    public BigDecimal apply(BigDecimal price) {
        BigDecimal reduced = price.subtract(REDUCTION);
        return reduced.compareTo(BigDecimal.ZERO) < 0 ? BigDecimal.ZERO : reduced;
    }
}
```

| 属性 | 值 | 说明 |
|------|-----|------|
| REDUCTION | 0.02 | 固定减免金额 |
| 下限 | 0.00 | 减免后不低于 0 |

---

#### 3.2.7 FinalUnitPrice（最终单价）

```java
// 值对象：不可变的最终结果，单位为 元/分钟
public record FinalUnitPrice(BigDecimal value) {
    public FinalUnitPrice {
        Objects.requireNonNull(value);
        if (value.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("单价不能为负");
        }
    }
}
```

**不变量**：value ≥ 0。

---

### 3.3 领域服务（Domain Service）

#### RateCalculator

```java
// 无状态领域服务，纯函数，无副作用
public class RateCalculator {
    public FinalUnitPrice calculate(CallContext context) {
        // Step 1: 基础费率
        BigDecimal price = BaseRate.of(context.calledNumber().countryCode());

        // Step 2: 客户折扣
        price = DiscountRate.of(context.customerType()).applyTo(price);

        // Step 3: 夜间减免（仅夜间时段生效）
        if (context.callTime().isNightPeriod()) {
            price = new NightReduction().apply(price);
        }

        return new FinalUnitPrice(price);
    }
}
```

**设计原则**：
- 无状态：不持有任何可变状态，线程安全
- 纯函数：相同输入始终产生相同输出
- 规则链：三步骤按固定顺序编排，新增规则只需在链中插入新步骤
- 可测试：每个值对象和规则都可独立进行单元测试

---

### 3.4 输入载体（Input DTO / Context Object）

```java
// 通话上下文，聚合输入参数
public record CallContext(
    CalledNumber calledNumber,   // 被叫号码
    CustomerType customerType,   // 客户身份
    CallTime callTime            // 通话时间
) {}
```

---

### 3.5 辅助类型：CountryCode

```java
public enum CountryCode {
    CHINA("+86"),
    USA("+1"),
    OTHER("");

    private final String prefix;

    public static CountryCode fromCalledNumber(String rawNumber) {
        if (rawNumber.startsWith("+86")) return CHINA;
        if (rawNumber.startsWith("+1"))  return USA;
        return OTHER;
    }
}
```

---

## 4. 架构分层映射

```
┌──────────────────────────────────────────────────┐
│  接口层 (API)                                     │
│  REST / gRPC Controller                          │
│      │                                           │
│      ▼                                           │
│  应用层 (Application)                             │
│  RateApplicationService                          │
│  (编排 DTO → 领域对象 转换)                        │
│      │                                           │
│      ▼                                           │
│  领域层 (Domain)                ◄── VoIPCalc-Core │
│  RateCalculator (领域服务)                        │
│  CalledNumber, CallTime, ... (值对象)            │
│  CustomerType, CountryCode (枚举)                │
│  FinalUnitPrice, BaseRate, ... (值对象)          │
│      │                                           │
│      ▼                                           │
│  基础设施层 (Infrastructure)                       │
│  (无外部依赖 — 本引擎为纯计算)                      │
└──────────────────────────────────────────────────┘
```

本引擎仅涵盖**领域层**，无 IO、无数据库、无网络调用。是一个纯粹的计算内核。

---

## 5. 测试策略

| 测试层级 | 测试对象 | 示例场景 |
|----------|----------|----------|
| 单元测试 | CalledNumber | "+86..."→CHINA, "+1..."→USA, "+44..."→OTHER |
| 单元测试 | CallTime.isNightPeriod() | 23:00→true, 04:59→true, 12:00→false |
| 单元测试 | NightReduction.apply() | 0.03→0.01, 0.01→0.00, 0.20→0.18 |
| 单元测试 | DiscountRate.applyTo() | VIP×0.9, NORMAL×1.0 |
| 集成测试 | RateCalculator.calculate() | 组合场景：中国+VIP+白天、美国+NORMAL+夜间 等 |
| 边界测试 | NightReduction | 减免后刚好 = 0 的场景 |

---

## 6. 关键设计决策

| 决策 | 理由 |
|------|------|
| 所有概念建模为值对象 | 不可变、无副作用、易于测试和推理 |
| RateCalculator 无状态 | 纯计算引擎，天然线程安全且符合函数式范式 |
| 规则链顺序固定 | 业务规则有优先级的偏序关系：先定基价→再打折→最后减固定值 |
| 使用 BigDecimal | 避免浮点精度问题，适合金额计算 |
| 夜间减免用条件判断而非通用规则框架 | 当前仅 3 条规则，过度抽象无益，遵循 YAGNI |
| 输入聚合为 CallContext | 减少方法参数个数，语义清晰，可扩展 |
