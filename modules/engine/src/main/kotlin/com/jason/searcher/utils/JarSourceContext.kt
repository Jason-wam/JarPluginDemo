package com.jason.searcher.utils

import java.io.File

abstract class JarSourceContext {
    lateinit var cacheDir: File
    private var onRequestShowDialog: ((params: DialogParams) -> Unit)? = null
    private var onRequestHideDialog: (() -> Unit)? = null
    private var onRequestShowQrcodeDialog: ((params: QrcodeDialogParams) -> Unit)? = null
    private var onRequestHideQrcodeDialog: (() -> Unit)? = null

    abstract fun putInt(key: String, value: Int)
    abstract fun getInt(key: String, def: Int): Int
    abstract fun putString(key: String, value: String)
    abstract fun getString(key: String, def: String): String
    abstract fun putLong(key: String, value: Long)
    abstract fun getLong(key: String, def: Long): Long
    abstract fun putFloat(key: String, value: Float)
    abstract fun getFloat(key: String, def: Float): Float
    abstract fun putBoolean(key: String, value: Boolean)
    abstract fun getBoolean(key: String, def: Boolean): Boolean
    abstract fun putStringSet(key: String, value: Set<String>)
    abstract fun getStringSet(key: String, def: Set<String>): Set<String>
    abstract fun remove(key: String)
    abstract fun clear()

    abstract fun showToast(text: String)


    data class DialogParams(
        val title: String,
        val message: String,
        val dismissButton: String,
        val confirmButton: String,
        val onDismiss: () -> Unit = {},
        val onConfirm: () -> Unit = {}
    )

    fun showDialog(
        title: String,
        message: String,
        dismissButton: String,
        confirmButton: String,
        onDismiss: () -> Unit = {},
        onConfirm: () -> Unit = {}
    ) {
        onRequestShowDialog?.invoke(
            DialogParams(title, message, dismissButton, confirmButton, onDismiss, onConfirm)
        )
    }

    fun onRequestHideDialog(onRequestHideDialog: () -> Unit) {
        this.onRequestHideDialog = onRequestHideDialog
    }

    fun hideDialog() {
        onRequestHideDialog?.invoke()
    }

    fun showQrcodeDialog(
        title: String, message: String, content: String, buttonText: String = "取消", onDismiss: () -> Unit = {}
    ) {
        onRequestShowQrcodeDialog?.invoke(QrcodeDialogParams(title, message, content, buttonText, onDismiss))
    }

    fun onRequestHideQrcodeDialog(onRequestHideQrcodeDialog: () -> Unit) {
        this.onRequestHideQrcodeDialog = onRequestHideQrcodeDialog
    }

    fun hideQrcodeDialog() {
        onRequestHideQrcodeDialog?.invoke()
    }

    fun onRequestShowDialog(onRequestShowDialog: ((dialogParams: DialogParams) -> Unit)?) {
        this.onRequestShowDialog = onRequestShowDialog
    }

    data class QrcodeDialogParams(
        val title: String, val message: String, val content: String, val buttonText: String, val onDismiss: () -> Unit
    )

    fun onRequestShowQrcodeDialog(onRequestShowQrcodeDialog: ((params: QrcodeDialogParams) -> Unit)?) {
        this.onRequestShowQrcodeDialog = onRequestShowQrcodeDialog
    }

    abstract fun getDeviceName(): String

    abstract fun getApplicationVersionCode(): Long

    abstract fun getApplicationVersionName(): String
}