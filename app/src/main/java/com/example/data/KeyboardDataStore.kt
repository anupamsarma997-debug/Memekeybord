package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.keyboardDataStore: DataStore<Preferences> by preferencesDataStore(name = "desi_keyboard_settings")

class KeyboardDataStore(private val context: Context) {

    companion object {
        val KEY_VIBRATION_ENABLED = booleanPreferencesKey("vibration_feedback_enabled")
        val KEY_AUDIO_PLAYBACK_ENABLED = booleanPreferencesKey("audio_playback_enabled")
        val KEY_DARK_THEME_ENABLED = booleanPreferencesKey("dark_theme_enabled")
    }

    val vibrationEnabledFlow: Flow<Boolean> = context.keyboardDataStore.data.map { preferences ->
        preferences[KEY_VIBRATION_ENABLED] ?: true
    }

    val audioPlaybackEnabledFlow: Flow<Boolean> = context.keyboardDataStore.data.map { preferences ->
        preferences[KEY_AUDIO_PLAYBACK_ENABLED] ?: true
    }

    val darkThemeEnabledFlow: Flow<Boolean> = context.keyboardDataStore.data.map { preferences ->
        preferences[KEY_DARK_THEME_ENABLED] ?: true
    }

    suspend fun setVibrationEnabled(enabled: Boolean) {
        context.keyboardDataStore.edit { preferences ->
            preferences[KEY_VIBRATION_ENABLED] = enabled
        }
    }

    suspend fun setAudioPlaybackEnabled(enabled: Boolean) {
        context.keyboardDataStore.edit { preferences ->
            preferences[KEY_AUDIO_PLAYBACK_ENABLED] = enabled
        }
    }

    suspend fun setDarkThemeEnabled(enabled: Boolean) {
        context.keyboardDataStore.edit { preferences ->
            preferences[KEY_DARK_THEME_ENABLED] = enabled
        }
    }
}
