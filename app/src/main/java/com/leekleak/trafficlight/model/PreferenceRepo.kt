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
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class PreferenceRepo (private val context: Context): KoinComponent {
    val permissionManager: PermissionManager by inject()
    val data get() = context.dataStore.data

    val notification: Flow<Boolean> = combine(
        context.dataStore.data,
        permissionManager.notificationPermissionFlow
    ) { settings, permission ->
        return@combine (settings[NOTIFICATION] ?: false) && permission
    }

    suspend fun setNotification(value: Boolean) = context.dataStore.edit { it[NOTIFICATION] = value }

    val modeAOD: Flow<Boolean> = data.map { it[MODE_AOD] ?: false }
    suspend fun setModeAOD(value: Boolean) = context.dataStore.edit { it[MODE_AOD] = value }

    val bigIcon: Flow<Boolean> = data.map { it[BIG_ICON] ?: false }
    suspend fun setBigIcon(value: Boolean) = context.dataStore.edit { it[BIG_ICON] = value }

    val speedBits: Flow<Boolean> = data.map { it[SPEED_BITS] ?: false }
    suspend fun setSpeedBits(value: Boolean) = context.dataStore.edit { it[SPEED_BITS] = value }

    val forceFallback: Flow<Boolean> = context.dataStore.data.map { it[FORCE_FALLBACK] ?: false }
    suspend fun setForceFallback(value: Boolean) = context.dataStore.edit { it[FORCE_FALLBACK] = value }

    val altVpn: Flow<Boolean> = data.map { it[ALT_VPN_WORKAROUND] ?: false }
    suspend fun setAltVpn(value: Boolean) = context.dataStore.edit { it[ALT_VPN_WORKAROUND] = value }

    val theme: Flow<Theme> = data.map { Theme.valueOf(it[THEME] ?: Theme.AutoMaterial.name ) }
    suspend fun setTheme(value: Theme) = context.dataStore.edit { it[THEME] = value.name }

    val expressiveFonts: Flow<Boolean> = context.dataStore.data.map { it[EXPRESSIVE_FONTS] ?: true }
    suspend fun setExpressiveFonts(value: Boolean) = context.dataStore.edit { it[EXPRESSIVE_FONTS] = value}

    private companion object {
        val NOTIFICATION = booleanPreferencesKey("notification")
        val MODE_AOD = booleanPreferencesKey("mode_aod")
        val BIG_ICON = booleanPreferencesKey("big_icon")
        val SPEED_BITS = booleanPreferencesKey("speed_bits")
        val FORCE_FALLBACK = booleanPreferencesKey("force_fallback")
        val ALT_VPN_WORKAROUND = booleanPreferencesKey("alt_vpn_workaround")
        val THEME = stringPreferencesKey("theme")
        val EXPRESSIVE_FONTS = booleanPreferencesKey("expressive_fonts")
    }
}