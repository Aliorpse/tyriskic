package tech.aliorpse.tyriskic.plugin.biliparser.entity

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BiliResponse<T>(
    val code: Int,
    val message: String,
    val ttl: Int,
    val data: T
)

@Serializable
data class BiliVideo(
    @SerialName("pic")
    val picture: String,
    @SerialName("stat")
    val statistics: BiliVideoStatistics,
    @SerialName("owner")
    val uploader: BiliVideoUploader,
    @SerialName("desc")
    val description: String,
    @SerialName("honor_reply")
    val honorReply: BiliHonorReply,
    val title: String,
    val bvid: String,
    val cid: Long,
    val aid: Long
)

@Serializable
data class BiliVideoStatistics(
    val view: Int,
    val danmaku: Int,
    val reply: Int,
    val favorite: Int,
    val coin: Int,
    val share: Int,
    val like: Int
)

@Serializable
data class BiliVideoUploader(
    @SerialName("mid")
    val id: Long,
    @SerialName("face")
    val picture: String,
    val name: String
)

@Serializable
data class BiliHonorReply(
    @SerialName("honor")
    val honors: List<BiliHonor>? = null
)

@Serializable
data class BiliHonor(
    @SerialName("desc")
    val description: String,
    val type: Int
)

@Serializable
data class BiliVideoSourceResponse(
    @SerialName("durl")
    val downloadUrl: List<BiliVideoSource>? = null
)

@Serializable
data class BiliVideoSource(
    val url: String,
    @SerialName("backup_url")
    val backupUrl: List<String>? = null
)
