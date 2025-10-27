package com.example.appmanagement.util

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore("session")

class SessionStore(private val context: Context) {
    private val KEY_UID = longPreferencesKey("current_user_id")

    val userIdFlow: Flow<Long?> = context.dataStore.data.map { it[KEY_UID] }

    suspend fun setUserId(id: Long) = context.dataStore.edit { it[KEY_UID] = id }
    suspend fun clear() = context.dataStore.edit { it.remove(KEY_UID) }
}
