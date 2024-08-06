package com.ira.easytreat.utils

import android.content.Context
import android.content.SharedPreferences
class PreferenceManager {
    companion object {
        private const val PREF_NAME = "app_preferences"
        private const val KEY_LANGUAGE_STRING = "language"

        fun saveLanguage(context: Context, value: String) {
            val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            with(sharedPreferences.edit()) {
                putString(KEY_LANGUAGE_STRING, value)
                apply()
            }
        }

        fun getLanguage(context: Context, defaultValue: String): String {
            val sharedPreferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            return sharedPreferences.getString(KEY_LANGUAGE_STRING, defaultValue) ?: defaultValue
        }
    }
}