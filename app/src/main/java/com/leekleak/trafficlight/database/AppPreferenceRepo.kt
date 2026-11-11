package com.leekleak.trafficlight.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.leekleak.core.ui.theme.Theme
import com.leekleak.core.util.valueOfOrNull
import com.leekleak.trafficlight.ui.overview.OverviewRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

private val Context.appPreferences: DataStore<Preferences> by preferencesDataStore(name = "settings")

class AppPreferenceRepo (
    private val context: Context,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val dataStore get() = context.appPreferences
    private val data get() = dataStore.data

    val notification: Flow<Boolean> = data.map { it[NOTIFICATION] ?: false }.distinctUntilChanged()
    suspend fun setNotification(value: Boolean) = dataStore.edit { it[NOTIFICATION] = value }

    val modeAOD: Flow<Boolean> = data.map { it[MODE_AOD] ?: false }.distinctUntilChanged()
    suspend fun setModeAOD(value: Boolean) = dataStore.edit { it[MODE_AOD] = value }

    val speedBits: Flow<Boolean> = data.map { it[SPEED_BITS] ?: false }.distinctUntilChanged()
    suspend fun setSpeedBits(value: Boolean) = dataStore.edit { it[SPEED_BITS] = value }

    val separateUpDown: Flow<Boolean> = data.map { it[SEPARATE_UP_DOWN] ?: false }.distinctUntilChanged()
    suspend fun setSeparateUpDown(value: Boolean) = dataStore.edit { it[SEPARATE_UP_DOWN] = value }

    val forceFallback: Flow<Boolean> = data.map { it[FORCE_FALLBACK] ?: false }.distinctUntilChanged()
    suspend fun setForceFallback(value: Boolean) = dataStore.edit { it[FORCE_FALLBACK] = value }

    val liveNotification: Flow<Boolean> = data.map { it[LIVE_NOTIFICATION] ?: false }.distinctUntilChanged()
    suspend fun setLiveNotification(value: Boolean) = dataStore.edit { it[LIVE_NOTIFICATION] = value }

    val overlay: Flow<Boolean> = data.map { it[OVERLAY] ?: false }.distinctUntilChanged()
    suspend fun setOverlay(value: Boolean) = dataStore.edit { it[OVERLAY] = value }

    val speedThreshold: Flow<Boolean> = data.map { it[SPEED_THRESHOLD] ?: false }.distinctUntilChanged()
    suspend fun setSpeedThreshold(value: Boolean) = dataStore.edit { it[SPEED_THRESHOLD] = value }

    val speedThresholdKb: Flow<Long> = data.map { it[SPEED_THRESHOLD_KB] ?: -1L }.distinctUntilChanged()
    suspend fun setSpeedThresholdKb(value: Long) = dataStore.edit { it[SPEED_THRESHOLD_KB] = value }

    val speedMetric: Flow<Boolean> = data.map { it[SPEED_METRIC] ?: true }.distinctUntilChanged()
    suspend fun setSpeedMetric(value: Boolean) = dataStore.edit { it[SPEED_METRIC] = value }

    val sizeMetric: Flow<Boolean> = data.map { it[SIZE_METRIC] ?: false }.distinctUntilChanged()
    suspend fun setSizeMetric(value: Boolean) = dataStore.edit { it[SIZE_METRIC] = value }
    
    // important setting, so force load asap.
    val theme: StateFlow<Theme> = data
        .map { prefs -> prefs[THEME]?.let { valueOfOrNull<Theme>(it) } ?: Theme.AutoMaterial }
        .distinctUntilChanged()
        .stateIn(scope, SharingStarted.Eagerly, Theme.AutoMaterial)
    suspend fun setTheme(value: Theme) = dataStore.edit { it[THEME] = value.name }

    val blur: Flow<Boolean> = data.map { prefs -> prefs[BLUR] ?: true }.distinctUntilChanged()
    suspend fun setBlur(value: Boolean) = dataStore.edit { it[BLUR] = value }

    val shizukuTracking: Flow<Boolean> = data.map { it[SHIZUKU_TRACKING] ?: false }.distinctUntilChanged()
    suspend fun setShizukuTracking(value: Boolean) = dataStore.edit { it[SHIZUKU_TRACKING] = value }

    val ads: Flow<Boolean> = data.map { it[ADS] ?: false }.distinctUntilChanged()
    suspend fun setAds(value: Boolean) = dataStore.edit { it[ADS] = value }

    val overviewDataType: Flow<DataType> = data.map { prefs -> prefs[OVERVIEW_DATA_TYPE]?.let { valueOfOrNull<DataType>(it) } ?: DataType.Mobile }.distinctUntilChanged()
    suspend fun setOverviewDataType(value: DataType) = dataStore.edit { it[OVERVIEW_DATA_TYPE] = value.name }

    val overviewRange: Flow<OverviewRange> = data.map { prefs -> prefs[OVERVIEW_RANGE]?.let { valueOfOrNull<OverviewRange>(it) } ?: OverviewRange.Today }.distinctUntilChanged()
    suspend fun setOverviewRange(value: OverviewRange) = dataStore.edit { it[OVERVIEW_RANGE] = value.name }

    private companion object {
        private val NOTIFICATION = booleanPreferencesKey("notification")
        private val LIVE_NOTIFICATION = booleanPreferencesKey("live_notification")
        private val OVERLAY = booleanPreferencesKey("overlay")
        private val MODE_AOD = booleanPreferencesKey("mode_aod")
        private val SPEED_BITS = booleanPreferencesKey("speed_bits")
        private val SEPARATE_UP_DOWN = booleanPreferencesKey("separate_up_down")
        private val FORCE_FALLBACK = booleanPreferencesKey("force_fallback")
        private val SPEED_THRESHOLD = booleanPreferencesKey("speed_threshold")
        private val SPEED_THRESHOLD_KB = longPreferencesKey("speed_threshold_kb")
        private val SPEED_METRIC = booleanPreferencesKey("speed_metric")
        private val SIZE_METRIC = booleanPreferencesKey("size_metric")
        private val THEME = stringPreferencesKey("theme")
        private val BLUR = booleanPreferencesKey("blur")
        private val SHIZUKU_TRACKING = booleanPreferencesKey("shizuku_tracking")
        private val ADS = booleanPreferencesKey("supporter_ads")
        private val OVERVIEW_DATA_TYPE = stringPreferencesKey("overview_data_type")
        private val OVERVIEW_RANGE = stringPreferencesKey("overview_range")
    }
}