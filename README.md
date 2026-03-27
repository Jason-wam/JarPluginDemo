# JarPluginDemo

一个用于创建视频插件的示例项目，演示如何实现一个完整的视频插件，包括分类、搜索、详情、播放等功能。

## 项目结构

```
JarPluginDemo/
├── .idea/            # IDE配置文件
├── DexTools/         # Dex转换工具
├── gradle/           # Gradle包装器
├── modules/          # 模块
│   ├── engine/       # 插件引擎核心模块
│   └── network/      # 网络请求模块
├── src/              # 示例插件源码
│   └── main/kotlin/  # Kotlin源码
├── build.gradle.kts  # 项目构建脚本
└── settings.gradle.kts # 项目设置
```

## 如何创建插件

### 1. 了解插件基础结构

插件需要继承 `BaseJarSource` 类，实现以下核心方法：

- `getFaviconUrl()`: 获取插件图标
- `isSupportClass()`: 是否支持分类浏览
- `isSupportSearch()`: 是否支持搜索
- `isSupportHomeRecommends()`: 是否支持首页推荐
- `getClassEntities()`: 获取分类列表
- `getHomeRecommends()`: 获取首页推荐（如果支持）
- `search()`: 搜索功能
- `getDetail()`: 获取视频详情
- `getVideoClassList()`: 获取分类下的视频列表
- `getPlayUrl()`: 获取播放地址
- `getDownloadUrl()`: 获取下载地址

### 2. 创建插件类

创建一个新的 Kotlin 文件，继承 `BaseJarSource` 类，并实现所有抽象方法。

```kotlin
class MyPlugin(context: JarSourceContext) : BaseJarSource(context) {
    init {
        // 插件基本信息
        id = "myplugin"           // 插件唯一ID
        name = "我的插件"         // 插件名称
        versionCode = 1           // 版本码
        versionName = "1.0.0"     // 版本名称
        description = "这是一个示例插件"  // 插件描述
    }

    // 实现各种抽象方法...
}
```

### 3. 实现核心方法

#### 3.1 配置插件基本信息

在 `init` 块中设置插件的基本信息，包括 ID、名称、版本等。

#### 3.2 实现图标方法

```kotlin
override fun getFaviconUrl(): String {
    // 返回图标 URL 或 Base64 编码的图片
    return "https://example.com/icon.png"
}
```

#### 3.3 实现分类相关方法

```kotlin
override fun isSupportClass(): Boolean {
    return true // 支持分类浏览
}

override fun getClassEntities(
    requestConfigs: UrlRequest<*>.() -> Unit,
    onError: (Throwable) -> Unit,
    onSuccess: (List<VideoClassEntity>) -> Unit
) {
    // 返回分类列表
    onSuccess(listOf(
        VideoClassEntity().apply {
            title = "分类1"
            url = "https://example.com/category1"
        },
        VideoClassEntity().apply {
            title = "分类2"
            url = "https://example.com/category2"
        }
    ))
}

override fun getVideoClassList(
    clazz: VideoClassEntity,
    page: Int,
    requestConfigs: UrlRequest<*>.() -> Unit,
    onError: (Throwable) -> Unit,
    onSuccess: (BaseClassResultEntity) -> Unit
) {
    // 从网络获取分类下的视频列表
    OkHttpClientUtil.enqueue<JSONObject> {
        url(clazz.url + "?page=$page")
        apply(requestConfigs)
        onError { onError(it) }
        onSuccess { response ->
            // 解析响应数据
            val videos = response.getJSONArray("videos")
            val resultList = mutableListOf<VideoClassResultEntity>()
            
            for (i in 0 until videos.length()) {
                val video = videos.getJSONObject(i)
                resultList.add(VideoClassResultEntity().apply {
                    title = video.getString("title")
                    cover = video.getString("cover")
                    duration = video.getString("duration")
                    detailPageUrl = video.getString("url")
                    sourceId = this@MyPlugin.id
                    sourceName = this@MyPlugin.name
                })
            }
            
            onSuccess(BaseClassResultEntity(
                hasMore = page < 10, // 假设有10页
                value = resultList
            ))
        }
    }
}
```

#### 3.4 实现搜索功能

```kotlin
override fun isSupportSearch(): Boolean {
    return true // 支持搜索
}

override fun search(
    keywords: String,
    page: Int,
    requestConfigs: UrlRequest<*>.() -> Unit,
    onError: (Throwable) -> Unit,
    onSuccess: (BaseSearchResultEntity) -> Unit
) {
    // 实现搜索逻辑
    val encodedKeywords = URLEncoder.encode(keywords, "UTF-8")
    
    OkHttpClientUtil.enqueue<JSONObject> {
        url("https://example.com/search?keyword=$encodedKeywords&page=$page")
        apply(requestConfigs)
        onError { onError(it) }
        onSuccess { response ->
            // 解析搜索结果
            val results = response.getJSONArray("results")
            val resultList = mutableListOf<VideoSearchResultEntity>()
            
            for (i in 0 until results.length()) {
                val video = results.getJSONObject(i)
                resultList.add(VideoSearchResultEntity().apply {
                    title = video.getString("title")
                    cover = video.getString("cover")
                    duration = video.getString("duration")
                    subtitle = video.getString("subtitle")
                    detailPageUrl = video.getString("url")
                    sourceId = this@MyPlugin.id
                    sourceName = this@MyPlugin.name
                })
            }
            
            onSuccess(BaseSearchResultEntity(
                hasMore = response.getBoolean("hasMore"),
                value = resultList
            ))
        }
    }
}
```

