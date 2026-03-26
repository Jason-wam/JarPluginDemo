package com.jason.searcher.preference

/**
 * 供 JarSource 使用的配置
 */
open class BasePreference {
    open fun isEnabled(): Boolean {
        return true
    }
}