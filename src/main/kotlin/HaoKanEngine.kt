import com.jason.network.OkHttpClientUtil
import com.jason.network.cache.CacheMode
import com.jason.network.cache.CacheValidDuration
import com.jason.network.request.UrlRequest
import com.jason.searcher.base.BaseJarSource
import com.jason.searcher.entities.*
import com.jason.searcher.entities.home.HomeRecommendsEntity
import com.jason.searcher.preference.BasePreference
import com.jason.searcher.preference.TextEditPreference
import com.jason.searcher.utils.JarSourceContext
import com.jason.searcher.utils.toMd5String
import org.json.JSONObject
import java.net.URLEncoder
import java.util.*

/**
 * 好看视频插件示例
 * 演示如何实现一个完整的视频插件，包括分类、搜索、详情、播放等功能
 *
 * 学习要点：
 * 1. 如何继承 BaseJarSource 创建插件
 * 2. 如何实现分类浏览功能
 * 3. 如何实现搜索功能
 * 4. 如何解析 JSON 数据
 * 5. 如何处理需要 Cookie 认证的 API
 * 6. 如何创建用户偏好设置
 *
 * 每个插件包中只能包含一个插件
 *
 * 打包 Jar 文件：[gradle jar](gradle://build.gradle.kts@jar)
 * 或使用命令行：`gradle jar` 或 `./gradlew jar`
 *
 * 生成 Jar 后需要使用 /DexTools 转换为 Android 端可用的 dex.jar
 * 当前项目Gradle已配置自动转换，使用 gradle jar成功后会自动生成 dex_4k.jar
 *
 * jar输出目录 [build/libs/xxx_Dex_4K_Aligned.jar]
 *
 */
class HaoKanEngine(context: JarSourceContext) : BaseJarSource(context) {
    // 刷新 ID，用于标识一次刷新操作，格式：当前时间戳（9 位）+ "00000"
    private var shuaXinId = System.currentTimeMillis().toString().substring(0, 9) + "00000"

    // User-Agent，模拟浏览器访问，避免被网站拒绝
    private val userAgent =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/125.0.0.0 Safari/537.36 115Browser/35.23.0 Chromium/125.0"

    /**
     * 创建 Cookie 设置偏好
     * TextEditPreference 是文本输入类型的偏好设置
     * 用户可以通过此设置输入网站的 Cookie，用于访问需要登录的内容
     */
    private val setCookiePreference = object : TextEditPreference(
        title = "Cookie 设置", summary = "请在弹出的输入框中输入网站 Cookie。", hint = "请在输入网站 Cookie..."
    ) {
        // 从存储中读取 Cookie 值
        override fun getText(): String {
            return context.getString("cookie", "")
        }

        // 当用户修改 Cookie 时调用
        override fun onTextChanged(text: String) {
            // 保存 Cookie 到存储
            context.putString("cookie", text)
            // 更新图标（如果已登录则显示用户头像）
            updateIcon()
        }
    }

    init {
        // 插件 ID - 必须是全局唯一的标识符
        id = "haokan"
        // 插件名称 - 显示在界面上的名字
        name = "好看视频"
        // 插件版本码 - 整数，主程序根据此判断是否需要更新
        versionCode = 1
        // 插件版本名称 - 字符串形式的版本号
        versionName = "1.0.0"
        // 插件描述 - 介绍插件功能
        description = "好看视频是百度短视频旗舰品牌，知识、美食、生活、健康、文化、游戏、影视等。"
        // 初始化图标
        updateIcon()
    }

