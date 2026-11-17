package com.leekleak.iperf.ui

import androidx.appstate.transform.transform
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.iperf.IperfNetworkMonitor
import kotlinx.coroutines.Dispatchers

data class IperfScreenUIState(
    val showServer: Boolean = false,
    val myIp: String? = null,
)

class IperfScreenVM(
    private val networkMonitor: IperfNetworkMonitor,
) : ViewModel() {
    private val showServer = mutableStateOf(false)

    fun setShowServer(show: Boolean) {
        showServer.value = show
    }

    val uiState: State<IperfScreenUIState> = transform(
        initialValue = IperfScreenUIState(),
        scope = viewModelScope,
        dispatcher = Dispatchers.Main,
    ) {
        val isServerShown by showServer
        val myIp by networkMonitor.myIp

        IperfScreenUIState(
            showServer = isServerShown,
            myIp = myIp,
        )
    }
}
