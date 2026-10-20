package com.leekleak.trafficlight.integrations.iperf

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

@Serializable
data class StreamEvent(
    val event: String,
    val data: JsonElement
)

@Serializable
data class StartData(
    val connected: List<ConnectedSocket> = emptyList(),
    val version: String? = null,
    @SerialName("system_info") val systemInfo: String? = null,
    val timestamp: Timestamp? = null,
    @SerialName("connecting_to") val connectingTo: ConnectingTo? = null,
    @SerialName("accepted_connection") val acceptedConnection: AcceptedConnection? = null,
    val cookie: String? = null,
    @SerialName("tcp_mss_default") val tcpMssDefault: Int? = null,
    @SerialName("target_bitrate") val targetBitrate: Long? = null,
    @SerialName("fq_rate") val fqRate: Long? = null,
    @SerialName("sock_bufsize") val sockBufsize: Int? = null,
    @SerialName("sndbuf_actual") val sndbufActual: Int? = null,
    @SerialName("rcvbuf_actual") val rcvbufActual: Int? = null,
    @SerialName("test_start") val testStart: TestStart
)

@Serializable
data class ConnectedSocket(
    val socket: Int,
    @SerialName("local_host") val localHost: String,
    @SerialName("local_port") val localPort: Int,
    @SerialName("remote_host") val remoteHost: String,
    @SerialName("remote_port") val remotePort: Int
)

@Serializable
data class ConnectingTo(
    val host: String,
    val port: Int
)

@Serializable
data class AcceptedConnection(
    val host: String,
    val port: Int
)

@Serializable
data class Timestamp(
    val time: String,
    val timesecs: Long,
    val timemillisecs: Long? = null
)

@Serializable
data class TestStart(
    val protocol: String,
    @SerialName("num_streams") val numStreams: Int? = null,
    val blksize: Long? = null,
    val omit: Double? = null,
    val duration: Double? = null,
    val bytes: Long? = null,
    val blocks: Long? = null,
    val reverse: Int? = null,
    val tos: Int? = null,
    @SerialName("target_bitrate") val targetBitrate: Long? = null,
    val bidir: Int? = null,
    @SerialName("fqrate") val fqRate: Long? = null,
    val interval: Double? = null
)

@Serializable
data class IntervalData(
    val streams: List<IntervalStream>,
    val sum: SumStats? = null,
    @SerialName("sum_bidir_reverse") val sumBidirReverse: SumStats? = null
)

@Serializable
data class IntervalStream(
    val socket: Int,
    val start: Double,
    val end: Double,
    val seconds: Double,
    val bytes: Long,
    @SerialName("bits_per_second") val bitsPerSecond: Double,
    val omitted: Boolean,
    val sender: Boolean? = null,

    // UDP
    @SerialName("jitter_ms") val jitterMs: Double? = null,
    @SerialName("lost_packets") val lostPackets: Long? = null,
    @SerialName("lost_percent") val lostPercent: Double? = null,
    val packets: Long? = null,

    // TCP
    val retransmits: Long? = null,
    @SerialName("snd_cwnd") val sndCwnd: Long? = null,
    @SerialName("snd_wnd") val sndWnd: Long? = null,
    val rtt: Long? = null,
    val rttvar: Long? = null,
    val pmtu: Int? = null,
    val reorder: Long? = null
)

@Serializable
data class SumStats(
    val start: Double,
    val end: Double,
    val seconds: Double,
    val bytes: Long,
    @SerialName("bits_per_second") val bitsPerSecond: Double,
    val omitted: Boolean? = null,
    val sender: Boolean? = null,

    // TCP
    val retransmits: Long? = null,

    // UDP
    @SerialName("jitter_ms") val jitterMs: Double? = null,
    @SerialName("lost_packets") val lostPackets: Long? = null,
    @SerialName("lost_percent") val lostPercent: Double? = null,
    val packets: Long? = null
)

@Serializable
data class EndData(
    val streams: List<EndStream> = emptyList(),

    // TCP
    @SerialName("sum_sent") val sumSent: SumStats? = null,
    @SerialName("sum_received") val sumReceived: SumStats? = null,

    // UDP
    val sum: SumStats? = null,

    @SerialName("sum_sent_bidir_reverse") val sumSentBidirReverse: SumStats? = null,
    @SerialName("sum_received_bidir_reverse") val sumReceivedBidirReverse: SumStats? = null,

    @SerialName("cpu_utilization_percent") val cpuUtilizationPercent: CpuUtilization? = null,
    @SerialName("sender_tcp_congestion") val senderTcpCongestion: String? = null,
    @SerialName("receiver_tcp_congestion") val receiverTcpCongestion: String? = null
)

/**
 * TCP streams have `sender` + `receiver`, UDP streams have `udp`.
 */
@Serializable
data class EndStream(
    val sender: EndStreamStats? = null,
    val receiver: EndStreamStats? = null,
    val udp: EndStreamStats? = null
)

