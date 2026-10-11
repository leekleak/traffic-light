package com.leekleak.trafficlight.ui.iperf

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.leekleak.iperfintegration.EndData
import com.leekleak.iperfintegration.EndEvent
import com.leekleak.iperfintegration.ErrorEvent
import com.leekleak.iperfintegration.IntervalEvent
import com.leekleak.iperfintegration.IperfEvent
import com.leekleak.iperfintegration.ServerOutputJsonEvent
import com.leekleak.iperfintegration.ServerOutputTextEvent
import com.leekleak.iperfintegration.StartData
import com.leekleak.iperfintegration.StartEvent
import com.leekleak.iperfintegration.SumStats
import com.leekleak.iperfintegration.TcpIntervalResult
import com.leekleak.iperfintegration.UdpIntervalResult
import com.leekleak.iperfintegration.UnknownEvent
import com.leekleak.trafficlight.R
import com.leekleak.trafficlight.ui.components.BackAction
import com.leekleak.trafficlight.ui.components.HazeScaffold
import com.leekleak.trafficlight.ui.navigation.Navigator
import com.leekleak.trafficlight.ui.theme.LocalSizeMetric
import com.leekleak.trafficlight.ui.theme.LocalSpeedMetric
import com.leekleak.trafficlight.ui.theme.card
import com.leekleak.trafficlight.util.DataSize
import kotlinx.serialization.json.JsonObject
import java.util.Locale

private val TcpColIdWidth = 50.dp
private val TcpColIntervalWidth = 75.dp
private val TcpColTransferWidth = 85.dp
private val TcpColBandwidthWidth = 100.dp
private val TcpColRetrWidth = 50.dp
private val TcpColRttWidth = 65.dp

private val UdpColIdWidth = 50.dp
private val UdpColIntervalWidth = 75.dp
private val UdpColTransferWidth = 85.dp
private val UdpColBandwidthWidth = 100.dp
private val UdpColJitterWidth = 70.dp
private val UdpColLostWidth = 120.dp

private sealed interface LogBlock {
    data class SingleEvent(val event: IperfEvent) : LogBlock
    data class IntervalGroup(val events: List<IntervalEvent>, val isUdp: Boolean) : LogBlock
}

private sealed interface IntervalTableRowData {
    val isSum: Boolean
    val socketLabel: String
    val intervalText: String
    val transferText: String
    val bandwidthText: String
    val omitted: Boolean

    data class TcpRow(
        override val isSum: Boolean,
        override val socketLabel: String,
        override val intervalText: String,
        override val transferText: String,
        override val bandwidthText: String,
        override val omitted: Boolean,
        val retransmitsText: String,
        val rttText: String,
    ) : IntervalTableRowData

    data class UdpRow(
        override val isSum: Boolean,
        override val socketLabel: String,
        override val intervalText: String,
        override val transferText: String,
        override val bandwidthText: String,
        override val omitted: Boolean,
        val jitterText: String,
        val lostText: String,
    ) : IntervalTableRowData
}

