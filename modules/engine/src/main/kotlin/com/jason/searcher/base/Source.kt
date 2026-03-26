package com.jason.searcher.base

import com.jason.network.request.UrlRequest
import com.jason.searcher.entities.BaseClassResultEntity
import com.jason.searcher.entities.BaseSearchResultEntity
import com.jason.searcher.entities.VideoClassEntity
import com.jason.searcher.entities.VideoCoverRatio
import com.jason.searcher.entities.VideoDetailEntity
import com.jason.searcher.entities.VideoSearchResultEntity
import com.jason.searcher.entities.home.HomeRecommendsEntity
import com.jason.searcher.preference.BasePreference
import com.jason.searcher.preference.CheckboxPreference
import com.jason.searcher.utils.JarSourceContext
import java.io.Serializable

abstract class Source(open val context: JarSourceContext) : Serializable {
    var id: String = ""
    var name: String = ""
    var versionCode: Int = 0
    var versionName: String = ""

    var description: String = ""

    var weight: Int = 0

    var resultCoverRatio: VideoCoverRatio = VideoCoverRatio.Auto
    var detailPageCoverRatio: VideoCoverRatio = VideoCoverRatio.Auto
    var classResultCoverRatio: VideoCoverRatio = VideoCoverRatio.Auto

    protected var classEntities: ArrayList<VideoClassEntity> = arrayListOf()

    abstract fun getFaviconUrl(): String

    abstract fun isSupportClass(): Boolean

    abstract fun isSupportSearch(): Boolean

    abstract fun isSupportHomeRecommends(): Boolean

    private var onRequestRefreshUserViews: () -> Unit = {}

    protected fun refreshUserViews() {
        onRequestRefreshUserViews.invoke()
    }

    open fun onRequestRefreshUserViews(doUpdate: () -> Unit) {
        onRequestRefreshUserViews = doUpdate
    }

    //无痕模式
    open fun isIncognitoModeEnabled(): Boolean {
        return context.getBoolean("isIncognitoModeEnabled", false)
    }

    open fun isSearchEnabled(): Boolean {
        return context.getBoolean("search_enabled", true)
    }

    open fun isClassEnabled(): Boolean {
        return context.getBoolean("browse_enabled", true)
    }

    open fun isRecommendEnabled(): Boolean {
        return context.getBoolean("recommend_enabled", true)
    }

    fun getInternalPreferences() = buildList {
        add(object : CheckboxPreference(
            title = "无痕模式", "开启后将不会保留任何历史记录"
        ) {
            override fun isChecked(): Boolean {
                return context.getBoolean("isIncognitoModeEnabled", false)
            }

            override fun onCheckedStateChange(checked: Boolean) {
                context.putBoolean("isIncognitoModeEnabled", checked)
            }
        })

        if (isSupportSearch()) {
            add(object : CheckboxPreference(
                title = "启用搜索", "关闭后将不从此插件搜索内容"
            ) {
                override fun isChecked(): Boolean {
                    return context.getBoolean("search_enabled", true)
                }

                override fun onCheckedStateChange(checked: Boolean) {
                    context.putBoolean("search_enabled", checked)
                }
            })
        }

        if (isSupportHomeRecommends()) {
            add(object : CheckboxPreference(
                title = "启用推荐", "关闭后将不在首页显示此插件的内容"
            ) {
                override fun isChecked(): Boolean {
                    return context.getBoolean("recommend_enabled", true)
                }

                override fun onCheckedStateChange(checked: Boolean) {
                    context.putBoolean("recommend_enabled", checked)
                }
            })
        }

        if (isSupportClass()) {
            add(object : CheckboxPreference(
                title = "启用浏览", "关闭后将不在浏览页显示此插件的内容"
            ) {
                override fun isChecked(): Boolean {
                    return context.getBoolean("browse_enabled", true)
                }

                override fun onCheckedStateChange(checked: Boolean) {
                    context.putBoolean("browse_enabled", checked)
                }
            })
        }
    }

    open fun getPreferences(): List<BasePreference> {
        return emptyList()
    }

    fun addClassEntity(title: String, url: String) {
        classEntities.add(
            VideoClassEntity().apply {
                this.title = title
                this.url = url
            })
    }

    fun addClassEntity(entity: VideoClassEntity) {
        classEntities.add(entity)
    }

    fun addClassEntities(entities: List<VideoClassEntity>) {
        classEntities.addAll(entities)
    }

    fun addClassEntity(block: VideoClassEntity.() -> Unit) {
        classEntities.add(VideoClassEntity().apply(block))
    }

    /**
     * @param requestConfigs 主程序请求配置，实际使用中apply到请求中
     */
    abstract fun getClassEntities(
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (List<VideoClassEntity>) -> Unit
    )

    /**
     * @param requestConfigs 主程序请求配置，实际使用中apply到请求中
     */
    abstract fun getHomeRecommends(
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (HomeRecommendsEntity) -> Unit
    )

    /**
     * @param requestConfigs 主程序请求配置，实际使用中apply到请求中
     */
    abstract fun search(
        keywords: String,
        page: Int,
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (BaseSearchResultEntity) -> Unit
    )

    /**
     * @param requestConfigs 主程序请求配置，实际使用中apply到请求中
     */
    abstract fun getDetail(
        result: VideoSearchResultEntity,
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (VideoDetailEntity) -> Unit
    )

    /**
     * @param requestConfigs 主程序请求配置，实际使用中apply到请求中
     */
    abstract fun getDetail(
        detailPageUrl: String,
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (VideoDetailEntity) -> Unit
    )

    /**
     * @param requestConfigs 主程序请求配置，实际使用中apply到请求中
     */
    abstract fun getVideoClassList(
        clazz: VideoClassEntity,
        page: Int,
        requestConfigs: UrlRequest<*>.() -> Unit,
        onError: (Throwable) -> Unit,
        onSuccess: (BaseClassResultEntity) -> Unit
    )
}