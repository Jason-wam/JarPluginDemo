package com.jason.network.utils

import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

object UnsafeSSLSocketFactory {
    fun sslSocketFactory(): SSLSocketFactory? {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(x509TrustManager)
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            sslContext.socketFactory
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun sslContext(): SSLContext? {
        return try {
            val trustAllCerts = arrayOf<TrustManager>(x509TrustManager)
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, SecureRandom())
            sslContext
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    val x509TrustManager: X509TrustManager by lazy {
        object : X509TrustManager {
            override fun checkClientTrusted(certs: Array<X509Certificate?>?, authType: String?) {
            }

            override fun checkServerTrusted(certs: Array<X509Certificate?>?, authType: String?) {
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> {
                return arrayOf()
            }
        }
    }
}