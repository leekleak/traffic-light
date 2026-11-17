package com.leekleak.iperf.ui.client

import android.content.Context
import android.widget.Toast
import androidx.appstate.transform.transform
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.core.util.DataSizeUnit
import com.leekleak.iperf.IPerf3Provider
import com.leekleak.iperf.IntervalEvent
import com.leekleak.iperf.IperfCallback
import com.leekleak.iperf.IperfEvent
import com.leekleak.iperf.data.IPerfEntry
import com.leekleak.iperf.data.IPerfEntryDao
import com.leekleak.iperf.ui.logs.IperfLogViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class IperfClientUIState(
    val selectedEntry: IPerfEntry? = null,
    val entries: List<IPerfEntry> = emptyList(),
    val selectedProtocol: IperfProtocol = IperfProtocol.TCP,
    val selectedDirection: IperfDirection = IperfDirection.UPLOAD,
    val bandwidth: Int = 1,
    val bandwidthUnit: DataSizeUnit = DataSizeUnit.MB,
    val parallelStreams: Int = 1,
    val testDuration: Int = 10,
    val showTestSettings: Boolean = false,
    val showEntrySelector: Boolean = false,
    val showEntryCreator: Boolean = false,
    val showEntryDeletion: Boolean = false,
    val editEntry: IPerfEntry? = null,
    val speedData: List<Float> = emptyList(),
    val isIperfRunning: Boolean = false,
    val isIperfStopping: Boolean = false,
) {
    val arguments: Array<String>?
        get() {
            val entry = selectedEntry ?: return null
            val args = mutableListOf(
                "-c", entry.ip,
                "-p", entry.port,
                "-t", testDuration.toString(),
                "-i", "0.5"
            )
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

private data class IperfClientInputs(
    val selectedProtocol: IperfProtocol = IperfProtocol.TCP,
    val selectedDirection: IperfDirection = IperfDirection.UPLOAD,
    val bandwidth: Int = 1,
    val bandwidthUnit: DataSizeUnit = DataSizeUnit.MB,
    val parallelStreams: Int = 1,
    val testDuration: Int = 10,
    val showTestSettings: Boolean = false,
    val showEntrySelector: Boolean = false,
    val showEntryCreator: Boolean = false,
    val showEntryDeletion: Boolean = false,
    val editEntry: IPerfEntry? = null,
)

class IperfClientVM(
    private val iPerfEntryDao: IPerfEntryDao,
    private val iPerf3Provider: IPerf3Provider,
) : ViewModel(), IperfLogViewModel {

    private val inputs = MutableStateFlow(IperfClientInputs())

    override val data: StateFlow<List<IperfEvent>>
        field = MutableStateFlow(listOf())

    val uiState: State<IperfClientUIState> = transform(
        initialValue = IperfClientUIState(),
        scope = viewModelScope,
        dispatcher = Dispatchers.Main,
    ) {
        val current by inputs.collectAsState()
        val entries by iPerfEntryDao.allEntries.collectAsState(emptyList())
        val events by data.collectAsState()
        val isRunning by iPerf3Provider.running.collectAsState()
        val isStopping by iPerf3Provider.stopping.collectAsState()

        IperfClientUIState(
            selectedEntry = entries.firstOrNull { it.selected },
            entries = entries,
            selectedProtocol = current.selectedProtocol,
            selectedDirection = current.selectedDirection,
            bandwidth = current.bandwidth,
            bandwidthUnit = current.bandwidthUnit,
            parallelStreams = current.parallelStreams,
            testDuration = current.testDuration,
            showTestSettings = current.showTestSettings,
            showEntrySelector = current.showEntrySelector,
            showEntryCreator = current.showEntryCreator,
            showEntryDeletion = current.showEntryDeletion,
            editEntry = current.editEntry,
            speedData = events.mapNotNull {
                (it as? IntervalEvent)?.data?.sum?.bitsPerSecond?.toFloat()
            },
            isIperfRunning = isRunning,
            isIperfStopping = isStopping,
        )
    }

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
        inputs.update { it.copy(selectedProtocol = protocol) }
    }

    fun setSelectedDirection(direction: IperfDirection) {
        inputs.update { it.copy(selectedDirection = direction) }
    }

    fun setBandwidth(bandwidth: Int) {
        inputs.update { it.copy(bandwidth = bandwidth) }
    }

    fun setBandwidthUnit(bandwidthUnit: DataSizeUnit) {
        inputs.update { it.copy(bandwidthUnit = bandwidthUnit) }
    }

    fun setParallelStreams(parallelStreams: Int) {
        inputs.update { it.copy(parallelStreams = parallelStreams) }
    }

    fun setTestDuration(testDuration: Int) {
        inputs.update { it.copy(testDuration = testDuration) }
    }

    fun setShowTestSettings(show: Boolean) {
        inputs.update { it.copy(showTestSettings = show) }
    }

    fun setShowEntrySelector(show: Boolean) {
        inputs.update { it.copy(showEntrySelector = show) }
    }

    fun setShowEntryCreator(show: Boolean) {
        inputs.update { it.copy(showEntryCreator = show) }
    }

    fun setShowEntryDeletion(show: Boolean) {
        inputs.update { it.copy(showEntryDeletion = show) }
    }

    fun setEditEntry(entry: IPerfEntry?) {
        inputs.update { it.copy(editEntry = entry) }
    }

    fun addIperfEvent(entry: IperfEvent) {
        data.value += entry
    }

    fun clearIperfEvents() {
        data.value = emptyList()
    }

    fun startTest(context: Context) {
        val args = uiState.value.arguments ?: return
        if (iPerf3Provider.running.value) return
        clearIperfEvents()
        viewModelScope.launch {
            iPerf3Provider.runTest(
                args,
                object : IperfCallback {
                    override fun onOutput(event: IperfEvent) {
                        addIperfEvent(event)
                    }

                    override fun onError(error: String) {
                        viewModelScope.launch {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, error, Toast.LENGTH_SHORT).show()
                            }
                        }
                    }

                    override fun onComplete() {}
                }
            )
        }
    }

    fun stopTest() {
        iPerf3Provider.stopTest()
    }
}
