# KWormhole

在机器间同步文件！

## 工作原理

### KFR

KFR（Kwormhole File Record）代表着一个在某一时间刻上由 KWormhole 同步的文件记录。

每一个 KFR 都有其独特的标识符 `path` 和用于同步的几个变量 `size` 、`time` 和 `hash` 。如果该 KFR 记录了一个被删除的文件，则 `size` 为 `-1`、`hash` 为 `0`
。在同一个客户端/服务端和同一时间内，只能存在一个拥有其独特 `path` 的 KFR。

当两个 KFR 的 `path` 相同时，两个 KFR 的**路径相同**，反之则**路径不同**。

当两个 KFR 的 `size` 和 `hash` 相同时，两个 KFR 的**内容相同**，反之则**内容不同**。

当一个 KFR 的 `time` 大于另一个KFR的 `time` 时，前者比后者**更新**，若小于，前者比后者**更旧**。

当一个 KFR 和另一个 KFR ，路径相同、内容不同、前者比后者更新时，前者才**可替换**后者，反之则前者**不可替换**后者。

当一个 KFR 可替换一个客户端/服务端内唯一一个与其路径相同的 KFR 时，该 KFR 对于这个客户端/服务端是**有效的**，反之则是**无效的**。

### 上传文件变更（客户端）

文件变更是产生新 KFR 的唯一方式，监控它的方式有以下两种：

1. 客户端启动时，会记录其监控目录下的所有文件为 KFR，若该 KFR 有效，则保存。
2. 客户端运行时，注册由文件系统支持的监控服务，收到事件后，记录该文件为 KFR，若该 KFR 有效，则保存。

随后，客户端将保存的 KFR 上传到服务端上，如果该 KFR 在服务端是有效的，则响应客户端，要求客户端上传对应的文件内容。客户端就将 KFR 及对应的文件内容上传到服务端上。

### 接收文件变更（服务端）

当服务端接收到一个来自客户端的 KFR，它将检查其有效性，若它是有效的，则响应客户端，要求客户端上传对应的文件内容。

当服务端接收到一个来自客户端的 KFR 及对应的文件内容，它将检查其有效性。若它是有效的，则保存。

### 广播文件变更（服务端）

在服务端保存一个 KFR 之后，它将向所有连接的客户端广播此次保存的 KFR。

由于原先上传此 KFR 的客户端持有的 KFR 与此次广播的 KFR 相同，该 KFR 对于此客户端是无效的。

此外，当客户端首次连接到服务端时，服务端会向客户端发送其保存的所有 KFR。

### 下载文件变更（客户端）

当客户端接收到服务端的 KFR 广播后，客户端首先检查其有效性。如果该 KFR 对于客户端是有效的，则客户端将向服务端请求此 KFR 对应的文件内容。随后，客户端将保存该 KFR 及其对应的文件内容。

注意，在客户端接收广播到客户端向服务端请求文件内容的这段时间里，服务端的 KFR 可能发生变化。但由于有效性可以传递，服务端的新 KFR（若有）必定是有效的，因此客户端可以直接保存。