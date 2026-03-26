package com.jason.network.converter

import com.jason.network.request.BaseRequest
import okhttp3.Response
import kotlin.reflect.KClass

class NoConverter : ResponseConverter<Response>() {
    override fun supportType(): KClass<*> {
        return Response::class
    }

    override fun convert(request: BaseRequest<Response>, response: Response): Response {
        return response
    }
}