#### 3.5 实现详情和播放功能

```kotlin
override fun getDetail(
    detailPageUrl: String,
    requestConfigs: UrlRequest<*>.() -> Unit,
    onError: (Throwable) -> Unit,
    onSuccess: (VideoDetailEntity) -> Unit
) {
    // 获取视频详情
    OkHttpClientUtil.enqueue<JSONObject> {
        url(detailPageUrl)
        apply(requestConfigs)
        onError { onError(it) }
        onSuccess { response ->
            // 解析详情数据
            val video = response.getJSONObject("video")
            
            onSuccess(VideoDetailEntity().apply {
                title = video.getString("title")
                cover = video.getString("cover")
                description = video.getString("description")
                browserUrl = video.getString("url")
                url = detailPageUrl
                
                // 添加播放渠道
                channels.add(VideoDetailChannelEntity().apply {
                    id = "channel1"
                    title = "播放地址"
                    videoDataList.add(VideoDetailDataEntity().apply {
                        id = "video1"
                        title = "高清"
                        videoUrl = video.getString("playUrl")
                        videoType = VideoType.Direct
                        canDownload = true
                    })
                })
                
                sourceId = this@MyPlugin.id
                sourceName = this@MyPlugin.name
            })
        }
    }
}

override fun getPlayUrl(
    dataItem: VideoDetailDataEntity,
    requestConfigs: UrlRequest<*>.() -> Unit,
    onError: (Throwable) -> Unit,
    onSuccess: (PlayUrlEntity) -> Unit
) {
    // 获取播放地址
    onSuccess(PlayUrlEntity(
        url = dataItem.videoUrl,
        headers = hashMapOf("User-Agent" to "Mozilla/5.0...")
    ))
}

override fun getDownloadUrl(
    dataItem: VideoDetailDataEntity,
    requestConfigs: UrlRequest<*>.() -> Unit,
    onError: (Throwable) -> Unit,
    onSuccess: (DownloadUrlEntity) -> Unit
) {
    // 获取下载地址
    onSuccess(DownloadUrlEntity(url = dataItem.videoUrl))
}
```

### 4. 添加偏好设置

```kotlin
override fun getPreferences(): List<BasePreference> {
    return listOf(
        object : TextEditPreference(
            title = "API Key",
            summary = "请输入API Key",
            hint = "输入API Key..."
        ) {
            override fun getText(): String {
                return context.getString("api_key", "")
            }
            
            override fun onTextChanged(text: String) {
                context.putString("api_key", text)
            }
        },
        object : CheckboxPreference(
            title = "启用高清",
            summary = "开启后优先使用高清视频"
        ) {
            override fun isChecked(): Boolean {
                return context.getBoolean("enable_hd", true)
            }
            
            override fun onCheckedStateChange(checked: Boolean) {
                context.putBoolean("enable_hd", checked)
            }
        }
    )
}
```

### 5. 构建和部署插件

#### 5.1 构建 Jar 文件

使用 Gradle 命令构建插件：

```bash
# Windows
gradlew.bat jar

# Linux/Mac
./gradlew jar
```

构建成功后，Jar 文件会生成在 `build/libs/` 目录下。

#### 5.2 转换为 DEX 文件

项目已经配置了自动转换功能，构建 Jar 成功后会自动生成 `dex_4k.jar` 文件。

最终的插件文件为 `build/libs/xxx_Dex_4K_Aligned.jar`。

### 6. 测试插件

在 `src/test/kotlin` 目录下创建测试类，测试插件的各项功能：

```kotlin
class MyPluginTest {
    @Test
    fun testSearch() {
        val context = JarSourceContextTestImpl()
        val plugin = MyPlugin(context)
        
        plugin.search(
            keywords = "测试",
            page = 1,
            requestConfigs = {},
            onError = { it.printStackTrace() },
            onSuccess = { result ->
                println("搜索结果数量: ${result.value.size}")
                assert(result.value.isNotEmpty())
            }
        )
    }
}
```

## 示例插件

项目中包含了一个完整的示例插件 `HaoKanEngine.kt`，演示了如何实现一个功能完整的视频插件。

### 功能特点

- 支持分类浏览
- 支持搜索功能
- 支持视频详情和播放
- 支持 Cookie 设置
- 支持用户头像作为插件图标
- 完整的错误处理

## 常见问题

### 1. 插件无法加载

- 检查插件的 ID 是否唯一
- 确保插件类名与文件名一致
- 检查是否实现了所有必要的抽象方法

### 2. 网络请求失败

- 检查网络连接
- 检查 URL 是否正确
- 检查是否需要添加 User-Agent 或其他请求头
- 检查是否需要 Cookie 认证

### 3. 视频无法播放

- 检查视频 URL 是否正确
- 检查视频格式是否支持
- 检查是否需要特殊的播放参数

## 开发工具

- **IDE**: IntelliJ IDEA
- **语言**: Kotlin
- **构建工具**: Gradle
- **网络库**: OkHttpUtils
- **JSON 解析**: org.json
