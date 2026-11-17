package com.leekleak.iperf.ui.logs

import com.leekleak.iperf.IperfEvent
import kotlinx.coroutines.flow.StateFlow

interface IperfLogViewModel {
    val data: StateFlow<List<IperfEvent>>
}
