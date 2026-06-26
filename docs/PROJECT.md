# Material Preparer 项目文档 (v1.0.0)

## 目录

- [项目介绍](#项目介绍)
- [已实现功能](#已实现功能)
- [架构设计](#架构设计)
- [核心功能实现细节](#核心功能实现细节)
- [后续计划](#后续计划)
- [代码改进方向](#代码改进方向)

---

## 项目介绍

Material Preparer 是一个 Minecraft Fabric 模组，旨在帮助玩家自动收集和整理材料。玩家可以配置需要的物品列表，模组会自动探索附近的容器（箱子等），并根据列表收集所需的物品。

### 核心特性

- 可配置的物品列表（支持从 Litematica 原理图导入）
- 自动探索附近容器并缓存物品信息
- 智能物品收集（精确移动、忽略已有物品）
- 容器标记系统（黑名单、输出容器）
- 完整的快捷键支持
- 调试可视化

### 技术栈

- Minecraft 1.21.1
- Fabric Loader
- YACL (Yet Another Config Lib) v3 - 配置库
- Mod Menu - 模组菜单集成

---

## 已实现功能

### 1. 物品列表 CSV 序列化存储

物品列表独立存储在 CSV 文件中，与配置文件分离。

**文件位置**: `config/material_preparer/item_list.csv`

**CSV 格式**:
```csv
item_id,count
minecraft:stone,10
minecraft:cobblestone,64
```

**特性**:
- 保存时机：配置界面点击保存时，与其他配置项同步保存
- 加载时机：模组客户端初始化时
- 使用 `BuiltInRegistries.ITEM` 进行物品 ID 与 Item 对象的转换
- 完整的错误处理和日志记录

**相关文件**:
- `config/ItemListSerializer.java` - CSV 序列化工具类
- `config/ItemEntry.java` - 物品条目记录（record）
- `ModMenuIntegration.java` - 保存回调
- `MaterialPreparerClient.java` - 加载逻辑

---

### 2. 本地化键规范化与补全

所有翻译键采用 Minecraft 风格的点隔格式，支持英文和中文。

**键命名规范**:
```
gui.material_preparer.title                    # 界面标题
gui.material_preparer.category.*               # 分类
gui.material_preparer.category.*.tooltip       # 分类提示
gui.material_preparer.group.*                  # 分组
gui.material_preparer.option.*                 # 选项
gui.material_preparer.keybind.*                # 快捷键名称
message.material_preparer.*                    # 游戏内消息
```

**本地化文件**:
- `assets/material-preparer/lang/en_us.json` - 英文
- `assets/material-preparer/lang/zh_cn.json` - 中文

---

### 3. 容器点击速率限制

限制容器点击操作的速率，避免过快点击导致的问题。

**配置项**:
- `minClickInterval` (Integer, 默认 0) - 点击间隔（单位：tick）
- 0 = 不限制，立即执行

**实现机制**:
- 点击队列（`Queue<ContainerClickAction>`）
- tick 计数器（`tickCounter`）
- 每个 tick 检查是否达到间隔时间，达到则执行下一个点击
- FIFO 顺序执行

**相关文件**:
- `MaterialPreparerClient.java` - 点击队列和处理逻辑
- `EventHandler.java` - tick 事件中处理队列

---

### 4. 精确物品移动 (Precise Move)

当 `alwaysQuickMove` 为 false 时，不全部使用 Shift+点击快速移动，而是使用精确的拾取和放置操作，只移动需要的数量。

**工作原理**:
1. 左键（PICKUP）拿起整个堆叠
2. 右键（PICKUP, k=1）放回多余的物品
3. 在玩家背包中找到相同物品的未满堆叠，左键放下
4. 如果还有剩余，继续找下一个
5. 如果都满了，找空格子放下

**配置项**:
- `alwaysQuickMove` (Boolean, 默认 true) - 是否始终快速移动

**特性**:
- 所有点击操作受 `minClickInterval` 速率限制
- 容器关闭前会等待所有点击执行完毕
- 可通过快捷键切换开关

**相关文件**:
- `MaterialPreparerClient.java` - `preciseMoveContainerSlot()`、`placeCursorItemIntoPlayerInventory()`

---

### 5. 忽略已有物品

开启后只收集玩家物品栏中缺少的物品。

**工作原理**:
- 收集开始前，遍历玩家物品栏（主背包+快捷栏共36格）
- 统计已有物品数量
- 从 `neededItems` 中减去已有数量
- 如果减去后数量 <= 0，则从需求列表中移除

**配置项**:
- `ignoreExistingItems` (Boolean, 默认 false)
- `ignoreExistingItemsKeybind` (String, 默认 "unbound")

**注意事项**:
- 只统计主背包和快捷栏（36格），不包括盔甲栏和副手
- 在收集开始时一次性计算，收集过程中不再重新计算
- 同时会排除输出容器中已缓存的物品

**相关文件**:
- `EventHandler.java` - `startItemCollection()` 中的计算逻辑

---

### 6. 容器标记系统

支持标记黑名单容器和输出容器。

#### 6.1 黑名单

- 将准星对准的方块添加到黑名单
- 渲染黑色调试边框
- 该位置的容器不会自动打开，收集物品时不会查看
- 再次执行可以移出黑名单
- 只在当前会话有效，断开连接后自动清空

#### 6.2 输出容器

- 将准星对准的方块设为输出容器
- 渲染红色调试边框
- 收集物品时不会从输出容器拿东西
- 当玩家物品栏已满时，将物品转移到输出容器
- 只在当前会话有效

#### 6.3 物品转移功能

当玩家背包满了，自动将物品列表中的物品转移到输出容器。

- 逐个尝试所有输出容器
- 优先尝试缓存中知道没满的容器
- 转移完后回到挂起的容器继续收集
- 所有输出容器都满了才停止收集

**配置项**:
- `toggleBlacklistKeybind` - 切换黑名单快捷键
- `toggleOutputContainerKeybind` - 切换输出容器快捷键
- `clearAllMarkersKeybind` - 清除所有标记快捷键

**调试边框颜色**:
- 黄色：已探索的普通容器
- 灰色：未探索的容器
- 黑色：黑名单容器
- 红色：输出容器

**相关文件**:
- `MaterialPreparerClient.java` - 数据结构和切换方法
- `EventHandler.java` - 渲染、探索/收集逻辑、断开清空

---

### 7. 消息本地化

所有游戏内消息和界面文本都已本地化。

**涵盖范围**:
- 游戏内操作反馈消息
- 错误提示消息
- 配置界面文本
- 自定义屏幕文本
- 状态提示消息

**未本地化的内容**:
- 快捷键名称显示（CustomKeybind）- 系统级显示
- 格式化占位符（如 `[%1$s]`）

**相关文件**:
- `EventHandler.java` - 游戏内消息
- `MaterialPreparerClient.java` - 操作反馈消息
- `EditItemScreen.java` - 编辑物品界面
- `LitematicaFileSelectorScreen.java` - 文件选择界面

---

### 8. 多输出容器逐个尝试

维护一个计划输出容器列表，逐个尝试，而不是只尝试最近的一个。

**工作原理**:
1. 收集开始时，收集范围内所有输出容器
2. 分为两类：chestMap 中没满的、其他的
3. 每类按距离排序
4. 优先添加没满的，再添加其他的
5. 背包满时，从列表中取下一个容器尝试
6. 东西放完了（背包不满了）就清空列表继续收集
7. 所有都试过了还是满的，中止操作

**相关文件**:
- `MaterialPreparerClient.java` - `scheduledOutputContainers`、`isContainerNotFull()`
- `EventHandler.java` - `tryNextOutputContainer()`、状态机逻辑

---

### 9. 逐个物品移动 + 挂起容器机制

每次移动完一个物品堆叠就检查背包，满了就挂起当前容器，先去输出容器放物品，放完再回来继续。

#### 新增状态

- `PROCESS_NEXT_ITEM` - 处理下一个物品
- `WAIT_ITEM_MOVE` - 等待物品移动完成

#### 工作流程

1. 打开容器，收到内容 → PROCESS_NEXT_ITEM
2. PROCESS_NEXT_ITEM：找到下一个需要的物品，移动它 → WAIT_ITEM_MOVE
3. WAIT_ITEM_MOVE：等待点击队列清空
   - 如果背包满了：挂起当前容器 → WAIT_CLOSE → 去输出容器转移
   - 如果背包没满：→ PROCESS_NEXT_ITEM，继续处理下一个
4. 所有物品处理完 → WAIT_CLOSE
5. 转移完输出容器后，如果有挂起的容器，重新打开继续

**相关文件**:
- `ExplorationPhase.java` - 新增状态
- `MaterialPreparerClient.java` - `suspendedContainerPos`
- `EventHandler.java` - 状态处理逻辑
- `MixinClientPacketListener.java` - 状态转换

---

### 10. 从 Litematica 导入物品列表

支持从 Litematica 原理图文件导入物品列表。

#### 10.1 Litematica 文件解析

- 文件格式：GZIP 压缩的 NBT 格式
- 解析所有区域的 BlockStatePalette 和 BlockStates
- 解包 BlockStates 数组（支持跨 long 边界）
- 统计每种方块的数量
- 使用 `Block.asItem()` 转换为物品
- 过滤空气和无物品形式的方块

#### 10.2 游戏内文件选择屏幕

仿照 Litematica 风格的文件选择界面。

**功能**:
- 自动扫描 `schematics/` 和 `config/litematica/schematics/` 目录
- 支持子文件夹导航
- 路径显示（以 schematics 为根）
- 返回上一级按钮
- 文件按名称排序
- 文件夹和文件区分显示
- 鼠标滚轮滚动
- 双击文件直接加载

**界面布局**:
```
┌─────────────────────────────────────┐
│     选择 Litematica 文件             │  ← 标题
├─────────────────────────────────────┤
│[上一级] schematics / 子文件夹        │  ← 路径栏
├─────────────────────────────────────┤
│ [+] 文件夹1    (青色)               │
│ [+] 文件夹2    (青色)               │  ← 文件列表
│     文件1.litematic                 │
│     ...                             │
├─────────────────────────────────────┤
│       [加载]    [取消]              │  ← 底部按钮
└─────────────────────────────────────┘
```

**相关文件**:
- `util/LitematicaLoader.java` - Litematica 文件解析
- `screen/LitematicaFileSelectorScreen.java` - 文件选择屏幕
- `MaterialPreparerClient.java` - 按钮触发逻辑

---

## 架构设计

### 包结构

```
io.github.myworldzycpc.material_preparer/
├── MaterialPreparer.java              # 主类
├── client/                            # 客户端代码
│   ├── MaterialPreparerClient.java    # 客户端主类
│   ├── ModMenuIntegration.java        # ModMenu 集成
│   ├── EventHandler.java              # 事件处理器
│   ├── ExplorationPhase.java          # 探索阶段枚举
│   ├── InteractionRecord.java         # 交互记录
│   ├── config/                        # 配置相关
│   │   ├── MaterialPreparerConfig.java  # 配置类
│   │   ├── ItemEntry.java               # 物品条目
│   │   ├── ItemEntryController.java     # 物品条目控件（YACL）
│   │   └── ItemListSerializer.java      # CSV 序列化工具
│   ├── keybind/                       # 快捷键相关
│   │   ├── KeybindingElement            # 快捷键元素基类
│   │   ├── KeybindEntry.java            # 快捷键条目
│   │   ├── CustomKeybind.java           # 自定义快捷键
│   │   ├── KeybindHelper.java           # 快捷键辅助工具
│   │   ├── ActionKeybindController.java # 动作型快捷键控件
│   │   └── TickBoxKeybindController.java # 勾选框型快捷键控件
│   ├── screen/                        # 屏幕相关
│   │   ├── ScreenHasParent.java         # 带父屏幕的基类
│   │   ├── EditItemScreen.java          # 编辑物品界面
│   │   └── LitematicaFileSelectorScreen.java # 文件选择界面
│   ├── util/                          # 工具类
│   │   └── LitematicaLoader.java       # Litematica 文件加载
│   └── mixin/                         # 客户端 Mixin
│       ├── ExampleClientMixin.java
│       └── MixinClientPacketListener.java
└── mixin/                             # 通用 Mixin
    └── ExampleMixin.java
```

### 核心类详解

#### MaterialPreparerClient

客户端主类，包含所有核心逻辑和静态状态。

**主要静态变量**:
- `mc` - Minecraft 实例
- `config` - 配置实例
- `itemList` - 物品列表（配置界面中编辑）
- `chestMap` - 箱子位置与物品列表的映射
- `craftingTables` - 工作台位置集合
- `blacklistedContainers` - 黑名单容器集合
- `outputContainers` - 输出容器集合
- `lastInteractionRecord` - 上次交互记录
- `currentExplorationPhase` - 当前探索阶段
- `scheduledBlockPosForExploration` - 待探索的方块位置列表
- `scheduledOutputContainers` - 待尝试的输出容器列表
- `suspendedContainerPos` - 挂起的容器位置
- `isCollectingItems` - 是否正在收集物品
- `neededItems` - 需要的物品及数量
- `containerClickQueue` - 点击队列（速率限制）
- `tickCounter` - tick 计数器
- `keybindEntries` - 所有快捷键条目列表

**主要方法**:
- `onInitializeClient()` - 客户端初始化
- `clickContainerSlot()` - 点击容器槽位
- `processContainerClickQueue()` - 处理点击队列
- `quickMoveContainerSlot()` - 快速移动（Shift+点击）
- `preciseMoveContainerSlot()` - 精确移动
- `placeCursorItemIntoPlayerInventory()` - 放置鼠标物品到背包
- `toggleBlacklist()` - 切换黑名单
- `toggleOutputContainer()` - 切换输出容器
- `clearAllMarkers()` - 清除所有标记
- `loadItemListFromLitematica()` - 从 Litematica 加载物品列表
- `isPlayerInventoryFull()` - 检查玩家背包是否已满
- `isContainerNotFull()` - 判断容器是否没满

#### EventHandler

事件处理器，处理客户端 tick、世界渲染、断开连接等事件。

**主要方法**:
- `onClientTick()` - 客户端 tick 事件，处理状态机
- `onWorldRenderLast()` - 世界渲染后事件，绘制调试边框
- `onDisconnect()` - 断开连接事件，清空会话数据
- `startItemCollection()` - 开始物品收集
- `exploreAllNearbyContainers()` - 探索所有附近容器
- `tryNextOutputContainer()` - 尝试下一个输出容器

#### ExplorationPhase

探索阶段枚举，定义状态机的所有状态。

**状态列表**:
- `IDLE` - 空闲状态
- `WAIT_NEXT` - 等待下一个容器探索
- `WAIT_OPEN` - 等待容器打开
- `WAIT_SET_CONTENTS` - 等待容器内容包
- `PROCESS_NEXT_ITEM` - 处理下一个物品
- `WAIT_ITEM_MOVE` - 等待物品移动完成
- `MOVING_ITEMS` - 正在移动物品（保留未使用）
- `WAIT_CLOSE` - 等待容器关闭
- `WAIT_OUTPUT_OPEN` - 等待输出容器打开
- `WAIT_OUTPUT_SET_CONTENTS` - 等待输出容器内容
- `TRANSFERRING_TO_OUTPUT` - 正在转移物品到输出容器
- `WAIT_OUTPUT_CLOSE` - 等待输出容器关闭

#### MaterialPreparerConfig

配置类，使用 YACL v3 的 `ConfigClassHandler` 和 `GsonConfigSerializer`。

**配置项**:
- `showDebuggingBorders` - 显示调试边框
- `minClickInterval` - 最小点击间隔
- `alwaysQuickMove` - 始终快速移动
- `ignoreExistingItems` - 忽略已有物品
- 各种快捷键配置（String 类型，存储按键绑定）

**配置文件**: `config/material_preparer/config.json5`

#### ItemEntry

物品条目，Java record 类型。

**字段**:
- `item` - Item 对象
- `count` - 数量（最小值为 1）

**方法**:
- `withItem(Item item)` - 返回新的 ItemEntry，替换 item
- `withCount(int count)` - 返回新的 ItemEntry，替换 count

#### ItemListSerializer

CSV 序列化工具类，提供静态方法。

**方法**:
- `loadFromCsv(Path filePath)` - 从 CSV 加载物品列表
- `saveToCsv(List<ItemEntry> itemList, Path filePath)` - 保存到 CSV

#### KeybindHelper

快捷键辅助工具，帮助创建 YACL 配置选项。

**方法**:
- `actionOption()` - 创建动作型快捷键选项
- `tickBoxOption()` - 创建勾选框型快捷键选项
- `withScreenGuard()` - 带界面防护的 Runnable

#### LitematicaLoader

Litematica 文件解析工具类。

**方法**:
- `loadItemListFromLitematica(File file)` - 从 Litematica 文件加载物品列表
- `processRegion()` - 处理单个区域
- 其他内部辅助方法

#### LitematicaFileSelectorScreen

游戏内文件选择屏幕，继承自 `ScreenHasParent`。

**主要功能**:
- 显示 schematics 目录下的文件和文件夹
- 支持子文件夹导航
- 路径显示
- 返回上一级
- 文件选择和加载

---

## 核心功能实现细节

### 物品收集流程

```
用户触发收集
    ↓
startItemCollection()
    ↓
从 CSV 加载物品列表 → neededItems
    ↓
如果 ignoreExistingItems：减去玩家已有和输出容器已有
    ↓
收集附近的箱子位置 → scheduledBlockPosForExploration
初始化 scheduledOutputContainers（按优先级排序）
    ↓
设置 isCollectingItems = true
设置 currentExplorationPhase = WAIT_NEXT
    ↓
onClientTick 中逐个打开箱子
    ↓
WAIT_OPEN → 打开容器
    ↓
WAIT_SET_CONTENTS → 收到容器内容
    ↓
PROCESS_NEXT_ITEM → 找到下一个需要的物品，移动
    ↓
WAIT_ITEM_MOVE → 等待点击队列清空
    ├─ 背包满了 → 挂起容器 → WAIT_CLOSE → 转移到输出容器
    └─ 背包没满 → PROCESS_NEXT_ITEM
    ↓
所有物品处理完 → WAIT_CLOSE → 关闭容器
    ↓
继续下一个箱子 → WAIT_NEXT
    ↓
所有箱子探索完 → 显示结果 → IDLE
```

### 精确移动原理

当需要移动 5 个物品，但源槽位有 7 个时：

1. **左键拿起整个堆叠**（7 个物品到鼠标上）
2. **右键放回 2 次**（每次放回 1 个，现在鼠标上有 5 个）
3. **智能放置到玩家背包**：
   - 第一遍：找到相同物品且未满的槽位，左键放下（填满该堆叠）
   - 第二遍：如果还有剩余，找空格子放下

### 容器标记系统

#### 数据结构

```java
Set<BlockPos> blacklistedContainers;   // 黑名单
Set<BlockPos> outputContainers;        // 输出容器
```

#### 渲染逻辑

在 `onWorldRenderLast` 中遍历所有标记的位置，绘制边框：
- 黑名单：黑色 (0, 0, 0)
- 输出容器：红色 (1, 0, 0)
- 普通容器：黄色（已探索）/ 灰色（未探索）

标记的方块始终显示边框，无论是否在 chestMap 中。

### Litematica 文件解析

#### 文件格式

- GZIP 压缩的 NBT 格式
- 根 compound 包含：Version, Metadata, Regions 等
- Regions 是一个 compound，每个子元素是一个区域
- 每个区域包含：Size, Position, BlockStatePalette, BlockStates 等

#### BlockStates 解包

- 每个方块的索引占用固定位数，由调色板大小决定：`bits = ceil(log2(paletteSize))`
- 索引连续存储，**跨 long 边界**（不是每个 long 存储整数个索引）
- 小端格式（低位在前）

**解包算法**:
```java
int bits = ceil(log2(paletteSize));
int mask = (1 << bits) - 1;
int currentBit = 0;
int longIndex = 0;
long currentLong = blockStates[0];

for (int i = 0; i < totalBlocks; i++) {
    int index;
    
    if (currentBit + bits > 64) {
        // 索引跨越了 long 边界
        int remainingBits = 64 - currentBit;
        long part1 = (currentLong >>> currentBit) & ((1L << remainingBits) - 1);
        
        longIndex++;
        currentLong = blockStates[longIndex];
        
        long part2 = currentLong & ((1L << (bits - remainingBits)) - 1);
        
        index = (int) (part1 | (part2 << remainingBits));
        currentBit = bits - remainingBits;
    } else {
        index = (int) ((currentLong >>> currentBit) & mask);
        currentBit += bits;
    }
    
    // 使用 index 从调色板获取方块
}
```

---

## 后续计划

### 1. 合成路径规划与自动合成

- 根据物品列表自动计算合成路径
- 自动打开工作台进行合成
- 支持多级合成（如先合成木板，再合成工作台）
- 考虑原材料是否足够

### 2. 导入多种格式的 CSV

- 支持不同格式的 CSV 文件导入
- 支持自定义分隔符
- 支持不同的列顺序
- 支持带/不带表头

### 3. 物品名称搜索功能

- 在编辑物品界面支持按物品名称搜索
- 目前只支持按物品 ID 搜索
- 需要获取物品的显示名称进行匹配

### 4. 潜影盒统计与管理

- 统计潜影盒中的物品
- 跟踪潜影盒内的物品变化
- 自动放置/回收潜影盒

### 5. 检查容器是否被阻挡

- 检查箱子上方是否有方块阻挡
- 阻挡的箱子无法打开，自动跳过
- 考虑潜影盒的朝向

### 6. 暂停和继续

- 支持暂停当前的探索/收集操作
- 暂停后可以继续
- 快捷键控制暂停/继续

### 7. 简单寻路算法

- 当容器不在交互范围内时，自动移动到附近
- 简单的 A* 或直线寻路
- 避免卡墙

---

## 代码改进方向（待定）

### 1. 代码结构优化

- 将 MaterialPreparerClient 拆分为多个更小的类
  - ContainerInteractionManager - 容器交互管理
  - ItemCollectionManager - 物品收集管理
  - ContainerMarkerManager - 容器标记管理
- 减少静态变量，使用单例或依赖注入

### 2. 状态机重构

- 将状态机逻辑独立出来
- 使用状态模式，每个状态一个类
- 减少 EventHandler 中的大段 switch 语句

### 3. 错误处理增强

- 更完善的异常处理
- 更多的日志输出
- 错误恢复机制

### 4. 性能优化

- 减少不必要的遍历
- 优化 chestMap 的数据结构
- 渲染优化（只渲染可见的边框）

### 5. 测试覆盖

- 添加单元测试
- 添加集成测试
- 测试各种边界情况

### 6. 文档完善

- 添加 JavaDoc 注释
- 完善开发文档
- 添加贡献指南

### 7. 配置系统优化

- 更多的配置选项
- 配置验证
- 配置迁移（版本升级时）

### 8. 兼容性

- 与其他模组的兼容性测试
- 支持更多类型的容器（不只是箱子、潜影盒、木桶）
- 支持更多版本的 Minecraft

---

## 相关文件路径

### 核心源码
- 主类: `src/main/java/io/github/myworldzycpc/material_preparer/MaterialPreparer.java`
- 客户端主类: `src/client/java/io/github/myworldzycpc/material_preparer/client/MaterialPreparerClient.java`
- 配置类: `src/client/java/io/github/myworldzycpc/material_preparer/client/config/MaterialPreparerConfig.java`

### 资源文件
- 英文本地化: `src/main/resources/assets/material-preparer/lang/en_us.json`
- 中文本地化: `src/main/resources/assets/material-preparer/lang/zh_cn.json`

### 配置文件（运行时生成）
- 主配置: `config/material_preparer/config.json5`
- 物品列表 CSV: `config/material_preparer/item_list.csv`
