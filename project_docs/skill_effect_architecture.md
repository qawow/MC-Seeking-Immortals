# 技能效果系统架构设计

## 核心设计思路

技能效果系统采用**策略模式 + 组合模式**：
- 每种技能类型对应一个具体效果实现
- 效果通过统一接口执行
- 支持技能等级和熟练度影响效果强度

---

## 1. 核心接口

### SkillEffect（技能效果接口）
```java
public interface SkillEffect {
    // 执行技能效果
    boolean execute(ServerPlayer player, PlayerCultivation cultivation, CultivationSkill skill, SkillContext context);
    
    // 获取灵力消耗
    int getSpiritualPowerCost(int skillLevel);
    
    // 获取神识消耗（可选）
    default int getDivineConsciousnessCost(int skillLevel) { return 0; }
    
    // 获取冷却时间（tick）
    int getCooldownTicks(int skillLevel);
    
    // 是否可以执行（额外条件检查）
    default boolean canExecute(ServerPlayer player, PlayerCultivation cultivation) { return true; }
}
```

### SkillContext（技能上下文）
```java
public class SkillContext {
    private final Level level;
    private final Vec3 position;
    private final Vec3 lookDirection;
    private final Entity targetEntity;
    private final BlockPos targetBlock;
    
    // 构建器模式
    public static Builder builder() { return new Builder(); }
}
```

---

## 2. 效果分类实现

### A. 功法效果（被动）
```
CultivationMethodEffect（抽象基类）
  ├─ PassiveCultivationBoost    // 被动修炼加速
  ├─ BreakthroughBonus           // 突破成功率加成
  └─ AttributeEnhancement        // 属性增强
```

**实现方式：**
- 功法效果在修炼时自动计算加成
- 通过 `PlayerCultivation.getCultivationSpeedMultiplier()` 查询已装备功法
- 不需要主动释放

### B. 法术效果（主动）
```
SpellEffect（抽象基类）
  ├─ ProjectileSpell     // 投射物法术（火球、冰锥、金刃）
  ├─ AreaSpell           // 区域法术（藤蔓、土墙、剑阵）
  ├─ BuffSpell           // 增益法术（轻身术）
  ├─ UtilitySpell        // 工具法术（隐身、遁术、探测）
  └─ SummonSpell         // 召唤法术（剑阵、傀儡）
```

**实现方式：**
- 玩家按键触发
- 检查灵力/神识/冷却
- 创建实体/粒子效果
- 计算伤害（基于技能等级、灵根亲和、战斗属性）

### C. 生活技能效果（触发式）
```
CraftingSkillEffect（抽象基类）
  ├─ AlchemyEffect       // 炼丹成功率/品质加成
  ├─ RefiningEffect      // 炼器品质加成
  ├─ FormationEffect     // 阵法效率加成
  └─ TalismanEffect      // 符箓成功率加成
```

**实现方式：**
- 在对应操作时检查技能等级
- 应用成功率和品质加成
- 不消耗灵力，消耗材料

### D. 特殊技能效果（混合）
```
SpecialSkillEffect（抽象基类）
  ├─ BeastTamingEffect   // 驭兽成功率
  ├─ PuppetControlEffect // 傀儡操控上限和效率
  └─ MultiCastingEffect  // 同时施法数量
```

---

## 3. 效果注册表

### SkillEffectRegistry
```java
public class SkillEffectRegistry {
    private static final Map<SkillType, SkillEffect> EFFECTS = new HashMap<>();
    
    static {
        // 功法
        register(SkillType.CHANGCHUN_METHOD, new PassiveCultivationBoost(1.2));
        register(SkillType.DAYAN_METHOD, new PassiveCultivationBoost(1.5));
        
        // 法术
        register(SkillType.FIREBALL, new FireballSpell());
        register(SkillType.ICE_CONE, new IceConeSpell());
        
        // 生活技能
        register(SkillType.ALCHEMY, new AlchemyEffect());
        
        // 特殊技能
        register(SkillType.BEAST_TAMING, new BeastTamingEffect());
    }
    
    public static SkillEffect get(SkillType type) {
        return EFFECTS.get(type);
    }
}
```

---

## 4. 技能执行流程

```
用户触发技能
    ↓
检查技能是否解锁
    ↓
检查境界、灵根是否满足
    ↓
检查灵力/神识是否足够
    ↓
检查冷却时间
    ↓
获取 SkillEffect 实例
    ↓
构建 SkillContext（目标、位置等）
    ↓
执行 effect.execute()
    ↓
消耗灵力/神识
    ↓
设置冷却时间
    ↓
增加技能经验和熟练度
```

---

## 5. 伤害计算公式（法术）

```java
基础伤害 = 技能基础伤害 × (1 + 技能等级 × 0.15) × (1 + 熟练度/10000)
灵根加成 = TechniqueAffinityCalculator.calculate(...)
最终伤害 = 基础伤害 × 灵根加成 × 法术攻击力系数
```

---

## 6. 文件结构

```
src/main/java/com/xunxian/seekingimmortals/skill/
├── SkillCategory.java              ✓ 已完成
├── SkillType.java                  ✓ 已完成
├── CultivationSkill.java           ✓ 已完成
├── effect/
│   ├── SkillEffect.java            待创建
│   ├── SkillContext.java           待创建
│   ├── SkillEffectRegistry.java    待创建
│   ├── cultivation/
│   │   ├── CultivationMethodEffect.java
│   │   ├── PassiveCultivationBoost.java
│   │   └── BreakthroughBonus.java
│   ├── spell/
│   │   ├── SpellEffect.java
│   │   ├── ProjectileSpell.java
│   │   ├── FireballSpell.java
│   │   └── IceConeSpell.java
│   ├── crafting/
│   │   ├── CraftingSkillEffect.java
│   │   └── AlchemyEffect.java
│   └── special/
│       ├── SpecialSkillEffect.java
│       └── BeastTamingEffect.java
└── executor/
    └── SkillExecutor.java          技能执行工具类
```

---

## 7. 优先级实现顺序

1. **Phase 1（核心框架）**
   - SkillEffect 接口
   - SkillContext 上下文
   - SkillEffectRegistry 注册表

2. **Phase 2（功法效果）**
   - PassiveCultivationBoost（被动加速）
   - 集成到 PlayerCultivation 修炼速度计算

3. **Phase 3（基础法术）**
   - FireballSpell（火球术）
   - 作为模板，其他法术复制修改

4. **Phase 4（扩展）**
   - 其他法术效果
   - 生活技能效果
   - 特殊技能效果

---

## 8. 关键技术点

- **网络同步**：法术释放需要客户端→服务端网络包
- **实体创建**：投射物法术需要自定义实体（继承 Projectile）
- **粒子效果**：客户端粒子渲染
- **音效**：法术释放音效
- **冷却显示**：UI 显示冷却进度

---

## 设计优势

1. **可扩展**：新增技能只需实现 SkillEffect 接口
2. **模块化**：效果独立，互不干扰
3. **可配置**：技能参数通过构造函数传入
4. **可测试**：接口清晰，易于单元测试