@Serializable
data class EndStreamStats(
    val socket: Int,
    val start: Double,
    val end: Double,
    val seconds: Double,
    val bytes: Long,
    @SerialName("bits_per_second") val bitsPerSecond: Double,
    val sender: Boolean? = null,

    // TCP sender only
    val retransmits: Long? = null,
    val reorder: Long? = null,
    @SerialName("max_snd_cwnd") val maxSndCwnd: Long? = null,
    @SerialName("max_snd_wnd") val maxSndWnd: Long? = null,
    @SerialName("max_rtt") val maxRtt: Long? = null,
    @SerialName("min_rtt") val minRtt: Long? = null,
    @SerialName("mean_rtt") val meanRtt: Long? = null,

    // UDP only
    @SerialName("jitter_ms") val jitterMs: Double? = null,
    @SerialName("lost_packets") val lostPackets: Long? = null,
    @SerialName("lost_percent") val lostPercent: Double? = null,
    @SerialName("out_of_order") val outOfOrder: Long? = null,
    val packets: Long? = null
)

@Serializable
data class CpuUtilization(
    @SerialName("host_total") val hostTotal: Double,
    @SerialName("host_user") val hostUser: Double,
    @SerialName("host_system") val hostSystem: Double,
    @SerialName("remote_total") val remoteTotal: Double,
    @SerialName("remote_user") val remoteUser: Double,
    @SerialName("remote_system") val remoteSystem: Double
)

sealed interface IperfEvent

data class StartEvent(val data: StartData) : IperfEvent

data class IntervalEvent(
    val data: IntervalData,
    val results: List<IntervalResult>
) : IperfEvent

data class EndEvent(val data: EndData) : IperfEvent

data class ErrorEvent(val message: String) : IperfEvent

data class ServerOutputTextEvent(val text: String) : IperfEvent

data class ServerOutputJsonEvent(val json: JsonObject) : IperfEvent

data class UnknownEvent(val event: String, val data: JsonElement) : IperfEvent

sealed interface IntervalResult {
    val protocol: String
    val socket: Int
    val bytesTransferred: Long
    val intervalStartOffset: Double
    val intervalEndOffset: Double
    val intervalDuration: Double
    val bitsPerSecond: Double
    val omitted: Boolean
}

data class TcpIntervalResult(
    override val protocol: String = "tcp",
    override val socket: Int,
    override val bytesTransferred: Long,
    override val intervalStartOffset: Double,
    override val intervalEndOffset: Double,
    override val intervalDuration: Double,
    override val bitsPerSecond: Double,
    override val omitted: Boolean,
    val retransmits: Long,
    val rtt: Long?
) : IntervalResult

data class UdpIntervalResult(
    override val protocol: String = "udp",
    override val socket: Int,
    override val bytesTransferred: Long,
    override val intervalStartOffset: Double,
    override val intervalEndOffset: Double,
    override val intervalDuration: Double,
    override val bitsPerSecond: Double,
    override val omitted: Boolean,
    val packetCount: Long,
    val jitterMs: Double,
    val lostPackets: Long,
    val lostPercent: Double
) : IntervalResult

class IntervalStreamParser {
    private val json = Json { ignoreUnknownKeys = true }
    private var protocol: String = "tcp"
    fun parseEvent(line: String): IperfEvent {
        val envelope = json.decodeFromString(StreamEvent.serializer(), line)
        val data = envelope.data

        return when (envelope.event) {
            "start" -> {
                val startData = json.decodeFromJsonElement(StartData.serializer(), data)
                protocol = startData.testStart.protocol.lowercase()
                StartEvent(startData)
            }
            "interval" -> {
                val intervalData = json.decodeFromJsonElement(IntervalData.serializer(), data)
                IntervalEvent(intervalData, intervalData.streams.map { toResult(it) })
            }
            "end" -> {
                EndEvent(json.decodeFromJsonElement(EndData.serializer(), data))
            }
            "error" -> {
                ErrorEvent((data as? JsonPrimitive)?.contentOrNull ?: data.toString())
            }
            "server_output_text" -> {
                ServerOutputTextEvent((data as? JsonPrimitive)?.contentOrNull ?: data.toString())
            }
            "server_output_json" -> {
                if (data is JsonObject) ServerOutputJsonEvent(data)
                else UnknownEvent(envelope.event, data)
            }
            else -> UnknownEvent(envelope.event, data)
        }
    }

    fun parseLine(line: String): List<IntervalResult> =
        (parseEvent(line) as? IntervalEvent)?.results ?: emptyList()

    private fun toResult(s: IntervalStream): IntervalResult =
        if (protocol == "udp") {
            UdpIntervalResult(
                socket = s.socket,
                bytesTransferred = s.bytes,
                intervalStartOffset = s.start,
                intervalEndOffset = s.end,
                intervalDuration = s.seconds,
                bitsPerSecond = s.bitsPerSecond,
                omitted = s.omitted,
                packetCount = s.packets ?: 0,
                jitterMs = s.jitterMs ?: 0.0,
                lostPackets = s.lostPackets ?: 0,
                lostPercent = s.lostPercent ?: 0.0
            )
        } else {
            TcpIntervalResult(
                socket = s.socket,
                bytesTransferred = s.bytes,
                intervalStartOffset = s.start,
                intervalEndOffset = s.end,
                intervalDuration = s.seconds,
                bitsPerSecond = s.bitsPerSecond,
                omitted = s.omitted,
                retransmits = s.retransmits ?: 0,
                rtt = s.rtt
            )
        }
}