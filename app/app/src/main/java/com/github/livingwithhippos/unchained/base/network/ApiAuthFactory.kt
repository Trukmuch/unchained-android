package com.github.livingwithhippos.unchained.base.network

import com.github.livingwithhippos.unchained.authentication.model.AuthenticationApi
import com.github.livingwithhippos.unchained.utilities.BASE_AUTH_URL
import com.github.livingwithhippos.unchained.utilities.OPEN_SOURCE_CLIENT_ID
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object ApiAuthFactory {

    //Creating Auth Interceptor to add client_id query in front of all the requests.
    private val authInterceptor = Interceptor { chain ->
        val newUrl = chain.request().url
            .newBuilder()
            .addQueryParameter("client_id", OPEN_SOURCE_CLIENT_ID)
            .build()

        val newRequest = chain.request()
            .newBuilder()
            .url(newUrl)
            .build()

        chain.proceed(newRequest)
    }

    //OkhttpClient for building http request url
    private val debridClient: OkHttpClient = OkHttpClient().newBuilder()
        .addInterceptor(authInterceptor)
        .build()


    fun retrofit(): Retrofit = Retrofit.Builder()
        .client(debridClient)
        .baseUrl(BASE_AUTH_URL)
        .addConverterFactory(MoshiConverterFactory.create())
        .build()


    val authApi: AuthenticationApi = ApiFactory.retrofit().create(
        AuthenticationApi::class.java
    )
}