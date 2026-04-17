package com.leekleak.trafficlight.database


import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.leekleak.trafficlight.model.PermissionManager
import com.leekleak.trafficlight.ui.theme.Theme
import com.leekleak.trafficlight.util.valueOfOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

class AppPreferenceRepo (
    private val context: Context,
    permissionManager: PermissionManager
) {
    private val Context.appPreferences: DataStore<Preferences> by preferencesDataStore(name = "settings")
    private val dataStore get() = context.appPreferences
    private val data get() = dataStore.data

    val notification: Flow<Boolean> = combine(
        context.appPreferences.data,
        permissionManager.notificationPermissionFlow
    ) { settings, permission ->
        return@combine (settings[NOTIFICATION] ?: false) && permission
    }

    suspend fun setNotification(value: Boolean) = dataStore.edit { it[NOTIFICATION] = value }

    val modeAOD: Flow<Boolean> = data.map { it[MODE_AOD] ?: false }
    suspend fun setModeAOD(value: Boolean) = dataStore.edit { it[MODE_AOD] = value }

    val bigIcon: Flow<Boolean> = data.map { it[BIG_ICON] ?: false }
    suspend fun setBigIcon(value: Boolean) = dataStore.edit { it[BIG_ICON] = value }

    val speedBits: Flow<Boolean> = data.map { it[SPEED_BITS] ?: false }
    suspend fun setSpeedBits(value: Boolean) = dataStore.edit { it[SPEED_BITS] = value }

    val forceFallback: Flow<Boolean> = data.map { it[FORCE_FALLBACK] ?: false }
    suspend fun setForceFallback(value: Boolean) = dataStore.edit { it[FORCE_FALLBACK] = value }

    val altVpn: Flow<Boolean> = data.map { it[ALT_VPN_WORKAROUND] ?: false }
    suspend fun setAltVpn(value: Boolean) = dataStore.edit { it[ALT_VPN_WORKAROUND] = value }

    val liveNotification: Flow<Boolean> = data.map { it[LIVE_NOTIFICATION] ?: false }
    suspend fun setLiveNotification(value: Boolean) = dataStore.edit { it[LIVE_NOTIFICATION] = value }
    
    val theme: Flow<Theme> = data.map { prefs -> prefs[THEME]?.let { valueOfOrNull<Theme>(it) } ?: Theme.AutoMaterial }
    suspend fun setTheme(value: Theme) = dataStore.edit { it[THEME] = value.name }

    val shizukuTracking: Flow<Boolean> = data.map { it[SHIZUKU_TRACKING] ?: false }
    suspend fun setShizukuTracking(value: Boolean) = dataStore.edit { it[SHIZUKU_TRACKING] = value }

    val shizukuHint: Flow<Boolean> = data.map { it[SHIZUKU_HINT] ?: true }
    suspend fun setShizukuHint(value: Boolean) = dataStore.edit { it[SHIZUKU_HINT] = value }

    private companion object {
        private val NOTIFICATION = booleanPreferencesKey("notification")
        private val LIVE_NOTIFICATION = booleanPreferencesKey("live_notification")
        private val MODE_AOD = booleanPreferencesKey("mode_aod")
        private val BIG_ICON = booleanPreferencesKey("big_icon")
        private val SPEED_BITS = booleanPreferencesKey("speed_bits")
        private val FORCE_FALLBACK = booleanPreferencesKey("force_fallback")
        private val ALT_VPN_WORKAROUND = booleanPreferencesKey("alt_vpn")
        private val THEME = stringPreferencesKey("theme")
        private val SHIZUKU_TRACKING = booleanPreferencesKey("shizuku_tracking")
        private val SHIZUKU_HINT = booleanPreferencesKey("shizuku_hint")
    }
}