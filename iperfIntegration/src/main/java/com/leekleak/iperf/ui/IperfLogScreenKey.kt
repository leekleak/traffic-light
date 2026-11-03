package com.leekleak.iperf.ui

import androidx.navigation3.runtime.NavKey
import com.leekleak.iperf.IperfEvent
import kotlinx.serialization.Serializable

@Serializable
data class IperfLogScreenKey(
    val events: List<IperfEvent>,
    val isServer: Boolean = false
) : NavKey
