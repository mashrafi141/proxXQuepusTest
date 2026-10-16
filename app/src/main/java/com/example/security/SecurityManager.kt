package com.example.security

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore(name = "proxxquepus_security")

class SecurityManager(private val context: Context) {

    companion object {
        private val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        private val KEY_LOCK_ENABLED = booleanPreferencesKey("app_lock_enabled")
        private val KEY_AUTO_LOCK = booleanPreferencesKey("auto_lock_enabled")
    }

    val isLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_LOCK_ENABLED] ?: false
    }

    val isAutoLockEnabled: Flow<Boolean> = context.dataStore.data.map { preferences ->
        preferences[KEY_AUTO_LOCK] ?: true
    }

    fun hasPinSet(): Flow<Boolean> = context.dataStore.data.map { preferences ->
        !preferences[KEY_PIN_HASH].isNullOrEmpty()
    }

    suspend fun setPin(pin: String) {
        val hash = hashPin(pin)
        context.dataStore.edit { preferences ->
            preferences[KEY_PIN_HASH] = hash
            preferences[KEY_LOCK_ENABLED] = true
        }
    }

    suspend fun removePin() {
        context.dataStore.edit { preferences ->
            preferences.remove(KEY_PIN_HASH)
            preferences[KEY_LOCK_ENABLED] = false
        }
    }

    suspend fun setLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_LOCK_ENABLED] = enabled
        }
    }

    suspend fun setAutoLockEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[KEY_AUTO_LOCK] = enabled
        }
    }

    suspend fun verifyPin(enteredPin: String): Boolean {
        val enteredHash = hashPin(enteredPin)
        var matches = false
        context.dataStore.edit { preferences ->
            val storedHash = preferences[KEY_PIN_HASH]
            matches = storedHash != null && storedHash == enteredHash
        }
        return matches
    }

    private fun hashPin(pin: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        val bytes = md.digest("proxxquepus_salt_$pin".toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
