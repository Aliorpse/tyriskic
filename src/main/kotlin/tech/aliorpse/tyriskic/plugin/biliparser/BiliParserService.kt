package tech.aliorpse.tyriskic.plugin.biliparser

import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import tech.aliorpse.tyriskic.plugin.biliparser.entity.BiliResponse
import tech.aliorpse.tyriskic.plugin.biliparser.entity.BiliVideo
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
}
sealed class BiliShortLinkParseResult {
    class Success(val bvid: String) : BiliShortLinkParseResult()
    object StatusCodeMismatch : BiliShortLinkParseResult()
    object NotAVideo : BiliShortLinkParseResult()
}
