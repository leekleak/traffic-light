package com.leekleak.trafficlight.ui.iperf

import com.leekleak.trafficlight.integrations.iperf.IperfEvent
import kotlinx.coroutines.flow.StateFlow

interface IperfLogViewModel {
    val data: StateFlow<List<IperfEvent>>
}
