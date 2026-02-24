package tech.aliorpse.tyriskic.plugin.biliparser

import org.ntqqrev.milky.Event
import org.ntqqrev.milky.IncomingMessage
import org.ntqqrev.milky.IncomingSegment
import org.ntqqrev.saltify.core.forward
import org.ntqqrev.saltify.core.image
import org.ntqqrev.saltify.core.node
import org.ntqqrev.saltify.core.reply
import org.ntqqrev.saltify.core.sendGroupMessageReaction
import org.ntqqrev.saltify.core.text
import org.ntqqrev.saltify.core.video
import org.ntqqrev.saltify.dsl.SaltifyPlugin
import kotlin.io.encoding.Base64

// 匹配 \? 是因为 LightApp 的 jsonPayload 会给键值的 / 前面加个 \
val shortLinkRegex = Regex("""(b23\.tv|bili2233\.cn)\\?/\w{7}""")
val bvRegex = Regex("""BV1\w{9}""")

private val service = BiliParserService()

val biliParser = SaltifyPlugin("BiliParser") {
    on<Event.MessageReceive> { e ->
        val text = e.data.segments
            .filterIsInstance<IncomingSegment.Text>()
            .joinToString(separator = "") { it.text }

        val lightApp = e.data.segments
            .filterIsInstance<IncomingSegment.LightApp>()
            .firstOrNull()

        val bvMatch = bvRegex.find(text)?.value
        val shortLinkMatch = shortLinkRegex.find(text + lightApp?.data?.jsonPayload)?.value
            ?.replace("\\", "")

        val bvid = when {
            bvMatch != null -> bvMatch
            shortLinkMatch != null -> runCatching {
                when (
                    val result = service.getBVFromShortLink("https://$shortLinkMatch")
                ) {
                    is BiliShortLinkParseResult.Success -> result.bvid
                    is BiliShortLinkParseResult.NotAVideo -> return@on
                    is BiliShortLinkParseResult.StatusCodeMismatch -> {
                        e.respond {
                            reply(e.data.messageSeq)
                            text("短链解析失败: HttpStatus 不匹配")
                        }
                        return@on
                    }
                }
            }.getOrElse {
                e.respond {
                    reply(e.data.messageSeq)
                    text("短链解析失败: ${it.message}")
                }
                return@on
            }

            else -> return@on
        }

        if (e.data is IncomingMessage.Group) {
            client.sendGroupMessageReaction(e.data.peerId, e.data.messageSeq, "60")
        }

        val response = service.getVideoInfo(bvid)
        if (response.code != 0) {
            e.respond {
                reply(e.data.messageSeq)
                text("视频信息获取失败: ${response.message}")
            }
            return@on
        }

        val source = service.downloadVideo(response.data)

        val data = response.data
        val statistics = data.statistics

        e.respond {
            forward {
                node(e.selfId, "Tyriskic") {
                    image(data.picture)
                    text(
                        """
                            ${data.title}
                            https://www.bilibili.com/video/$bvid
                            -----
                            播放: ${statistics.view.formatted} | 弹幕: ${statistics.danmaku.formatted}
                            点赞: ${statistics.like.formatted} | 投币: ${statistics.coin.formatted}
                            收藏: ${statistics.favorite.formatted} | 评论: ${statistics.reply.formatted}
                        """.trimIndent()
                    )
                }
                if (source != null) {
                    node(e.selfId, "Tyriskic") {
                        video("base64://${Base64.encode(source)}", data.picture)
                    }
                }
                node(e.selfId, "Tyriskic") {
                    text(
                        """
                            视频简介
                            -----
                            ${data.description}
                        """.trimIndent()
                    )
                }
                node(e.selfId, "Tyriskic") {
                    text(
                        """
                            UP主: ${data.uploader.name}
                            https://space.bilibili.com/${data.uploader.id}
                        """.trimIndent()
                    )
                }
            }
        }
    }
}

@Suppress("MagicNumber")
private val Int.formatted: String
    get() = when {
        this < 10000 -> toString()
        this < 100000000 -> {
            val res = this / 10000.0
            if (res % 1 == 0.0) "${res.toInt()}万" else "%.1f万".format(res)
        }
        else -> {
            val res = this / 100000000.0
            if (res % 1 == 0.0) "${res.toInt()}亿" else "%.1f亿".format(res)
        }
    }
