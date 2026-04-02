package com.leekleak.trafficlight.database

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.leekleak.trafficlight.model.AppManager
import com.leekleak.trafficlight.model.AppManager.Companion.allApp
import com.leekleak.trafficlight.ui.history.ListParam
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HistoryPreferenceRepo (
    private val context: Context,
    appManager: AppManager,
) {
    private val Context.historyPreferences: DataStore<Preferences> by preferencesDataStore(name = "history")
    private val dataStore get() = context.historyPreferences
    private val data get() = dataStore.data

    val listParam: Flow<ListParam> = dataStore.data.map { prefs ->
        prefs[LIST_PARAM]?.let { ListParam.valueOf(it) } ?: ListParam.AppList
    }
    suspend fun saveListParam(param: ListParam) {
        dataStore.edit { it[LIST_PARAM] = param.name }
    }

    val query1: Flow<UsageQuery> = data.map { prefs ->
        UsageQuery(
            dataType = prefs[QUERY1_TYPE]?.mapNotNull { DataType.entries.find { type -> type.internalName == it } } ?: listOf(Wifi),
            dataDirection = prefs[QUERY1_DIRECTION]?.let { DataDirection.entries[it] } ?: DataDirection.Bidirectional,
            dataUID = prefs[QUERY1_UID]?.let { appManager.getAppForUID(it) } ?: allApp
        )
    }
    val query2: Flow<UsageQuery> = data.map { prefs ->
        UsageQuery(
            dataType = prefs[QUERY2_TYPE]?.mapNotNull { DataType.entries.find { type -> type.internalName == it } } ?: listOf(Mobile),
            dataDirection = prefs[QUERY2_DIRECTION]?.let { DataDirection.entries[it] } ?: DataDirection.Bidirectional,
            dataUID = prefs[QUERY2_UID]?.let { appManager.getAppForUID(it) } ?: allApp
        )
    }

    suspend fun saveQuery(n: Int, query: UsageQuery) {
        dataStore.edit { prefs ->
            val typeKey = if (n == 1) QUERY1_TYPE else QUERY2_TYPE
            val directionKey = if (n == 1) QUERY1_DIRECTION else QUERY2_DIRECTION
            val uidKey = if (n == 1) QUERY1_UID else QUERY2_UID
            prefs[typeKey] = query.dataType.map { it.internalName }.toSet()
            prefs[directionKey] = query.dataDirection.ordinal
            prefs[uidKey] = query.dataUID.uid
        }
    }

    private companion object {
        val QUERY1_TYPE = stringSetPreferencesKey("query1_data_type")
        val QUERY2_TYPE = stringSetPreferencesKey("query2_data_type")
        val QUERY1_DIRECTION = intPreferencesKey("query1_direction")
        val QUERY2_DIRECTION = intPreferencesKey("query2_direction")
        val QUERY1_UID = intPreferencesKey("query1_uid")
        val QUERY2_UID = intPreferencesKey("query2_uid")
        val LIST_PARAM = stringPreferencesKey("list_param")
    }
}