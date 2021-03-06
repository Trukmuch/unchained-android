package com.github.livingwithhippos.unchained.data.repositoy

import android.util.Log
import arrow.core.Either
import com.github.livingwithhippos.unchained.BuildConfig
import com.github.livingwithhippos.unchained.data.model.APIError
import com.github.livingwithhippos.unchained.data.model.ApiConversionError
import com.github.livingwithhippos.unchained.data.model.EmptyBodyError
import com.github.livingwithhippos.unchained.data.model.NetworkError
import com.github.livingwithhippos.unchained.data.model.NetworkResponse
import com.github.livingwithhippos.unchained.data.model.UnchainedNetworkException
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import retrofit2.Response
import java.io.IOException

/**
 * Base repository class to be extended by other repositories.
 * Manages the calls between retrofit and the actual repositories.
 */
open class BaseRepository {

    //todo: inject this
    private val jsonAdapter: JsonAdapter<APIError> = Moshi.Builder()
        .build()
        .adapter(APIError::class.java)

    suspend fun <T : Any> safeApiCall(call: suspend () -> Response<T>, errorMessage: String): T? {

        val result: NetworkResponse<T> = safeApiResult(call, errorMessage)
        var data: T? = null

        when (result) {
            is NetworkResponse.Success ->
                data = result.data
            is NetworkResponse.SuccessEmptyBody ->
                if (BuildConfig.DEBUG)
                    Log.d("BaseRepository", "Successful call with empty body : ${result.code}")
            is NetworkResponse.Error ->
                if (BuildConfig.DEBUG)
                    Log.d("BaseRepository", errorMessage)
        }

        return data
    }

    private suspend fun <T : Any> safeApiResult(
        call: suspend () -> Response<T>,
        errorMessage: String
    ): NetworkResponse<T> {
        val response = call.invoke()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null)
                return NetworkResponse.Success(body)
            else
                return NetworkResponse.SuccessEmptyBody(response.code())
        }

        return NetworkResponse.Error(IOException("Error Occurred while getting api result, error : $errorMessage"))
    }

    suspend fun <T : Any> eitherApiResult(
        call: suspend () -> Response<T>,
        errorMessage: String
    ): Either<UnchainedNetworkException, T> {
        val response = call.invoke()
        if (response.isSuccessful) {
            val body = response.body()
            return if (body != null)
                Either.Right(body)
            else
                Either.Left(EmptyBodyError(response.code()))
        } else {
            try {
                val error: APIError? = jsonAdapter.fromJson(response.errorBody()!!.string())
                return if (error != null)
                    Either.Left(error)
                else
                    Either.Left(ApiConversionError(-1))
            } catch (e: IOException) {
                // todo: analyze error to return code
                return Either.Left(NetworkError(-1, errorMessage))
            }
        }
    }
}