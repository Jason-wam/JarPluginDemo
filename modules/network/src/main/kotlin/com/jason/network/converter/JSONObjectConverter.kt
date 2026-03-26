package com.jason.network.converter

import com.jason.network.error.ConvertException
import com.jason.network.extension.readString
import com.jason.network.request.BaseRequest
import okhttp3.Response
import org.json.JSONObject
import kotlin.reflect.KClass

class JSONObjectConverter : ResponseConverter<JSONObject>() {
    override fun supportType(): KClass<*> {
        return JSONObject::class
    }

    override fun convert(request: BaseRequest<JSONObject>, response: Response): JSONObject {
        return response.use {
            it.readString(request.charset)?.let { str -> JSONObject(str) }
                ?: throw ConvertException("Response body is null!")
        }
    }
}