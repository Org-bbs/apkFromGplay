# APK From GPlay

一个示例 Android App：支持在 **Google Play 网页端搜索应用**，并通过可公开访问的 APK 下载源发起下载，下载完成后调用系统安装流程安装 APK。

## 功能

- 关键词搜索 Google Play 应用（网页搜索解析）
- 展示应用标题、开发者、包名
- 一键下载 APK（后台下载）
- 下载完成后自动拉起系统安装界面
- Android 单元测试（JUnit + Robolectric）
- Roborazzi UI 截图测试（在 JVM 测试中产出截图）

> 说明：Google Play 官方不提供公开直链 APK 下载接口。本项目使用 Google Play 进行“搜索”，APK 下载链接由公开镜像源构造，仅用于技术演示。

## 本地构建与测试

```bash
./gradlew clean testDebugUnitTest assembleDebug
```

构建成功后：

- APK：`app/build/outputs/apk/debug/app-debug.apk`
- 单元测试报告：`app/build/reports/tests/testDebugUnitTest/index.html`
- Roborazzi 结果：`app/build/outputs/roborazzi/`

## GitHub Actions

仓库内置工作流：`.github/workflows/android-ci.yml`

工作流会自动执行：

1. 编译与测试：`clean testDebugUnitTest assembleDebug`
2. 上传可下载产物：
   - `app-debug-apk`（可下载安装）
   - `unit-test-reports`（JUnit XML + HTML 报告）
   - `roborazzi-results`（UI 截图测试结果）

在 GitHub Actions 的对应运行页中可直接下载并查看以上产物。