# 私信发送框

底部发送框采用单行排列：左侧图片入口、中间圆角输入框、右侧表情入口；有正文或图片时才在最右侧显示发送按钮，空白草稿不占发送按钮的空间。沿用现有图标、主题颜色和发送按钮样式，正文可随输入增高至最多四行。选图缩略图显示在输入行上方，支持移除或更换；上传及发送期间显示状态并禁用重复操作。

表情复用评论区的 `CommentEmoteRepository`、`EmotePicker` 和原生 `CommentEditorEditText`，共享表情目录、账号缓存、最近使用及行内图片渲染。点击表情在光标处插入，选中文字时替换选区；删除时按完整表情或 Unicode code point 处理。评论列表与私信气泡继续使用 `CommentRichText`。

键盘和表情共用底部空间，记录 IME 的完整目标高度；切回键盘时暂时保留表情占位，交接后移除。首次进入不自动弹出键盘，返回键优先收起表情，底部导航栏只计算一次。

主窗口显式设置 `android:windowSoftInputMode="adjustResize"`，按 [Android 的 Compose 边到边配置要求](https://developer.android.com/develop/ui/compose/system/setup-e2e) 接收键盘遮挡高度。默认的自动模式在设备上可能选择 `adjustPan`，系统整体上移会与发送框自身的底部占位叠加，导致收起后再次弹出键盘时出现大块空白。

图片通过系统照片选择器单选，发送前显示可移除、更换的缩略图，也可以不填写文字直接发送。复用评论区的 `ImageAttachmentHandler` 校验文件；私信独立使用 `/x/dynamic/feed/draw/upload_bfs` 的 `biz=im` 上传，再通过已有 gRPC `SendMsg` 发送类型 2 的图片消息。图片正文包含上传结果的 URL、宽高、格式、原图标记和大小。接口参数参考 [PiliPlus 私信图片发送实现](https://github.com/bggRGjQaUbCoE/PiliPlus/blob/main/lib/pages/whisper_detail/view.dart)。

正文及选图保存在当前会话的 `SavedStateHandle` 中，成功发送后才清空。文字和图片同时选择时依次发送为两条消息；已成功的文字清空后，图片上传或发送失败仍保留图片，重试不会再次发送文字。已上传但发送失败的图片在当前 ViewModel 中缓存上传结果，重试复用。文字消息通过 `JSONObject` 编码，支持引号、反斜杠和换行。

## 性能

会话列表与编辑区分别订阅自己的状态，输入文字、移动光标、切换表情及上传状态只更新编辑区。草稿只在正文发生变化时写入 `SavedStateHandle`，光标及输入法组合区变化不重复保存正文。

消息的显示文字、时间及图片比例由 ViewModel 在 `Dispatchers.Default` 上生成。会话级缓存按消息 key 和完整内容复用消息行及时间分隔行，加载历史或追加发送结果时只重新计算新增、变化的消息；缓存仅保留当前已加载消息，并在日期或时区变化时重算时间。列表继续使用稳定 key 与内容类型，分页在 `snapshotFlow` 中监听滚动，不让滚动位置触发整页重组。

消息气泡共享一个表情目录索引，纯文字不进入表情正则解析。最近表情在内存中更新，并由后台协程按顺序保存；连续输入时合并尚未保存的记录，避免在点击表情时重建完整目录和读写偏好设置。相关做法参考 [Compose 性能最佳实践](https://developer.android.com/develop/ui/compose/performance/bestpractices)。

## 验证

自动检查：

```powershell
.\gradlew.bat :feature:im:testDebugUnitTest :core:im:testDebugUnitTest :feature:comment:testDebugUnitTest :core:comment:testDebugUnitTest :app:assembleDebug
```

单元检查覆盖消息行复用、时间分组及跨日期/时区失效、历史消息增补、图片比例和表情索引共享。未进行设备性能测量，不提供帧率或耗时提升的结论。

手动验证由开发者完成：长会话连续输入、快速插入表情、滚动加载多页历史后继续输入及发送；连续多次收起及再次弹出键盘时发送框应紧贴键盘、表情与键盘切换、光标插入和整体删除、图片选择与移除、仅图片发送、文字和图片依次发送、断网失败保留草稿及重试，以及横屏、导航栏和深浅色布局。实际发送需要使用测试账号联调。
