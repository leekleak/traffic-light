package com.leekleak.iperf.ui

import android.content.Context
import android.widget.Toast
import androidx.appstate.transform.transform
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.leekleak.iperf.IPerf3Provider
import com.leekleak.iperf.IperfCallback
import com.leekleak.iperf.IperfEvent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    val uiState: State<IperfServerUIState> = transform(
        initialValue = IperfServerUIState(),
        scope = viewModelScope,
        dispatcher = Dispatchers.Main,
    ) {
        val isRunning by iPerf3Provider.running.collectAsState()
        val isStopping by iPerf3Provider.stopping.collectAsState()

        IperfServerUIState(
            isIperfRunning = isRunning,
            isIperfStopping = isStopping,
        )
    }

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
