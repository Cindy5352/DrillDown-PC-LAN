# DrillDown

一款基于 Java / libGDX 的工厂建造游戏，当前重点面向 PC 与 LAN 联机。

## 实现原理

- 游戏主循环采用 `deltaTime` 连续时间模拟，所有生产、输送、电力、冷却等逻辑都按“经过了多少秒”推进。
- 联机采用主机权威模型。
- 加入房间时，客户端先接收主机导出的整局快照并直接加载。
- 日常操作通过自定义 TCP 命令广播，同步建造、拆除、旋转、速度、光标、预览等事件。
- 对容易漂移的结构运行态，主机会周期性发送结构快照做纠偏。
- 传送带等复杂对象在客户端会减少本地模拟，直接以主机同步状态为准。
- 玩家鼠标与建造预览通过轻量状态同步并在本地插值渲染。

## 构建依赖

- JDK 8
- Gradle
- libGDX 1.9.9
- 项目内模块：
  - `core`
  - `desktop`
  - `commons/core`
  - `commons/annotations`
  - `gdx-sfx/core`
  - `gdx-sfx/desktop`

## 构建方式

### 直接运行

```bash
./gradlew desktop:run
```

### 生成桌面版 JAR

```bash
./gradlew desktop:dist
```

### 生成便携包

```bash
./gradlew desktop:portableZip
```

输出通常位于 `distribute/output/`。

## 目录说明

- `core/`：核心游戏逻辑、场景、UI、结构和网络同步。
- `desktop/`：桌面启动器与桌面端构建配置。
- `commons/`：共享工具与注解模块。
- `gdx-sfx/`：共享音频支持。
- `assets/`：游戏资源。

## 联机说明

- 仅支持 PC 与 LAN。
- 使用 TCP Socket + NBT 二进制消息。
- 主要同步的是“操作”和“结构状态”，不是每帧整图广播。

