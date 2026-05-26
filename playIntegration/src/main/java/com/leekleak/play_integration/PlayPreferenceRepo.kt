package com.leekleak.play_integration

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val Context.playPreferences: DataStore<Preferences> by preferencesDataStore(name = "play_preferences")

class PlayPreferenceRepo (
    private val context: Context,
) {
    private val dataStore get() = context.playPreferences
    private val data get() = dataStore.data

    val reviewPromptStamp: Flow<Long> = data.map { it[REVIEW_PROMPT_STAMP] ?: 0 }.distinctUntilChanged()
    suspend fun setReviewPromptStamp(value: Long) = dataStore.edit { it[REVIEW_PROMPT_STAMP] = value }

    private companion object {
        private val REVIEW_PROMPT_STAMP = longPreferencesKey("review_prompt_stamp")
    }
}