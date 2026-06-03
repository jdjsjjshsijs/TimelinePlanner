# TimelinePlanner

Android 日程时间线规划应用，帮助用户以分钟级精度管理每日任务。

## 功能特性

### 时间线视图
- 以时间轴形式展示每日任务
- 左右滑动切换日期，点击日期可快速跳转
- 支持多选任务批量删除
- Material3 风格的日期选择器

### 任务管理
- 创建/编辑/删除任务
- 自定义任务颜色（8 种预设颜色）
- 支持备注信息
- 精确到分钟的时间设置

### 计时器
- 实时计时，支持暂停/继续/结束
- 记录暂停时间段，自动计算有效时长
- **专注模式**：计时器运行时锁定应用，隐藏导航栏和状态栏，防止分心
- 暂停时可正常退出 app，计时状态自动保存，重新打开自动恢复
- 计时面板显示当前时间（HH:MM:SS）
- 计时结束后保存结果到任务

### 云同步
- 任务数据自动同步到远程服务器，防止数据丢失
- 每次增删改操作后全量备份到服务器
- 启动时自动从服务器恢复数据
- 同步状态实时显示（云朵图标）
- Python Flask 后端，SQLite 存储
- 支持数据导出：CSV（Excel）、JSON 格式

### 每日总结
- 圆环图展示任务时间占比
- 点击圆环色块查看任务名称和耗时（带凸出动画）
- 支持日/周/月三种维度切换，周/月视图显示日均时长
- 任务列表中点击彩色圆点展开/收起时间范围
- 统计已安排时间和空闲时间
- 任务数据实时同步，增删任务后自动刷新

### 操作回溯
- 支持多步撤销（创建、编辑、删除操作均可撤销）
- 撤销历史永久保留，通过 FAB 菜单访问
- 任务创建/编辑事件自动记录到撤销栈

### AI 助手
- 接入 DeepSeek API，支持自然语言指令
- 创建/修改/删除任务："帮我加一个 9:00-9:30 的晨会"
- 查询日程："今天有什么安排？"
- 支持自定义 API 地址和模型
- 对话历史持久化存储

## 技术栈

| 类别 | 技术 |
|------|------|
| 语言 | Kotlin |
| UI | Jetpack Compose + Material3 |
| 架构 | MVVM |
| 依赖注入 | Hilt |
| 数据库 | Room |
| 网络 | Retrofit + OkHttp |
| 序列化 | Gson |
| AI | DeepSeek API（兼容 OpenAI 格式） |

## 项目结构

```
app/src/main/java/com/example/timelineplanner/
├── data/
│   ├── ai/           # AI 响应解析、提示词构建、操作执行
│   ├── db/           # Room 数据库、DAO、Entity
│   ├── remote/       # Retrofit API 接口和客户端
│   └── repository/   # 数据仓库层
├── di/               # Hilt 依赖注入模块
├── model/            # 数据模型
├── ui/
│   ├── aichat/       # AI 聊天界面
│   ├── navigation/   # 导航路由
│   ├── summary/      # 每日总结页面
│   ├── taskdetail/   # 任务详情/编辑/计时器
│   ├── theme/        # 主题、颜色、字体
│   └── timeline/     # 时间线主页面
└── util/             # 工具类（日期格式化等）

server/
├── app.py            # Flask 同步服务器
└── requirements.txt  # Python 依赖
```

## 构建与运行

1. 克隆项目
```bash
git clone https://github.com/jdjsjjshsijs/TimelinePlanner.git
```

2. 用 Android Studio 打开项目

3. 连接设备或启动模拟器，点击 Run

### 云同步服务器（可选）

```bash
cd server
pip install flask flask-cors
python app.py
```

服务器默认监听 `0.0.0.0:5000`，需在 `SyncClient.kt` 中配置服务器 IP 地址。

#### API 接口

| 接口 | 方法 | 说明 |
|------|------|------|
| `/api/tasks/sync` | POST | 同步任务数据 |
| `/api/tasks?date=<millis>` | GET | 获取指定日期任务 |
| `/api/tasks/all` | GET | 获取全部任务（JSON） |
| `/api/tasks/<id>` | DELETE | 删除指定任务 |
| `/api/tasks/export/csv` | GET | 导出 CSV 文件 |
| `/api/tasks/export/json` | GET | 导出格式化 JSON |

## 权限说明

- **INTERNET** — AI 助手网络请求
- **屏幕固定** — 计时器专注模式（需手动开启：设置 > 屏幕固定）