    /**
     * 更新插件图标
     * 如果用户配置了 Cookie，则尝试获取用户头像作为图标
     * 否则使用默认的 favicon
     */
    private fun updateIcon() {
        val cookie = setCookiePreference.getText()
        if (cookie.isEmpty()) {
            // 没有 Cookie，移除自定义图标
            context.remove("icon")
            refreshUserViews()
        } else {
            // 有 Cookie，请求用户信息获取头像
            OkHttpClientUtil.enqueue<String> {
                url("https://haokan.baidu.com/")
                // 设置缓存模式：优先使用网络，失败则使用缓存
                setCacheMode(CacheMode.NETWORK_ELSE_CACHE)
                // 缓存永久有效
                setCacheValidDuration(CacheValidDuration.FOREVER)
                header("User-Agent", userAgent)
                header("Cookie", setCookiePreference.getText())
                onError {
                    it.printStackTrace()
                }
                onSuccess {
                    try {
                        // 使用正则表达式从 HTML 中提取用户头像 URL
                        val icon = Regex("\"user_info\":\\{\"avatar\":\"(.*?)\"").find(it)?.groupValues?.getOrNull(1)
                        if (icon == null) {
                            // 未找到头像，说明 Cookie 失效，清除配置
                            context.remove("cookie")
                            context.remove("icon")
                            context.showToast("好看视频 Cookie 已失效，请更新 Cookie！")
                        } else {
                            // 保存头像 URL
                            context.putString("icon", icon)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    } finally {
                        refreshUserViews()
                    }
                }
            }
        }
    }

    /**
     * 是否支持分类浏览
     * 返回 true 表示插件支持分类功能，主程序会调用 getClassEntities 获取分类列表
     */
    override fun isSupportClass(): Boolean {
        return true
    }

    /**
     * 是否支持搜索
     * 返回 true 表示插件支持搜索功能，主程序会调用 search 方法进行搜索
     */
    override fun isSupportSearch(): Boolean {
        return true
    }

    /**
     * 是否支持首页推荐
     * 返回 false 表示不支持，主程序不会调用 getHomeRecommends
     */
    override fun isSupportHomeRecommends(): Boolean {
        return false
    }

    // 默认图标（Base64 编码的 WebP 图片）
    private val favicon =
        "data:image/webp;base64,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"

    /**
     * 获取插件图标
     * 支持 HTTP 网络图片或者 base64 编码的图片
     * 格式：data:image/[类型];base64,[编码数据]
     *
     * @return 返回图标的 URL 或 Base64 数据
     */
    override fun getFaviconUrl(): String {
        // 优先使用用户头像（如果有），否则使用默认图标
        return context.getString("icon", favicon)
    }

    /**
     * 获取插件的偏好设置列表
     * 主程序会根据此方法返回的设置项自动生成设置界面
     *
     * @return 返回偏好设置列表
     */
    override fun getPreferences(): List<BasePreference> {
        // 返回 Cookie 设置项，用户可以通过此设置输入 Cookie
        return listOf(setCookiePreference)
    }

    /**
     * 获取分类列表
     * 当 isSupportClass() 返回 true 时，主程序会调用此方法获取分类数据
     * !!!如果需要从 网络获取分类数据，请使用 异步请求 方法
     *
     * @param requestConfigs 请求配置函数，用于配置网络请求（缓存、Header 等）
     * @param onError 错误回调，当发生错误时调用
     * @param onSuccess 成功回调，当获取到分类数据时调用
     */
    override fun getClassEntities(
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (List<VideoClassEntity>) -> Unit
    ) {
        // 直接返回分类列表，每个分类包含标题和对应的 API URL
        // 格式：Pair<分类标题，API URL>
        onSuccess(
            listOf(
                // 好看视频的分类：全部、影视、音乐、VLOG、游戏等
                // 每个分类对应不同的 tab 参数
                "全部" to "recommend",
                "影视" to "yingshi_new",
                "音乐" to "yinyue_new",
                "VLOG" to "yunying_vlog",
                "游戏" to "youxi_new",
                "搞笑" to "gaoxiao_new",
                "综艺" to "zongyi_new",
                "娱乐" to "yule_new",
                "动漫" to "dongman_new",
                "生活" to "shenghuo_new",
                "广场舞" to "guangchuangwu_new",
                "美食" to "meishi_new",
                "宠物" to "chongwu_new",
                "三农" to "sannong_new",
                "军事" to "junshi_new",
                "社会" to "shehui_new",
                "体育" to "tiyu_new",
                "科技" to "keji_new",
                "时尚" to "shichang_new",
                "汽车" to "qiche_new",
                "亲子" to "qinzi_new",
                "文化" to "wenhua_new",
                "旅游" to "lvyou_new",
                "秒懂" to "yunying_miaodong_new"
            ).mapIndexed { index, (title, tab) ->
                // 将每个分类转换为 VideoClassEntity 对象
                VideoClassEntity().apply {
                    this.title = title  // 分类标题
                    // 分类的 API URL，包含 tab 参数
                    this.url = "https://haokan.baidu.com/haokan/ui-web/video/rec?tab=$tab&act=pcFeed&pd=pc&num=40"
                    this.weight = index  // 权重，决定分类的排序位置
                }
            })
    }

    /**
     * 获取首页推荐
     * 当 isSupportHomeRecommends() 返回 true 时，主程序会调用此方法
     * 本插件不支持首页推荐，直接返回错误
     */
    override fun getHomeRecommends(
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (HomeRecommendsEntity) -> Unit
    ) {
        onError(Exception("Not support"))
    }

    /**
     * 获取视频详情（通过搜索结果实体）
     * 当用户点击搜索结果时，主程序会调用此方法获取详细信息
     *
     * @param result 搜索结果实体，包含视频的基本信息
     * @param requestConfigs 请求配置函数
     * @param onError 错误回调
     * @param onSuccess 成功回调，返回详细的视频信息
     */
    override fun getDetail(
        result: VideoSearchResultEntity,
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (VideoDetailEntity) -> Unit
    ) {
        // 调用实际的详情获取方法
        doGetDetail(result.detailPageUrl, requestConfigs, onError, onSuccess)
    }

    /**
     * 获取视频详情（通过详情页 URL）
     * 当用户直接访问某个 URL 时，主程序会调用此方法
     *
     * @param detailPageUrl 视频详情页 URL
     * @param requestConfigs 请求配置函数
     * @param onError 错误回调
     * @param onSuccess 成功回调
     */
    override fun getDetail(
        detailPageUrl: String,
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (VideoDetailEntity) -> Unit
    ) {
        return doGetDetail(detailPageUrl, requestConfigs, onError, onSuccess)
    }

    /**
     * 获取分类下的视频列表
     * 当用户点击某个分类时，主程序会调用此方法获取该分类下的视频
     *
     * @param clazz 分类实体，包含分类的 URL
     * @param page 页码（从 0 开始）
     * @param requestConfigs 请求配置函数
     * @param onError 错误回调
     * @param onSuccess 成功回调，返回视频列表
     */
    override fun getVideoClassList(
        clazz: VideoClassEntity,
        page: Int,
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (BaseClassResultEntity) -> Unit
    ) {
        OkHttpClientUtil.enqueue<JSONObject> {
            // 生成 API 所需的时间戳和 Token
            val hkTimestamp = System.currentTimeMillis().toString().substring(0, 10)
            val hkToken = Base64.getEncoder().encodeToString(hkTimestamp.toByteArray())

            // 构建完整的 API URL，包含各种签名参数
            val clazzUrl =
                clazz.url + "&act=pcFeed&shuaxin_id=$shuaXinId&hk_timestamp=$hkTimestamp&hk_nonce=e22794e40db8939d1e72e40315eaac8a&hk_sign=aa30903e0f3ca0fd1575ebb907fb01d7&hk_token=$hkToken"
            url(clazzUrl)

            // 设置 User-Agent
            header("User-Agent", userAgent)

            // 设置 Cookie（如果用户配置了的话）
            val cookie = setCookiePreference.getText()
            if (cookie.isNotEmpty()) {
                header("Cookie", cookie)
            } else {
                // 使用默认 Cookie
                header("Cookie", "BAIDUID=F31640221C92BA5E705FF4681C711944:FG=1;")
            }

            // 应用主程序的请求配置
            apply(requestConfigs)

            // 覆盖主程序的缓存模式，强制使用网络数据，保证列表实时更新
            setCacheMode(CacheMode.NETWORK_ELSE_CACHE)
            // 设置缓存 Key（使用 URL+ 页码的 MD5）
            setCacheKey((clazzUrl + page).toMd5String())
            // 缓存永久有效
            setCacheValidDuration(CacheValidDuration.FOREVER)

            onError {
                onError(it)
            }

            onSuccess { obj ->
                // 解析 JSON 响应，构建结果实体
                onSuccess(BaseClassResultEntity(hasMore = true, buildList {
                    // 从 JSON 中提取 videos 数组
                    // 数据结构：data -> response -> videos
                    val videos = obj.getJSONObject("data").getJSONObject("response").getJSONArray("videos")
                    for (i in 0 until videos.length()) {
                        val video = videos.getJSONObject(i)
                        add(VideoClassResultEntity().apply {
                            // 视频标题
                            title = video.getString("title")
                            // 副标题：作者 · 发布时间
                            subtitle = buildString {
                                append(video.getString("source_name"))
                                append(" · ")
                                append(video.getString("publish_time"))
                            }

                            // 封面图片 URL
                            cover = video.getString("poster_big")
                            // 视频时长（格式：MM:SS）
                            duration = video.getString("duration")
                            // 详情页 URL，用于后续获取详细信息
                            detailPageUrl = "https://haokan.baidu.com/v?vid=${video.getString("id")}&_format=json"

                            // 来源插件 ID 和名称（必需字段）
                            sourceId = this@HaoKanEngine.id
                            sourceName = this@HaoKanEngine.name
                        })
                    }
                }))
            }
        }
    }

    /**
     * 搜索功能
     * 当用户搜索关键词时，主程序会调用此方法
     *
     * @param keywords 搜索关键词
     * @param page 页码（从 1 开始）
     * @param requestConfigs 请求配置函数
     * @param onError 错误回调
     * @param onSuccess 成功回调，返回搜索结果
     */
    override fun search(
        keywords: String,
        page: Int,
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (BaseSearchResultEntity) -> Unit
    ) {
        // 每页返回 10 条结果
        val rn = 10
        // 当前时间戳（毫秒）
        val timestamp = System.currentTimeMillis().toString()
        // URL 编码关键词
        val encodedKeywords = URLEncoder.encode(keywords, "UTF-8")

        // 构建签名字符串
        // 格式：页码_关键词_每页数量_时间戳_版本
        val searchParams = buildString {
            append(page)
            append("_")
            append(encodedKeywords)
            append("_")
            append(rn)
            append("_")
            append(timestamp)
            append("_")
            append(1)
        }
        // 计算签名（MD5）
        val sign = searchParams.toMd5String()

        // 构建搜索 API URL
        val url = buildString {
            append("https://haokan.baidu.com/haokan/ui-search/pc/search/video?")
            append("pn=$page&rn=$rn&type=video&query=$encodedKeywords&sign=$sign&version=1&timestamp=$timestamp")
        }

        // 获取用户配置的 Cookie
        val cookie = setCookiePreference.getText()

        // 发起网络请求
        OkHttpClientUtil.enqueue<JSONObject> {
            url(url)
            header("User-Agent", userAgent)
            if (cookie.isNotEmpty()) {
                header("Cookie", cookie)
            } else {
                header("Cookie", "BAIDUID=F31640221C92BA5E705FF4681C711944:FG=1;")
            }
            apply(requestConfigs)
            onError {
                onError(it)
            }
            onSuccess {
                // 解析响应数据
                val data = it.getJSONObject("data")
                val list = data.optJSONArray("list")

                if (list == null) {
                    // 没有搜索结果
                    onSuccess(
                        BaseSearchResultEntity(
                            hasMore = false, value = emptyList()
                        )
                    )
                } else {
                    // 有结果，解析列表数据
                    onSuccess(BaseSearchResultEntity(hasMore = data.getInt("has_more") == 1, value = buildList {
                        for (i in 0 until list.length()) {
                            val video = list.getJSONObject(i)
                            add(VideoSearchResultEntity().apply {
                                // 封面图片
                                cover = video.getString("cover_src")
                                // 视频标题
                                title = video.getString("title")
                                // 视频时长
                                duration = video.getString("duration") //03:47

                                // 副标题：作者 / 播放量 / 发布时间
                                subtitle = buildList {
                                    add(video.getString("author"))
                                    add(video.getString("read_num")) //25 次播放
                                    add(video.getString("publishTimeText")) //1 天前
                                }.joinToString(" / ")

                                // 详情页 URL
                                detailPageUrl = "https://haokan.baidu.com/v?vid=${video.getString("vid")}&_format=json"
                                // 来源插件信息
                                sourceId = this@HaoKanEngine.id
                                sourceName = this@HaoKanEngine.name
                            })
                        }
                    }))
                }
            }
        }
    }

    /**
     * 内部方法：获取视频详情
     * 这是 getDetail 的实际实现，两个 getDetail 方法都委托到此方法
     *
     * @param detailPageUrl 详情页 URL
     * @param requestConfigs 请求配置函数
     * @param onError 错误回调
     * @param onSuccess 成功回调
     */
    private fun doGetDetail(
        detailPageUrl: String,
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (VideoDetailEntity) -> Unit
    ) {
        // 获取 Cookie
        val cookie = setCookiePreference.getText()

        // 请求视频详情 API
        OkHttpClientUtil.enqueue<JSONObject> {
            url(detailPageUrl)
            header("User-Agent", userAgent)
            if (cookie.isNotEmpty()) {
                header("Cookie", cookie)
            } else {
                header("Cookie", "BAIDUID=F31640221C92BA5E705FF4681C711944:FG=1;")
            }
            apply(requestConfigs)
            onError {
                onError(it)
            }
            onSuccess {
                // 解析 JSON 响应
                // 数据结构：data -> apiData -> curVideoMeta
                val apiData = it.getJSONObject("data").getJSONObject("apiData")
                val curVideoMeta = apiData.getJSONObject("curVideoMeta")

                // 构建详情实体
                onSuccess(VideoDetailEntity().apply {
                    // 视频 ID
                    id = curVideoMeta.optString("id")
                    // 标题
                    title = curVideoMeta.optString("title")
                    // 副标题：播放量 · 发布日期
                    subtitle = buildString {
                        append(curVideoMeta.optString("fmplaycnt"))
                        append(" · ")
                        append(curVideoMeta.optString("date"))
                    }

                    // 封面图片
                    cover = curVideoMeta.optString("poster")
                    // 描述信息（清理掉多余的广告文字）
                    description = apiData.optJSONObject("header").optString("description")
                        .replace(",好看视频是由百度团队打造的集内涵和颜值于一身的专业短视频聚合平台", "")
                    // 浏览器 URL（去掉 _format=json 参数）
                    browserUrl = detailPageUrl.removeSuffix("&_format=json")
                    // 原始 URL
                    url = detailPageUrl

                    // 添加播放渠道
                    channels.add(VideoDetailChannelEntity().apply {
                        id = detailPageUrl.toMd5String()
                        title = "播放地址"
                        weight = 0  // 权重，决定显示顺序
                        videoDataList.addAll(buildList {
                            // 获取清晰度列表
                            val clarityUrls = curVideoMeta.getJSONArray("clarityUrl")
                            // 取最后一个（通常是最高清晰度）
                            val clarityUrlObj = clarityUrls.getJSONObject(clarityUrls.length() - 1)
                            add(VideoDetailDataEntity().apply {
                                // 数据 ID（使用 URL+ 清晰度的 MD5）
                                id = (detailPageUrl + clarityUrlObj.optString("key")).toMd5String()
                                // 清晰度名称（如：超清、高清等）
                                title = clarityUrlObj.optString("title")
                                // 视频播放 URL
                                videoUrl = clarityUrlObj.optString("url")
                                // 视频类型：Direct 表示直接播放
                                videoType = VideoType.Direct
                                // 允许下载
                                canDownload = true
                            })
                        })
                    })

                    // 检查是否有合集信息
                    val collectionInfo = curVideoMeta.get("collection_info")
                    if (collectionInfo is JSONObject) {
                        // 有合集，添加合集渠道
                        channels.add(VideoDetailChannelEntity().apply {
                            id = collectionInfo.optString("collection_id")
                            title = collectionInfo.optString("title")
                            weight = 1  // 权重为 1，显示在播放地址后面
                            videoDataList.addAll(buildList {
                                // 获取合集中的视频列表
                                val videoInfo = collectionInfo.getJSONArray("video_info")
                                for (i in 0 until videoInfo.length()) {
                                    val video = videoInfo.getJSONObject(i)
                                    add(VideoDetailDataEntity().apply {
                                        // 视频 ID
                                        id = video.optString("vid")
                                        // 封面
                                        cover = video.optString("cover_src")
                                        // 标题
                                        title = video.optString("title")
                                        // 视频 URL（加上 vid:前缀表示需要解码）
                                        videoUrl = "vid:" + video.optString("vid")
                                        // 类型：Decode 表示需要解码处理
                                        videoType = VideoType.Decode
                                        // 允许下载
                                        canDownload = true
                                    })
                                }
                            })
                        })
                    }

                    // 设置来源插件信息
                    sourceId = this@HaoKanEngine.id
                    sourceName = this@HaoKanEngine.name
                })
            }
        }

        // 如果用户配置了 Cookie，额外发送阅读记录请求（增加视频播放量）
        if (cookie.isNotEmpty()) {
            val vid = detailPageUrl.substringAfter("v?vid=").removeSuffix("&_format=json")
            OkHttpClientUtil.enqueue<String> {
                url("https://haokan.baidu.com/haokan/ui-web/video/read?vid=$vid")
                header("Cookie", cookie)
                // 此请求不需要处理响应，只是触发一下
            }
        }
    }

    /**
     * 获取播放地址
     * 当用户点击播放时，主程序会调用此方法获取实际的视频流地址
     *
     * @param dataItem 视频数据项，包含之前存储的 videoUrl
     * @param requestConfigs 请求配置函数
     * @param onError 错误回调
     * @param onSuccess 成功回调，返回播放地址
     */
    override fun getPlayUrl(
        dataItem: VideoDetailDataEntity,
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (PlayUrlEntity) -> Unit
    ) {
        // 从 videoUrl 中提取 vid（格式：vid:xxxxx）
        val vid = dataItem.videoUrl.substringAfter("vid:")

        // 请求视频详情获取播放地址
        OkHttpClientUtil.enqueue<JSONObject> {
            url("https://haokan.baidu.com/v?vid=$vid&_format=json")
            // 使用缓存，避免重复请求
            setCacheMode(CacheMode.NETWORK_ELSE_CACHE)
            setCacheValidDuration(CacheValidDuration.FOREVER)
            header("User-Agent", userAgent)

            val cookie = setCookiePreference.getText()
            if (cookie.isNotEmpty()) {
                header("Cookie", cookie)
            } else {
                header("Cookie", "BAIDUID=F31640221C92BA5E705FF4681C711944:FG=1;")
            }
            apply(requestConfigs)
            onError {
                onError(it)
            }
            onSuccess {
                var url = ""
                val data = it.getJSONObject("data")
                val apiData = data.getJSONObject("apiData")
                val curVideoMeta = apiData.getJSONObject("curVideoMeta")
                val clarityUrls = curVideoMeta.getJSONArray("clarityUrl")

                // 遍历清晰度列表，选择合适的清晰度
                for (i in 0 until clarityUrls.length()) {
                    val clarityUrlObj = clarityUrls.getJSONObject(i)
                    val clarityUrlRank = clarityUrlObj.getInt("rank")
                    url = clarityUrlObj.getString("url")

                    // rank=2 表示超清，找到就停止
                    if (clarityUrlRank == 2) {
                        break
                    }
                }

                if (url.isBlank()) {
                    // 未找到播放地址
                    onError(Exception("Not found url!"))
                } else {
                    //创建一个测试弹幕文件，具体根据需求进行修改
                    val danmakuFile =
                        DanmakuFile(context.cacheDir.resolve("danmaku_${System.currentTimeMillis()}.txt")).apply {
                            appendDanmaku(
                                DanmakuEntity(
                                    text = "弹幕测试",
                                    time = 1000,
                                    type = DanmakuType.TYPE_SCROLL_RL,
                                    color = "#FFFFFFFF"
                                )
                            )
                            appendDanmaku(
                                DanmakuEntity(
                                    text = "弹幕测试1",
                                    time = 2000,
                                    type = DanmakuType.TYPE_SCROLL_RL,
                                    color = "#FFFFFFFF"
                                )
                            )
                            appendDanmaku(
                                DanmakuEntity(
                                    text = "弹幕测试2",
                                    time = 10000,
                                    type = DanmakuType.TYPE_SCROLL_RL,
                                    color = "#FFFFFFFF"
                                )
                            )
                            flush()
                            close()
                        }

                    // 返回播放地址
                    onSuccess(
                        PlayUrlEntity(
                            url = url, headers = hashMapOf("User-Agent" to userAgent), danmakuFile = danmakuFile
                        )
                    )
                }
            }
        }
    }

    /**
     * 获取下载地址
     * 当用户需要下载视频时，主程序会调用此方法
     * 本插件直接使用播放地址作为下载地址
     *
     * @param dataItem 视频数据项
     * @param requestConfigs 请求配置函数
     * @param onError 错误回调
     * @param onSuccess 成功回调，返回下载地址
     */
    override fun getDownloadUrl(
        dataItem: VideoDetailDataEntity,
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (DownloadUrlEntity) -> Unit
    ) {
        // 复用 getPlayUrl 方法获取播放地址
        getPlayUrl(dataItem, requestConfigs, onError = onError, onSuccess = {
            // 将播放地址作为下载地址返回
            onSuccess(DownloadUrlEntity(url = it.url))
        })
    }
}
