package com.leekleak.trafficlight.ui.iperf

import android.content.Context
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.iperfintegration.IPerf3Provider
import com.leekleak.iperfintegration.IperfCallback
import com.leekleak.iperfintegration.IperfEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class IperfServerUIState(
    val isIperfRunning: Boolean = false,
    val isIperfStopping: Boolean = false,
)

class IperfServerVM(
    private val iPerf3Provider: IPerf3Provider,
) : ViewModel(), IperfLogViewModel {

    override val data: StateFlow<List<IperfEvent>>
        field = MutableStateFlow(listOf())

    val uiState: StateFlow<IperfServerUIState> = combine(
        iPerf3Provider.running,
        iPerf3Provider.stopping
    ) { running, stopping ->
        IperfServerUIState(
            isIperfRunning = running,
            isIperfStopping = stopping
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000L),
        initialValue = IperfServerUIState()
    )

    fun startTest(context: Context) {
        if (iPerf3Provider.running.value) return
        clearIperfEvents()
        viewModelScope.launch {
            iPerf3Provider.runTest(
                arrayOf("-s", "-p", "5201"),
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

    fun addIperfEvent(event: IperfEvent) {
        data.value += event
    }

    fun clearIperfEvents() {
        data.value = emptyList()
    }
}
