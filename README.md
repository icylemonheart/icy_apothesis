# Icy Apotheosis

一个神化（Apotheosis）模组附属，通过配置文件指定实体固定成为特定等级的神化 BOSS。

同时支持 **Forge 1.20.1** 和 **NeoForge 1.21.1** 两个版本。

## ✨ 功能

- 按稀有度配置实体，支持权重随机分配等级
- 9 个等级：普通 → 非凡 → 珍稀 → 史诗 → 神话 → 源起 → **归墟** → **冰烬之终焉**
- 自定义稀有度（ordinal 6/7），独立颜色、材质、名称效果
- 自动 BOSS 装备神化，含保底词缀与宝石槽
- 三重保险的创造/旁观模式索敌屏蔽
- 游戏内命令 `/icyapotheosis reload|list|help`
- 跨模组兼容（L2Hostility / KubeJS / Fallen-Gems-Affixes）

## 📦 仓库结构

```
icy_apotheosis/
├── forge-1.20.1/          # Minecraft 1.20.1 · Forge · Java 17
│   ├── src/main/java/com/icyapotheosis/
│   ├── src/main/resources/
│   ├── data/apotheosis/rarities/
│   ├── build.gradle
│   ├── gradle.properties
│   ├── settings.gradle
│   ├── gradlew / gradlew.bat
│   └── README.md
│
└── neoforge-1.21.1/       # Minecraft 1.21.1 · NeoForge · Java 21
    ├── src/main/java/com/icyapotheosis/
    ├── src/main/resources/
    ├── data/apotheosis/rarities/
    ├── build.gradle
    ├── gradle.properties
    ├── settings.gradle
    └── gradlew / gradlew.bat
```

## 🚀 使用

1. 选择对应的版本目录（Forge 或 NeoForge）
2. 用 Gradle 构建：
   ```bash
   ./gradlew build        # Linux/macOS
   gradlew.bat build      # Windows
   ```
3. 将 `build/libs/icy_apotheosis-*.jar` 放入 Minecraft `mods/` 目录
4. 启动游戏后编辑 `config/icy_apotheosis-common.toml` 配置实体等级

## ⚙️ 配置示例

```toml
# config/icy_apotheosis-common.toml

[icy_apotheosis]
# Epic 等级：监守者和凋灵
epicEntities = ["minecraft:warden", "minecraft:wither"]

# Mythic 等级：末影龙
mythicEntities = ["minecraft:ender_dragon"]

# 多个等级可选（权重随机）
guixuEntities = ["minecraft:warden|7"]
icycinderEndEntities = []

# 开关
announceBoss = true
bossAutoAggro = true
```

## 🏆 等级规则

| 等级 | Ordinal | 颜色 | 装备词缀 |
|------|---------|------|----------|
| 普通 Common | 0 | - | 标准 |
| 非凡 Uncommon | 1 | - | 速度+防火 |
| 珍稀 Rare | 2 | - | 多 20% |
| 史诗 Epic | 3 | - | 多 40% |
| 神话 Mythic | 4 | - | 多 60% |
| 源起 Ancient | 5 | - | 多 80% |
| **归墟 Guixu** | 6 | 🔴 大红 | 保底 6 STAT / 4 ABILITY / 5 宝石 |
| **冰烬之终焉 Icycinder End** | 7 | 🔵 冰蓝闪烁 | 保底 7 STAT / 4 ABILITY / 5 宝石 |

## 🛠️ 命令

游戏内 OP 权限（Level 2）可用：

```
/icyapotheosis reload   # 重新加载配置文件
/icyapotheosis list    # 显示已配置实体列表
/icyapotheosis help    # 显示帮助
```

## 📄 License

本项目仅供学习交流使用。
