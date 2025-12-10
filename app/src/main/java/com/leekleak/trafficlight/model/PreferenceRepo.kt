package com.leekleak.trafficlight.model


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.leekleak.trafficlight.services.PermissionManager
import com.leekleak.trafficlight.ui.theme.Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import kotlin.getValue

class PreferenceRepo (
    private val context: Context
): KoinComponent {
    val permissionManager: PermissionManager by inject()
    val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val scope = CoroutineScope(Job() + Dispatchers.Default)
    val data get() = context.dataStore.data

    val notification: Flow<Boolean> = combine(
        context.dataStore.data,
        permissionManager.notificationPermissionFlow
    ) { settings, permission ->
        return@combine (settings[NOTIFICATION] ?: false) && permission
    }

    fun setNotification(value: Boolean) = scope.launch { context.dataStore.edit { it[NOTIFICATION] = value } }

    val modeAOD: Flow<Boolean> = context.dataStore.data.map { it[MODE_AOD] ?: false }
    fun setModeAOD(value: Boolean) = scope.launch { context.dataStore.edit { it[MODE_AOD] = value } }

    val bigIcon: Flow<Boolean> = context.dataStore.data.map { it[BIG_ICON] ?: false }
    fun setBigIcon(value: Boolean) = scope.launch { context.dataStore.edit { it[BIG_ICON] = value } }

    val speedBits: Flow<Boolean> = context.dataStore.data.map { it[SPEED_BITS] ?: false }
    fun setSpeedBits(value: Boolean) = scope.launch { context.dataStore.edit { it[SPEED_BITS] = value } }

    val forceFallback: Flow<Boolean> = context.dataStore.data.map { it[FORCE_FALLBACK] ?: false }
    fun setForceFallback(value: Boolean) = scope.launch { context.dataStore.edit { it[FORCE_FALLBACK] = value } }

    val theme: Flow<Theme> = context.dataStore.data.map { Theme.valueOf(it[THEME] ?: Theme.AutoMaterial.name ) }
    fun setTheme(value: Theme) = scope.launch { context.dataStore.edit { it[THEME] = value.name } }

    val expressiveFonts: Flow<Boolean> = context.dataStore.data.map { it[EXPRESSIVE_FONTS] ?: true }
    fun setExpressiveFonts(value: Boolean) = scope.launch { context.dataStore.edit { it[EXPRESSIVE_FONTS] = value} }

    private companion object {
        val NOTIFICATION = booleanPreferencesKey("notification")
        val MODE_AOD = booleanPreferencesKey("mode_aod")
        val BIG_ICON = booleanPreferencesKey("big_icon")
        val SPEED_BITS = booleanPreferencesKey("speed_bits")
        val FORCE_FALLBACK = booleanPreferencesKey("force_fallback")
        val THEME = stringPreferencesKey("theme")
        val EXPRESSIVE_FONTS = booleanPreferencesKey("expressive_fonts")
    }
}