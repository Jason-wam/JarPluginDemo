package utils

import com.jason.searcher.utils.JarSourceContext
import java.io.File

/**
 * 仅测试使用，请勿使用于正式环境
 */
class JarSourceContextTestImpl(id: String) : JarSourceContext() {
    private val desktop = File("C:\\Users\\Administrator\\Desktop")
    private val preference by lazy {
        DataSaveForDesktop(desktop.resolve("cache").resolve(id).resolve("preference.xml"))
    }

    init {
        cacheDir = desktop.resolve("cache").resolve(id).apply {
            if (!exists()) {
                mkdirs()
            }
        }
    }

    override fun putInt(key: String, value: Int) {
        preference.putString(key, value.toString())
        preference.flush()
    }

    override fun getInt(key: String, def: Int): Int {
        return preference.getString(key, def.toString()).toInt()
    }

    override fun putString(key: String, value: String) {
        preference.putString(key, value)
        preference.flush()
    }

    override fun getString(key: String, def: String): String {
        return preference.getString(key, def)
    }

    override fun putLong(key: String, value: Long) {
        preference.putString(key, value.toString())
        preference.flush()
    }

    override fun getLong(key: String, def: Long): Long {
        return preference.getString(key, def.toString()).toLong()
    }

    override fun putFloat(key: String, value: Float) {
        preference.putString(key, value.toString())
        preference.flush()
    }

    override fun getFloat(key: String, def: Float): Float {
        return preference.getString(key, def.toString()).toFloat()
    }

    override fun putBoolean(key: String, value: Boolean) {
        preference.putString(key, value.toString())
        preference.flush()
    }

    override fun getBoolean(key: String, def: Boolean): Boolean {
        return preference.getString(key, def.toString()).toBoolean()
    }

    override fun putStringSet(key: String, value: Set<String>) {
        preference.putString(key, value.joinToString(","))
    }

    override fun getStringSet(key: String, def: Set<String>): Set<String> {
        return preference.getString(key, def.joinToString(",")).split(",").toSet()
    }

    override fun remove(key: String) {
        preference.remove(key)
        preference.flush()
    }

    override fun clear() {
        preference.clear()
        preference.flush()
    }

    override fun showToast(text: String) {
        println("showToast: $text")
    }

    override fun getDeviceName(): String {
        return "IDEA"
    }

    override fun getApplicationVersionCode(): Long {
        return 1
    }

    override fun getApplicationVersionName(): String {
        return "1.0.0"
    }
}