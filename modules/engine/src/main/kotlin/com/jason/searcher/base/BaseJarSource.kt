package com.jason.searcher.base

import com.jason.network.request.UrlRequest
import com.jason.searcher.entities.*
import com.jason.searcher.preference.BasePreference
import com.jason.searcher.preference.CheckboxPreference
import com.jason.searcher.utils.JarSourceContext

abstract class BaseJarSource(context: JarSourceContext) : Source(context) {
    /**
     * 当VideoType为VideoType.Decode时才会调用此方法进行解析
     * @param requestConfigs 主程序请求配置，实际使用中apply到请求中
     */
    abstract fun getPlayUrl(
        dataItem: VideoDetailDataEntity,
        requestConfigs: UrlRequest<*>.() -> Unit = {},
        onError: (Throwable) -> Unit,
        onSuccess: (PlayUrlEntity) -> Unit
    )

    abstract fun getDownloadUrl(
        dataItem: VideoDetailDataEntity,
        requestConfigs: UrlRequest<*>.() -> Unit = {},
        onError: (Throwable) -> Unit,
        onSuccess: (DownloadUrlEntity) -> Unit
    )
}