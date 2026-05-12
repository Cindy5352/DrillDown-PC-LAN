# Agent 指南

## 项目定位
Drill Down 当前是纯 PC 版本，重点是局域网房主制联机。
- 默认不要修改 `commons/` 和 `gdx-sfx/` 这两个子项目；如果任务确实需要动它们，先单独说明。

## 联机现状
- 会话层已存在：`core/src/de/dakror/quarry/net/LanSession.java`
- 快照层已存在：`core/src/de/dakror/quarry/net/LanSnapshot.java`
- 主流程已接好：加入时拉快照，房主广播命令，客户端发送命令
- 已支持的命令：`pause`、`speed`、`build`、`build_batch`、`destroy`、`rotate`、`flip`
- 协同光标已接入：`Game.update()` 周期性发送 `cursor`，`Game.draw()` 渲染远程鼠标，`GameUi.update()` 显示跨层状态
- 协同光标采用屏幕坐标 + 归一化坐标同步，远程端用游戏自带 `mouseTex` 直接绘制鼠标图标
- 协同光标是常驻状态，不再按超时自动清除；仅在切到其他层或连接断开时隐藏/清理
- 箱子/库存类结构已接入周期性快照同步：房主每 0.5 秒广播一次 `storage_sync`，客户端按 `layer/x/y` 覆盖库存并刷新 UI
- 库存 UI 刷新已经抽成统一入口，避免联机回包后面板显示残留旧数据
- tooltip 开关已经统一走 `GameUi.setTooltipsEnabled(...)`，避免客机侧把 hover 提示误关死
- 当前已固定的联机方向：
  - 房主权威模拟，客户端只提交操作
  - 每个客户端可本地切层，切层不广播
  - 所有建造/拆除/旋转/翻转命令都带 `layer`
  - 全部层持续模拟，只渲染当前层
  - 协同光标只做提示，不影响世界状态
  - 正常断开连接不再视为异常，`SocketException: Connection reset` 这类退出噪音已压低
- 仍待完善：过程态同步、断线重连、分包/重试、建造资源裁决

## 核心入口
- 桌面启动：`de.dakror.quarry.desktop.DesktopLauncher`
- 联机开房：`de.dakror.quarry.scenes.Game.startLanHost(...)`
- 联机加入：`de.dakror.quarry.scenes.Game.joinLanClient(...)`
- 联机发包：`de.dakror.quarry.scenes.Game.emitLanCommand(...)`
- 联机收包：`de.dakror.quarry.scenes.Game.applyLanCommand(...)`

## 游戏速度控制
- 当前游戏不是固定 tick 驱动，而是每帧按 `deltaTime` 推进，逻辑速度由 `gameSpeed` 参与计算。
- `de.dakror.quarry.scenes.Game.increaseSpeed()`
  - 将 `gameSpeed` 直接翻倍，最高到 `100`。
  - 会在联机时广播 `speed` 命令给其他端。
- `de.dakror.quarry.scenes.Game.resetSpeed()`
  - 将 `gameSpeed` 重置为 `1`。
  - 会在联机时广播 `speed` 命令给其他端。
- `de.dakror.quarry.scenes.Game.setPaused(boolean)`
  - 切换暂停状态。
  - 联机时广播 `pause` 命令。
- `de.dakror.quarry.scenes.Game.update(double deltaTime)`
  - 这是主更新入口。
  - `gamePaused` 为真时，主逻辑大多按 `0` 速度运行。
  - 正常情况下会把 `gameSpeed` 传给 `powerGrid.update(...)`、`Layer.update(...)`。
- `de.dakror.quarry.game.Layer.update(double deltaTime, int gameSpeed)`
  - 按当前速度更新该层所有结构、特效和声音相关状态。
- `de.dakror.quarry.game.Chunk.update(double deltaTime, int gameSpeed, Bounds dirtyBounds)`
  - 将速度继续下传给结构和传送带物品更新。
- 具体影响范围
  - 生产/生成/冷却/燃烧/泵出等耗时逻辑通常乘 `deltaTime * gameSpeed`。
  - 传送带、库存、发电、液体管路等会按速度加快或暂停。
  - 渲染本身不受速度影响，只有世界状态推进受影响。

## 联机实现位置
- `core/src/de/dakror/quarry/scenes/Game.java`
  - `applyLanCommand(...)` 处理 `pause`、`speed`、`build`、`destroy`、`rotate`、`flip`、`cursor`
  - `placeStructureInternal(...)`、`removeStructureInternal(...)` 本地操作后会发联机命令
  - `changeLayer(...)` 只切本地视角层，不广播
  - `syncLocalCursor(...)`、`drawRemoteCursors()` 负责协同鼠标；本地同步当前 tile、屏幕坐标与归一化坐标
  - `syncLanStorageStates(...)` 定期打包 `Layer.storages` 的快照并广播，`applyLanStorageSync(...)` 在客户端落库存
- `core/src/de/dakror/quarry/scenes/GameUi.java`
  - `LayerSelection` 只改本地当前层
  - `rotateActiveStructure()`、`flipActiveStructure()` 会带层号发命令
  - `setTooltipsEnabled(...)` 统一管理 hover 提示的启停，避免联机模式下 tooltip 状态被局部逻辑改乱
  - `update(...)` 刷新协同光标文字状态
- `core/src/de/dakror/quarry/net/LanSession.java`
  - 房主监听、客户端连接、快照下发、命令转发
  - 客户端/房主断开时会清理远程鼠标状态，正常退出不再刷异常
- `core/src/de/dakror/quarry/structure/storage/Storage.java`、`Barrel.java`、`Warehouse.java`
  - 库存 UI 和输出格子背景改成可重刷的状态入口，确保联机同步后界面能立即回显
- `core/src/de/dakror/quarry/structure/base/StorageStructure.java`
  - 新增 `applySyncedState(...)` / `refreshUIFromState()`，给联机库存快照提供统一落点

## 打包现状
- 便携包任务已加入：`desktop:portableZip`
- 输出目录：`distribute/output/portable/`
- 便携包内容：`jre/`、`DrillDown.jar`、`run.bat`、`run-debug.bat`
- 压缩包输出：`distribute/output/DrillDown-portable.zip`
- 便携包使用 `jlink` 生成 JRE，不依赖目标机器预装 Java

## 目录优先级
1. `core/src/de/dakror/quarry/`
2. `desktop/src/`
3. `assets/`
4. 根目录 `build.gradle`、`settings.gradle`

## 处理原则
- 优先保留 PC 和联机相关代码。
- 不再恢复 Android 入口、权限流或移动端资源目录。
- 改动时先看 `Game.java`、`LanSession.java`、`DesktopLauncher.java`、`desktop/build.gradle`。
- 回答时尽量直接指出具体文件。
- 默认不要修改两个子项目：`commons/` 和 `gdx-sfx/`。如果任务确实需要动它们，先明确说明再单独处理。
- `agent.md` 仅作为本地项目指引维护，不要纳入 GitHub 推送；除非用户明确要求，否则不要把它加入提交。
- 提交说明默认使用中文。
- 使用垃圾桶贴图时要尽量隔离修改，避免影响其他资源正常加载；优先只改垃圾桶自身资源与引用，并验证其它贴图页保持稳定。
