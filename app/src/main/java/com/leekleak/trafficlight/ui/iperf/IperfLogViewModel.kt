package com.leekleak.trafficlight.ui.iperf

import com.leekleak.iperfintegration.IperfEvent
import kotlinx.coroutines.flow.StateFlow

interface IperfLogViewModel {
    val data: StateFlow<List<IperfEvent>>
}
