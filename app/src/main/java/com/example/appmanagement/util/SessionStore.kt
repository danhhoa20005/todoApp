package com.example.appmanagement.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Backing DataStore instance used to persist session information
private val Context.dataStore by preferencesDataStore("session")

// Wrapper for reading and updating the logged in user id
class SessionStore(private val context: Context) {
    // Preference key representing the user identifier
    private val KEY_UID = longPreferencesKey("current_user_id")

    // Flow exposing the stored user identifier changes
    val userIdFlow: Flow<Long?> = context.dataStore.data.map { it[KEY_UID] }

    // Persist a new user identifier to the DataStore
    suspend fun setUserId(id: Long) = context.dataStore.edit { it[KEY_UID] = id }
    // Remove the stored user identifier from the DataStore
    suspend fun clear() = context.dataStore.edit { it.remove(KEY_UID) }
}
