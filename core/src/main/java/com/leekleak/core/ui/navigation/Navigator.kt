package com.leekleak.core.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey

class Navigator(startDestination: NavKey) {
    val backStack : SnapshotStateList<NavKey> = mutableStateListOf(startDestination)
    val current: NavKey
        get() = backStack.last()

    fun goTo(destination: NavKey){
        backStack.add(destination)
    }

    fun setTo(destination: NavKey){
        Snapshot.withMutableSnapshot {
            backStack.clear()
            backStack.add(destination)
        }
    }

    fun goBack(){
        if (backStack.size != 1) backStack.removeLastOrNull()
    }
}