package com.leekleak.trafficlight.ui.iperf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.iperfintegration.IPerf3Provider
import com.leekleak.iperfintegration.IntervalEvent
import com.leekleak.iperfintegration.IperfEvent
import com.leekleak.trafficlight.database.IPerfEntry
import com.leekleak.trafficlight.database.IPerfEntryDao
import com.leekleak.trafficlight.util.DataSizeUnit
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class IperfClientUIState(
    val selectedEntry: IPerfEntry? = null,
    val entries: List<IPerfEntry> = emptyList(),
    val selectedProtocol: IperfProtocol = IperfProtocol.TCP,
    val selectedDirection: IperfDirection = IperfDirection.UPLOAD,
    val bandwidth: Int = 1,
    val bandwidthUnit: DataSizeUnit = DataSizeUnit.MB,
    val parallelStreams: Int = 1,
    val showTestSettings: Boolean = false,
    val showEntrySelector: Boolean = false,
    val showEntryCreator: Boolean = false,
    val showEntryDeletion: Boolean = false,
    val editEntry: IPerfEntry? = null,
    val speedData: List<Float> = emptyList(),
) {
    val arguments: Array<String>?
        get() {
            val entry = selectedEntry ?: return null
            val args = mutableListOf("-c", entry.ip, "-p", entry.port, "-i", "0.5")
            if (parallelStreams > 1) {
                args.addAll(listOf("-P", parallelStreams.toString()))
            }
            when (selectedProtocol) {
                IperfProtocol.UDP -> {
                    args.add("--udp")
                    val unitChar = when (bandwidthUnit) {
                        DataSizeUnit.GB -> "G"
                        DataSizeUnit.KB -> "K"
                        else -> "M"
                    }
                    args.addAll(listOf("-b", "$bandwidth$unitChar"))
                }
                else -> {}
            }
            when (selectedDirection) {
                IperfDirection.DOWNLOAD -> args.add("-R")
                else -> {}
            }
            return args.toTypedArray()
        }
}

class IperfClientVM(
    private val iPerfEntryDao: IPerfEntryDao,
    val iPerf3Provider: IPerf3Provider,
) : ViewModel() {

    private val selectedProtocol = MutableStateFlow(IperfProtocol.TCP)
    private val selectedDirection = MutableStateFlow(IperfDirection.UPLOAD)
    private val bandwidth = MutableStateFlow(1)
    private val bandwidthUnit = MutableStateFlow(DataSizeUnit.MB)
    private val parallelStreams = MutableStateFlow(1)
    private val showTestSettings = MutableStateFlow(value = false)
    private val showEntrySelector = MutableStateFlow(value = false)
    private val showEntryCreator = MutableStateFlow(value = false)
    private val showEntryDeletion = MutableStateFlow(value = false)
    private val editEntry = MutableStateFlow<IPerfEntry?>(null)

    val data: StateFlow<List<IperfEvent>>
        field = MutableStateFlow(listOf())

    private val speedData: Flow<List<Float>> = data.map { list ->
        list.mapNotNull { event ->
            if (event is IntervalEvent) {
                event.data.sum?.bitsPerSecond?.toFloat()?.div(8f)
            } else {
                null
            }
        }
    }
    val uiState: StateFlow<IperfClientUIState> = combine(
        iPerfEntryDao.allEntries,
        selectedProtocol,
        selectedDirection,
        bandwidth,
        bandwidthUnit,
        parallelStreams,
        showTestSettings,
        showEntrySelector,
        showEntryCreator,
        showEntryDeletion,
        editEntry,
        speedData
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val entries = flows[0] as List<IPerfEntry>
        val protocol = flows[1] as IperfProtocol
        val direction = flows[2] as IperfDirection
        val bw = flows[3] as Int
        val bwUnit = flows[4] as DataSizeUnit
        val streams = flows[5] as Int
        val testSettings = flows[6] as Boolean
        val entrySelector = flows[7] as Boolean
        val entryCreator = flows[8] as Boolean
        val entryDeletion = flows[9] as Boolean
        val editing = flows[10] as IPerfEntry?
        val speedData = flows[11] as List<Float>

        IperfClientUIState(
            selectedEntry = entries.firstOrNull { it.selected },
            entries = entries,
            selectedProtocol = protocol,
            selectedDirection = direction,
            bandwidth = bw,
            bandwidthUnit = bwUnit,
            parallelStreams = streams,
            showTestSettings = testSettings,
            showEntrySelector = entrySelector,
            showEntryCreator = entryCreator,
            showEntryDeletion = entryDeletion,
            editEntry = editing,
            speedData = speedData
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = IperfClientUIState()
    )

    fun selectEntry(entry: IPerfEntry) {
        viewModelScope.launch {
            val entries = uiState.value.entries
            val currentlySelected = entries.find { it.selected }
            currentlySelected?.let {
                iPerfEntryDao.upsert(it.copy(selected = false))
            }
            iPerfEntryDao.upsert(entry.copy(selected = true))
        }
    }

    fun deleteEntry(entry: IPerfEntry) {
        viewModelScope.launch {
            iPerfEntryDao.delete(entry)
        }
    }

    fun setSelectedProtocol(protocol: IperfProtocol) {
        selectedProtocol.value = protocol
    }

    fun setSelectedDirection(direction: IperfDirection) {
        selectedDirection.value = direction
    }

    fun setBandwidth(bandwidth: Int) {
        this.bandwidth.value = bandwidth
    }

    fun setBandwidthUnit(bandwidthUnit: DataSizeUnit) {
        this.bandwidthUnit.value = bandwidthUnit
    }

    fun setParallelStreams(parallelStreams: Int) {
        this.parallelStreams.value = parallelStreams
    }

    fun setShowTestSettings(show: Boolean) {
        showTestSettings.value = show
    }

    fun setShowEntrySelector(show: Boolean) {
        showEntrySelector.value = show
    }

    fun setShowEntryCreator(show: Boolean) {
        showEntryCreator.value = show
    }

    fun setShowEntryDeletion(show: Boolean) {
        showEntryDeletion.value = show
    }

    fun setEditEntry(entry: IPerfEntry?) {
        editEntry.value = entry
    }

    fun addIperfEvent(entry: IperfEvent) {
        data.value += entry
    }

    fun clearIperfEvents() {
        data.value = emptyList()
    }
}
