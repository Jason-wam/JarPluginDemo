package com.jason.network.utils

import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Protocol
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Proxy

internal object CallManager {
    private val calls by lazy { ArrayList<Call>() }

    fun cancel(call: Call) {
        call.cancel()
        synchronized(calls) {
            OkhttpLogger.i("OkHttpClient", "cancel call: ${call.request()}")
            calls.remove(call)
        }
    }

    fun cancelByTag(tag: Any) {
        synchronized(calls) {
            val cancellations = calls.filter { it.request().tag() == tag }
            calls.removeAll(cancellations.onEach {
                it.cancel()
                OkhttpLogger.i("OkHttpClient", "cancel call: ${it.request()}")
            }.toSet())
        }
    }

    fun cancelAll() {
        synchronized(calls) {
            calls.forEach { it.cancel() }
            calls.clear()
            OkhttpLogger.i("OkHttpClient", "cancel all calls.")
        }
    }

    fun bind(client: OkHttpClient.Builder) {
        client.eventListener(object : EventListener() {
            override fun callStart(call: Call) {
                super.callStart(call)
                OkhttpLogger.i("OkHttpClient", "callStart: ${call.request()}")
                synchronized(calls) {
                    calls.add(call)
                }
            }

            override fun callFailed(call: Call, ioe: IOException) {
                super.callFailed(call, ioe)
                OkhttpLogger.e("OkHttpClient", "callFailed: ${call.request()} , reason: $ioe")
                synchronized(calls) {
                    calls.remove(call)
                }
            }

            override fun callEnd(call: Call) {
                super.callEnd(call)
                OkhttpLogger.i("OkHttpClient", "callEnd: ${call.request()}")
                synchronized(calls) {
                    calls.remove(call)
                }
            }

            override fun canceled(call: Call) {
                super.canceled(call)
                OkhttpLogger.i("OkHttpClient", "canceled: ${call.request()}")
                synchronized(calls) {
                    calls.remove(call)
                }
            }

            override fun connectFailed(
                call: Call, inetSocketAddress: InetSocketAddress, proxy: Proxy, protocol: Protocol?, ioe: IOException
            ) {
                super.connectFailed(call, inetSocketAddress, proxy, protocol, ioe)
                OkhttpLogger.e("OkHttpClient", "connectFailed: ${call.request()} , reason: $ioe")
                synchronized(calls) {
                    calls.remove(call)
                }
            }

            override fun requestFailed(call: Call, ioe: IOException) {
                super.requestFailed(call, ioe)
                OkhttpLogger.e("OkHttpClient", "requestFailed: ${call.request()} , reason: $ioe")
                synchronized(calls) {
                    calls.remove(call)
                }
            }

            override fun responseFailed(call: Call, ioe: IOException) {
                super.responseFailed(call, ioe)
                OkhttpLogger.e("OkHttpClient", "responseFailed: ${call.request()} , reason: $ioe")
                synchronized(calls) {
                    calls.remove(call)
                }
            }
        })
    }
}