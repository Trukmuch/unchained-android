package com.github.livingwithhippos.unchained.data.remote

import com.github.livingwithhippos.unchained.data.model.DownloadItem
import retrofit2.Response
import javax.inject.Inject

class DownloadApiHelperImpl @Inject constructor(private val downloadsApi: DownloadsApi) :
    DownloadApiHelper {

    override suspend fun getDownloads(
        token: String,
        offset: Int?,
        page: Int,
        limit: Int
    ): Response<List<DownloadItem>> =
        downloadsApi.getDownloads(token, offset, page, limit)

    override suspend fun deleteDownload(token: String, id: String) =
        downloadsApi.deleteDownload(token, id)
}