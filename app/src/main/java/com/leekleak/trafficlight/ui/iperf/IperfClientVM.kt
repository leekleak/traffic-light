package com.leekleak.trafficlight.ui.iperf

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.iperfintegration.IPerf3Provider
import com.leekleak.trafficlight.database.IPerfEntry
import com.leekleak.trafficlight.database.IPerfEntryDao
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class IperfClientUIState(
    val selectedEntry: IPerfEntry? = null,
    val entries: List<IPerfEntry> = emptyList(),
    val selectedProtocol: IperfProtocol = IperfProtocol.TCP,
    val showTestSettings: Boolean = false,
    val showEntrySelector: Boolean = false,
    val showEntryCreator: Boolean = false,
    val showEntryDeletion: Boolean = false,
    val editEntry: IPerfEntry? = null,
) {
    val arguments: Array<String>?
        get() {
            val entry = selectedEntry ?: return null
            val args = mutableListOf("-c", entry.ip, "-p", entry.port, "-i", "0.5")
            when (selectedProtocol) {
                IperfProtocol.UDP -> args.add("--udp")
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
    private val showTestSettings = MutableStateFlow(value = false)
    private val showEntrySelector = MutableStateFlow(value = false)
    private val showEntryCreator = MutableStateFlow(value = false)
    private val showEntryDeletion = MutableStateFlow(value = false)
    private val editEntry = MutableStateFlow<IPerfEntry?>(null)

    val uiState: StateFlow<IperfClientUIState> = combine(
        iPerfEntryDao.allEntries,
        selectedProtocol,
        showTestSettings,
        showEntrySelector,
        showEntryCreator,
        showEntryDeletion,
        editEntry,
    ) { flows ->
        @Suppress("UNCHECKED_CAST")
        val entries = flows[0] as List<IPerfEntry>
        val protocol = flows[1] as IperfProtocol
        val testSettings = flows[2] as Boolean
        val entrySelector = flows[3] as Boolean
        val entryCreator = flows[4] as Boolean
        val entryDeletion = flows[5] as Boolean
        val editing = flows[6] as IPerfEntry?

        IperfClientUIState(
            selectedEntry = entries.firstOrNull { it.selected },
            entries = entries,
            selectedProtocol = protocol,
            showTestSettings = testSettings,
            showEntrySelector = entrySelector,
            showEntryCreator = entryCreator,
            showEntryDeletion = entryDeletion,
            editEntry = editing,
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
}
