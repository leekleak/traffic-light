package com.leekleak.trafficlight.ui.navigation

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.navigation3.runtime.NavKey

class Navigator(startDestination: NavKey) {
    val backStack : SnapshotStateList<NavKey> = mutableStateListOf(startDestination)

    fun goTo(destination: NavKey){
        backStack.add(destination)
    }

    fun setTo(destination: NavKey){
        backStack.clear().also { backStack.add(destination) }
    }

    fun goBack(){
        backStack.removeLastOrNull()
    }
}