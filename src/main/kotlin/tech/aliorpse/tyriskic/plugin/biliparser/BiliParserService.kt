package tech.aliorpse.tyriskic.plugin.biliparser

import io.ktor.client.call.body
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess
import tech.aliorpse.tyriskic.plugin.biliparser.entity.BiliResponse
import tech.aliorpse.tyriskic.plugin.biliparser.entity.BiliVideo
import tech.aliorpse.tyriskic.plugin.biliparser.entity.BiliVideoSourceResponse
import tech.aliorpse.tyriskic.util.HttpClientProvider.httpClient

class BiliParserService {
    suspend fun getVideoInfo(bvid: String): BiliResponse<BiliVideo> =
        httpClient.get("https://api.bilibili.com/x/web-interface/view?bvid=$bvid").body()

    suspend fun getBVFromShortLink(link: String): BiliShortLinkParseResult {
        val response = httpClient.get(link)

        if (response.status != HttpStatusCode.Found) return BiliShortLinkParseResult.StatusCodeMismatch

        val bvMatch = bvRegex.find(response.headers["Location"] ?: "")?.value
        return if (bvMatch == null) {
            BiliShortLinkParseResult.NotAVideo
        } else {
            BiliShortLinkParseResult.Success(bvMatch)
        }
    }

    @Suppress("MagicNumber")
    suspend fun downloadVideo(data: BiliVideo): ByteArray? {
        val response: BiliResponse<BiliVideoSourceResponse> =
            httpClient.get("https://api.bilibili.com/x/player/playurl") {
                parameter("bvid", data.bvid)
                parameter("cid", data.cid)
                parameter("qn", 64)
                parameter("fnval", 0)
            }.body()

        val source = response.data.downloadUrl?.first() ?: return null

        if (source.size > 40 * 1024 * 1024) return null

        val availableUrls = sequence {
            yield(source.url)
            source.backupUrl?.forEach { yield(it) }
        }

        for (url in availableUrls) {
            val fileResponse = httpClient.get(url) {
                header("Referer", "https://www.bilibili.com/video/${data.bvid}")

                timeout {
                    socketTimeoutMillis = 5000
                    requestTimeoutMillis = 60000
                }
            }

            if (fileResponse.status.isSuccess()) {
                return fileResponse.body<ByteArray>()
            }
        }

        return null
    }
}

sealed class BiliShortLinkParseResult {
    class Success(val bvid: String) : BiliShortLinkParseResult()
    object StatusCodeMismatch : BiliShortLinkParseResult()
    object NotAVideo : BiliShortLinkParseResult()
}