@Composable
fun IperfLogScreen(
    navigator: Navigator,
    events: List<IperfEvent>,
    viewModel: IperfLogViewModel,
) {
    val liveEvents by viewModel.data.collectAsStateWithLifecycle()
    val displayEvents = liveEvents.ifEmpty { events }
    val sizeMetric = LocalSizeMetric.current
    val speedMetric = LocalSpeedMetric.current

    HazeScaffold(
        title = stringResource(R.string.logs),
        backAction = BackAction.Normal(navigator),
    ) { contentPadding ->
        if (displayEvents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(contentPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_logs_available),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            val blocks = remember(displayEvents) { partitionEvents(displayEvents) }
            val blockRows = remember(blocks, sizeMetric, speedMetric) {
                blocks.map { block ->
                    if (block is LogBlock.IntervalGroup) {
                        buildIntervalRows(block.events, sizeMetric, speedMetric)
                    } else {
                        emptyList()
                    }
                }
            }

            blocks.forEachIndexed { blockIndex, block ->
                when (block) {
                    is LogBlock.SingleEvent -> {
                        when (val event = block.event) {
                            is StartEvent -> StartEventCard(event.data)
                            is EndEvent -> EndEventCard(event.data)
                            is ErrorEvent -> ErrorEventCard(event.message)
                            is ServerOutputTextEvent -> ServerOutputTextCard(event.text)
                            is ServerOutputJsonEvent -> ServerOutputJsonCard(event.json)
                            is UnknownEvent -> UnknownEventCard(event)
                            else -> {}
                        }
                    }
                    is LogBlock.IntervalGroup -> {
                        Column(
                            modifier = Modifier
                                .clip(MaterialTheme.shapes.medium)
                                .horizontalScroll(rememberScrollState()),
                        ) {
                            IntervalTableHeader(
                                isUdp = block.isUdp,
                            )

                            val rows = blockRows[blockIndex]

                            for (i in rows.indices) {
                                IntervalTableRow(
                                    isOdd = i % 2 == 1,
                                    rowData = rows[i],
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun partitionEvents(events: List<IperfEvent>): List<LogBlock> {
    val blocks = mutableListOf<LogBlock>()
    val currentIntervals = mutableListOf<IntervalEvent>()

    fun flushIntervals() {
        if (currentIntervals.isNotEmpty()) {
            val isUdp = currentIntervals.any { event ->
                event.results.any { it is UdpIntervalResult } ||
                        event.data.streams.any { it.jitterMs != null || it.lostPackets != null } ||
                        event.data.sum?.jitterMs != null
            }
            blocks.add(LogBlock.IntervalGroup(currentIntervals.toList(), isUdp))
            currentIntervals.clear()
        }
    }

    for (event in events) {
        if (event is IntervalEvent) {
            currentIntervals.add(event)
        } else {
            flushIntervals()
            blocks.add(LogBlock.SingleEvent(event))
        }
    }
    flushIntervals()

    return blocks
}

private fun buildIntervalRows(
    events: List<IntervalEvent>,
    sizeMetric: Boolean,
    speedMetric: Boolean
): List<IntervalTableRowData> {
    val rows = mutableListOf<IntervalTableRowData>()

    events.forEach { event ->
        var hasUdpResult = false

        event.results.forEach { result ->
            when (result) {
                is TcpIntervalResult -> {
                    rows.add(
                        IntervalTableRowData.TcpRow(
                            isSum = false,
                            socketLabel = "[${result.socket}]",
                            intervalText = formatInterval(result.intervalStartOffset, result.intervalEndOffset),
                            transferText = formatBytes(result.bytesTransferred, sizeMetric),
                            bandwidthText = formatBitrate(result.bitsPerSecond, speedMetric),
                            retransmitsText = "${result.retransmits}",
                            rttText = result.rtt?.let { formatRtt(it) } ?: "-",
                            omitted = result.omitted
                        )
                    )
                }
                is UdpIntervalResult -> {
                    hasUdpResult = true
                    rows.add(
                        IntervalTableRowData.UdpRow(
                            isSum = false,
                            socketLabel = "[${result.socket}]",
                            intervalText = formatInterval(result.intervalStartOffset, result.intervalEndOffset),
                            transferText = formatBytes(result.bytesTransferred, sizeMetric),
                            bandwidthText = formatBitrate(result.bitsPerSecond, speedMetric),
                            jitterText = "${String.format(Locale.US, "%.2f", result.jitterMs)} ms",
                            lostText = "${result.lostPackets}/${result.packetCount} (${String.format(Locale.US, "%.1f", result.lostPercent)}%)",
                            omitted = result.omitted
                        )
                    )
                }
            }
        }

        event.data.sum?.let { sum ->
            if (event.results.size > 1 || event.results.isEmpty()) {
                val isUdpSum = hasUdpResult || sum.jitterMs != null || sum.lostPackets != null
                if (isUdpSum) {
                    rows.add(
                        IntervalTableRowData.UdpRow(
                            isSum = true,
                            socketLabel = "[SUM]",
                            intervalText = formatInterval(sum.start, sum.end),
                            transferText = formatBytes(sum.bytes, sizeMetric),
                            bandwidthText = formatBitrate(sum.bitsPerSecond, speedMetric),
                            jitterText = sum.jitterMs?.let { "${String.format(Locale.US, "%.2f", it)} ms" } ?: "-",
                            lostText = if (sum.lostPackets != null && sum.packets != null) {
                                "${sum.lostPackets}/${sum.packets} (${String.format(Locale.US, "%.1f", sum.lostPercent ?: 0.0)}%)"
                            } else "-",
                            omitted = sum.omitted ?: false
                        )
                    )
                } else {
                    rows.add(
                        IntervalTableRowData.TcpRow(
                            isSum = true,
                            socketLabel = "[SUM]",
                            intervalText = formatInterval(sum.start, sum.end),
                            transferText = formatBytes(sum.bytes, sizeMetric),
                            bandwidthText = formatBitrate(sum.bitsPerSecond, speedMetric),
                            retransmitsText = "${sum.retransmits ?: 0}",
                            rttText = "-",
                            omitted = sum.omitted ?: false
                        )
                    )
                }
            }
        }
    }

    return rows
}

@Composable
private fun IntervalTableHeader(
    isUdp: Boolean,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        shadowElevation = 2.dp
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = stringResource(if (isUdp) R.string.udp_interval_results else R.string.tcp_interval_results),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isUdp) {
                    TableCell(stringResource(R.string.socket_id), UdpColIdWidth, TextAlign.Start, FontWeight.Bold)
                    TableCell(stringResource(R.string.interval), UdpColIntervalWidth, TextAlign.End, FontWeight.Bold)
                    TableCell(stringResource(R.string.transfer), UdpColTransferWidth, TextAlign.End, FontWeight.Bold)
                    TableCell(stringResource(R.string.bandwidth), UdpColBandwidthWidth, TextAlign.End, FontWeight.Bold)
                    TableCell(stringResource(R.string.jitter), UdpColJitterWidth, TextAlign.End, FontWeight.Bold)
                    TableCell(stringResource(R.string.lost_total), UdpColLostWidth, TextAlign.End, FontWeight.Bold)
                } else {
                    TableCell(stringResource(R.string.socket_id), TcpColIdWidth, TextAlign.Start, FontWeight.Bold)
                    TableCell(stringResource(R.string.interval), TcpColIntervalWidth, TextAlign.End, FontWeight.Bold)
                    TableCell(stringResource(R.string.transfer), TcpColTransferWidth, TextAlign.End, FontWeight.Bold)
                    TableCell(stringResource(R.string.bandwidth), TcpColBandwidthWidth, TextAlign.End, FontWeight.Bold)
                    TableCell(stringResource(R.string.retransmits), TcpColRetrWidth, TextAlign.End, FontWeight.Bold)
                    TableCell(stringResource(R.string.rtt), TcpColRttWidth, TextAlign.End, FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun IntervalTableRow(
    isOdd: Boolean,
    rowData: IntervalTableRowData,
) {
    val backgroundColor = when {
        rowData.isSum -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.25f)
        else -> {
            if (isOdd) {
                MaterialTheme.colorScheme.surfaceContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerLow
            }
        }
    }
    val textColor = when {
        rowData.omitted -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        rowData.isSum -> MaterialTheme.colorScheme.primary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val fontWeight = if (rowData.isSum) FontWeight.Bold else FontWeight.Normal

    Box(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            when (rowData) {
                is IntervalTableRowData.TcpRow -> {
                    TableCell(rowData.socketLabel, TcpColIdWidth, TextAlign.Start, fontWeight, textColor)
                    TableCell(rowData.intervalText, TcpColIntervalWidth, TextAlign.End, fontWeight, textColor)
                    TableCell(rowData.transferText, TcpColTransferWidth, TextAlign.End, fontWeight, textColor)
                    TableCell(rowData.bandwidthText, TcpColBandwidthWidth, TextAlign.End, fontWeight, textColor)
                    TableCell(rowData.retransmitsText, TcpColRetrWidth, TextAlign.End, fontWeight, textColor)
                    TableCell(rowData.rttText, TcpColRttWidth, TextAlign.End, fontWeight, textColor)
                }
                is IntervalTableRowData.UdpRow -> {
                    TableCell(rowData.socketLabel, UdpColIdWidth, TextAlign.Start, fontWeight, textColor)
                    TableCell(rowData.intervalText, UdpColIntervalWidth, TextAlign.End, fontWeight, textColor)
                    TableCell(rowData.transferText, UdpColTransferWidth, TextAlign.End, fontWeight, textColor)
                    TableCell(rowData.bandwidthText, UdpColBandwidthWidth, TextAlign.End, fontWeight, textColor)
                    TableCell(rowData.jitterText, UdpColJitterWidth, TextAlign.End, fontWeight, textColor)
                    TableCell(rowData.lostText, UdpColLostWidth, TextAlign.End, fontWeight, textColor)
                }
            }
        }
    }
}

@Composable
private fun TableCell(
    text: String,
    width: Dp,
    align: TextAlign = TextAlign.End,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color = Color.Unspecified,
    style: TextStyle = MaterialTheme.typography.bodySmall
) {
    Text(
        text = text,
        modifier = Modifier.width(width),
        textAlign = align,
        fontWeight = fontWeight,
        color = color,
        style = style,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun StartEventCard(data: StartData) {
    val sizeMetric = LocalSizeMetric.current
    val speedMetric = LocalSpeedMetric.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.discover_tune),
                contentDescription = null,
            )
            Text(
                text = stringResource(R.string.test_configuration),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoChip(label = data.testStart.protocol.uppercase())
            data.testStart.numStreams?.let {
                InfoChip(label = "$it ${if (it == 1) "stream" else "streams"}")
            }
            if (data.testStart.reverse == 1) {
                InfoChip(label = stringResource(R.string.download))
            } else {
                InfoChip(label = stringResource(R.string.upload))
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            val connecting = data.connectingTo
            val accepted = data.acceptedConnection
            val serverHostPort = when {
                connecting != null -> "${connecting.host}:${connecting.port}"
                accepted != null -> "${accepted.host}:${accepted.port}"
                else -> null
            }
            serverHostPort?.let {
                DetailRow(label = stringResource(R.string.server_address), value = it)
            }
            data.testStart.duration?.let {
                DetailRow(label = stringResource(R.string.time), value = "${it.toInt()} s")
            }
            data.testStart.blksize?.let {
                DetailRow(label = stringResource(R.string.block_sizes), value = DataSize(it).toString(speed = false, metric = sizeMetric))
            }
            data.testStart.targetBitrate?.let {
                if (it > 0) {
                    val bps = (it / 8.0).toLong()
                    DetailRow(label = stringResource(R.string.bandwidth), value = DataSize(bps).toString(speed = true, inBits = true, metric = speedMetric))
                }
            }
            data.version?.let {
                DetailRow(label = stringResource(R.string.version), value = it)
            }
            data.timestamp?.time?.let {
                DetailRow(label = stringResource(R.string.time), value = it)
            }
        }

        if (data.connected.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Text(
                text = "Connected Sockets",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            data.connected.forEach { socket ->
                Text(
                    text = "[${socket.socket}] ${socket.localHost}:${socket.localPort} ➔ ${socket.remoteHost}:${socket.remotePort}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EndEventCard(data: EndData) {
    val sizeMetric = LocalSizeMetric.current
    val speedMetric = LocalSpeedMetric.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .card()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.text_snippet),
                contentDescription = null,
            )
            Text(
                text = stringResource(R.string.test_summary),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        data.sumSent?.let { sent ->
            StatBlock(
                title = stringResource(R.string.sender),
                bytes = sent.bytes,
                bitsPerSecond = sent.bitsPerSecond,
                retransmits = sent.retransmits,
                sizeMetric = sizeMetric,
                speedMetric = speedMetric
            )
        }
        data.sumReceived?.let { rcvd ->
            StatBlock(
                title = stringResource(R.string.receiver),
                bytes = rcvd.bytes,
                bitsPerSecond = rcvd.bitsPerSecond,
                retransmits = null,
                sizeMetric = sizeMetric,
                speedMetric = speedMetric
            )
        }
        data.sum?.let { udpSum ->
            UdpStatBlock(
                title = stringResource(R.string.test_summary),
                sum = udpSum,
                sizeMetric = sizeMetric,
                speedMetric = speedMetric
            )
        }

        if (data.senderTcpCongestion != null || data.receiverTcpCongestion != null) {
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            DetailRow(
                label = stringResource(R.string.congestion_control),
                value = listOfNotNull(data.senderTcpCongestion, data.receiverTcpCongestion).joinToString(" / ")
            )
        }

        data.cpuUtilizationPercent?.let { cpu ->
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
            Text(
                text = stringResource(R.string.cpu_utilization),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.host_cpu),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", cpu.hostTotal)}% (User: ${String.format(Locale.US, "%.1f", cpu.hostUser)}%, Sys: ${String.format(Locale.US, "%.1f", cpu.hostSystem)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.remote_cpu),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "${String.format(Locale.US, "%.1f", cpu.remoteTotal)}% (User: ${String.format(Locale.US, "%.1f", cpu.remoteUser)}%, Sys: ${String.format(Locale.US, "%.1f", cpu.remoteSystem)}%)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun StatBlock(
    title: String,
    bytes: Long,
    bitsPerSecond: Double,
    retransmits: Long?,
    sizeMetric: Boolean,
    speedMetric: Boolean
) {
    val bytesText = DataSize(bytes).toString(speed = false, metric = sizeMetric)
    val bpsText = DataSize((bitsPerSecond / 8.0).toLong()).toString(speed = true, inBits = true, metric = speedMetric)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(R.string.transfer)}: $bytesText",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${stringResource(R.string.bandwidth)}: $bpsText",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        retransmits?.let {
            Text(
                text = "${stringResource(R.string.retransmits)}: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun UdpStatBlock(
    title: String,
    sum: SumStats,
    sizeMetric: Boolean,
    speedMetric: Boolean
) {
    val bytesText = DataSize(sum.bytes).toString(speed = false, metric = sizeMetric)
    val bpsText = DataSize((sum.bitsPerSecond / 8.0).toLong()).toString(speed = true, inBits = true, metric = speedMetric)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small
            )
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "${stringResource(R.string.transfer)}: $bytesText",
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "${stringResource(R.string.bandwidth)}: $bpsText",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold
            )
        }
        sum.jitterMs?.let {
            Text(
                text = "${stringResource(R.string.jitter)}: ${String.format(Locale.US, "%.2f", it)} ms",
                style = MaterialTheme.typography.bodySmall
            )
        }
        if (sum.lostPackets != null && sum.packets != null) {
            Text(
                text = "${stringResource(R.string.lost_total)}: ${sum.lostPackets}/${sum.packets} (${String.format(Locale.US, "%.1f", sum.lostPercent ?: 0.0)}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ErrorEventCard(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.warning),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = stringResource(R.string.error_log),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer
        )
    }
}

@Composable
private fun ServerOutputTextCard(text: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .card()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.server_output),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ServerOutputJsonCard(json: JsonObject) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .card()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = stringResource(R.string.server_output),
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            text = json.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun UnknownEventCard(event: UnknownEvent) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .card()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "${stringResource(R.string.unknown)}: ${event.event}",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = event.data.toString(),
            style = MaterialTheme.typography.bodySmall,
            fontFamily = FontFamily.Monospace,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun InfoChip(label: String) {
    Surface(
        color = MaterialTheme.colorScheme.primaryContainer,
        shape = MaterialTheme.shapes.small
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatInterval(start: Double, end: Double): String {
    return String.format(Locale.US, "%.1f-%.1f s", start, end)
}

private fun formatRtt(rttUs: Long): String {
    return String.format(Locale.US, "%.2f ms", rttUs / 1000.0)
}

private fun formatBytes(bytes: Long, metric: Boolean): String {
    return DataSize(bytes).toString(speed = false, inBits = false, metric = metric)
}

private fun formatBitrate(bitsPerSecond: Double, metric: Boolean): String {
    val bytesPerSec = (bitsPerSecond / 8.0).toLong()
    return DataSize(bytesPerSec).toString(speed = true, inBits = true, metric = metric)
}
