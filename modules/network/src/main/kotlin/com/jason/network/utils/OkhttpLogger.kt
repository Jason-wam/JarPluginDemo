package com.jason.network.utils

object OkhttpLogger {
    var enabled = true

    fun e(tag: String, msg: String) {
        if (enabled) {
            System.err.println("$tag: $msg")
        }
    }

    fun i(tag: String, msg: String) {
        if (enabled) {
            println("$tag: $msg")
        }
    }
}