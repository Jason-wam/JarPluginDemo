package utils

import java.io.File
import java.util.Properties

/**
 * 仅测试使用，请勿使用于正式环境
 */
class DataSaveForDesktop(val file: File) {
    private val properties = Properties()

    init {
        if (file.exists()) {
            file.inputStream().use {
                properties.loadFromXML(it)
            }
        } else {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
    }

    fun putString(key: String, value: String) {
        properties.setProperty(key, value)
    }

    fun getString(key: String, def: String): String {
        return properties.getProperty(key, def)
    }

    fun remove(key: String) {
        properties.remove(key)
    }

    fun clear() {
        properties.clear()
    }

    fun flush() {
        file.outputStream().use {
            properties.storeToXML(it, null)
        }
    }
}