# AppDataTable 可排序模式设计

## 目标

`AppDataTable` 保持项目统一的表格布局、响应式、分页、选择和行操作能力。`sh.calvin.reorderable` 仅作为可选拖动交互层，用于用户可配置顺序的有限列表，例如饮品快捷预设、收藏项和自定义模板。

## API

```kotlin
@Immutable
data class AppDataTableReorder<T>(
    val onMove: (fromIndex: Int, toIndex: Int) -> Unit,
    val onMoveFinished: (() -> Unit)? = null,
)
```

`AppDataTable(..., reorder = AppDataTableReorder(...))` 启用排序模式。调用方持有有序 `rows` 并在 `onMove` 中仅更新内存列表；`onMoveFinished` 在拖动结束后执行一次，供 ViewModel/Repository 持久化。表格不复制或写入领域数据。

## 约束

- 排序模式必须提供稳定且唯一的 `rowKey`。
- 排序模式不支持分页，`showPager` 必须为 `false`。
- 排序模式不支持多选，`selectionEnabled` 必须为 `false`。
- 第一版只支持 `HorizontalScroll` 布局；响应式紧凑布局保留给记录和查询表。需要排序的业务列表应使用横向布局，避免拖动手势与紧凑行的整行点击语义竞争。
- 拖动仅从统一的首列手柄启动，保证窄屏横向表格初始位置可见，且不抢占整行点击和横向滚动。

不满足约束时开发期立即 `require` 失败，避免产生跨页重排、索引错位或手势竞争。

## 稳定性与性能

- `rowKey` 既供 `LazyColumn` 复用，也供 reorderable 识别拖动项；不得使用可变索引。
- 表格持续使用 `LazyColumn` 虚拟化，禁止为排序改为全量 `Column`。
- 拖动回调只重排调用方内存列表；不得在拖动帧内写文件、访问 Repository 或触发网络操作。
- 列宽仅在列定义、可用宽度或动作列变化时计算；分页切片通过索引构建，避免 `drop()` 产生中间列表。
- 行号按渲染索引提供，不在单元格中调用 `indexOf`，避免 O(n) 查找。

## 持久化规则

领域 Repository 接收完整排序结果或完整 ID 序列，并校验 ID 集合不重复、无缺失、无未知项。成功后一次性落盘；失败时由 ViewModel 重载权威数据恢复 UI。排序数组顺序即默认顺序，不另存冗余 `sortOrder` 或默认 ID。

## 非适用场景

时间倒序记录、查询结果、服务端分页、跨页历史数据和批量选择表不启用排序模式。图表继续使用项目自研 Canvas，项目不引入第三方图表库。
