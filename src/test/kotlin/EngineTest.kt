import com.jason.network.cache.CacheMode
import com.sun.xml.internal.ws.api.pipe.Engine
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.suspendCancellableCoroutine
import utils.JarSourceContextTestImpl
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.test.Test

class EngineTest {
    val engine = HaoKanEngine(JarSourceContextTestImpl("HaoKanEngine"))

    @Test
    fun testGetHomeRecommends() {
        runBlocking { //因为getHomeRecommends是异步的，所以需要用suspendCoroutine转主线程测试
            suspendCancellableCoroutine { continuation ->
                println("正在获取首页推荐...")
                engine.getHomeRecommends(requestConfigs = {
                    //优先读取缓存，如果缓存不存在则从网络获取
                    setCacheMode(CacheMode.CACHE_ELSE_NETWORK)
                }, onError = {
                    continuation.resume(Result.failure(it))
                }, onSuccess = {
                    continuation.resume(Result.success(it))
                })
            }.onFailure {
                it.printStackTrace()
            }.onSuccess {
                it.banners.forEach { banner ->
                    println("banner:")
                    println("image: ${banner.image}")
                    println("title: ${banner.title}")
                    println("subtitle: ${banner.subtitle}")
                    println("data: ${banner.data}")
                    println("dataType: ${banner.dataType}")
                    println("---------------------------")
                }

                println("")
                it.groups.forEach { group ->
                    println("group: ${group.title}")
                    group.children.forEach { item ->
                        println("children: ${item.title}")
                        println("children: ${item.image}")
                        println("children: ${item.data}")
                        println("children: ${item.dataType}")
                        println()
                    }
                    println("---------------------------")
                }
            }
        }
    }

    @Test
    fun testSearch() {
        runBlocking { //因为search是异步的，所以需要用suspendCoroutine转主线程测试
            suspendCancellableCoroutine { continuation ->
                println("正在搜索...")
                engine.search(keywords = "钢铁侠", page = 1, requestConfigs = {
                    //优先读取缓存，如果缓存不存在则从网络获取
                    setCacheMode(CacheMode.CACHE_ELSE_NETWORK)
                }, onError = {
                    continuation.resume(Result.failure(it))
                }, onSuccess = {
                    continuation.resume(Result.success(it))
                })
            }.onFailure {
                it.printStackTrace()
            }.onSuccess {
                println("搜索结果：")
                println("hasMore: ${it.hasMore}")
                println("---------------------------")
                it.value.forEach { item ->
                    println("image: ${item.cover}")
                    println("title: ${item.title}")
                    println("subtitle: ${item.subtitle}")
                    println("updateInfo: ${item.updateInfo}")
                    println("detailPageUrl: ${item.detailPageUrl}")
                    println("detailPageUrlType: ${item.detailPageUrlType}")
                    println("---------------------------")
                }
            }
        }
    }

    @Test
    fun testGetDetail() {
        runBlocking { //因为getDetail是异步的，所以需要用suspendCoroutine转主线程测试
            suspendCancellableCoroutine { continuation ->
                println("正在获取视频详情...")
                //详情页url生成参考 [src/main/kotlin/HaoKanEngine.kt:468]
                engine.getDetail(detailPageUrl = "https://haokan.baidu.com/v?vid=7676742717481779479&_format=json", requestConfigs = {
                    //优先读取缓存，如果缓存不存在则从网络获取
                    setCacheMode(CacheMode.CACHE_ELSE_NETWORK)
                }, onError = {
                    continuation.resume(Result.failure(it))
                }, onSuccess = {
                    continuation.resume(Result.success(it))
                })
            }.onFailure {
                it.printStackTrace()
            }.onSuccess {
                println("详情页结果：")
                println("详情: ${it.toJSONObject().toString(2)}")
            }
        }
    }
}