package com.leekleak.core.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

class Navigator(val backStack: NavBackStack<NavKey>) {
    val current: NavKey
        get() = backStack.last()

    fun goTo(destination: NavKey) {
        backStack.add(destination)
    }

    fun setTo(destination: NavKey) {
        if (backStack.size == 1 && backStack.last() == destination) return
        Snapshot.withMutableSnapshot {
            backStack.clear()
            backStack.add(destination)
        }
    }

    fun goBack() {
        if (backStack.size != 1) backStack.removeLastOrNull()
    }
}

val LocalNavigator = staticCompositionLocalOf<Navigator> {
    error("Navigator is not provided")
}
