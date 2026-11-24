package com.example.core.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.apnaDataStore: DataStore<Preferences> by preferencesDataStore(name = "apnahub_prefs")

enum class AppLanguage(val code: String, val label: String) {
    ENGLISH("en", "English"),
    URDU("ur", "اردو"),
    ROMAN_URDU("roman_ur", "Roman Urdu");

    companion object {
        fun fromCode(code: String?): AppLanguage =
            entries.firstOrNull { it.code == code } ?: ENGLISH
    }
}

enum class ThemeMode(val code: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromCode(code: String?): ThemeMode =
            entries.firstOrNull { it.code == code } ?: SYSTEM
    }
}

data class UserPrefsState(
    val language: AppLanguage = AppLanguage.ENGLISH,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val notificationsEnabled: Boolean = true,
    val completedLessons: Set<String> = emptySet(),
    val quizBestScore: Int = 0,
    val quizzesTaken: Int = 0
)

class UserPreferencesRepository(private val context: Context) {
    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val COMPLETED_LESSONS = stringSetPreferencesKey("completed_lessons")
        val QUIZ_BEST_SCORE = intPreferencesKey("quiz_best_score")
        val QUIZZES_TAKEN = intPreferencesKey("quizzes_taken")
    }

    val prefsFlow: Flow<UserPrefsState> = context.apnaDataStore.data.map { prefs ->
        UserPrefsState(
            language = AppLanguage.fromCode(prefs[Keys.LANGUAGE]),
            themeMode = ThemeMode.fromCode(prefs[Keys.THEME_MODE]),
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            completedLessons = prefs[Keys.COMPLETED_LESSONS] ?: emptySet(),
            quizBestScore = prefs[Keys.QUIZ_BEST_SCORE] ?: 0,
            quizzesTaken = prefs[Keys.QUIZZES_TAKEN] ?: 0
        )
    }

    suspend fun setLanguage(language: AppLanguage) {
        context.apnaDataStore.edit { it[Keys.LANGUAGE] = language.code }
    }

    suspend fun setThemeMode(themeMode: ThemeMode) {
        context.apnaDataStore.edit { it[Keys.THEME_MODE] = themeMode.code }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.apnaDataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun toggleLessonCompleted(lessonId: String) {
        context.apnaDataStore.edit { prefs ->
            val current = prefs[Keys.COMPLETED_LESSONS] ?: emptySet()
            prefs[Keys.COMPLETED_LESSONS] =
                if (lessonId in current) current - lessonId else current + lessonId
        }
    }

    suspend fun recordQuizResult(scorePercent: Int) {
        context.apnaDataStore.edit { prefs ->
            val currentBest = prefs[Keys.QUIZ_BEST_SCORE] ?: 0
            val count = prefs[Keys.QUIZZES_TAKEN] ?: 0
            prefs[Keys.QUIZ_BEST_SCORE] = maxOf(currentBest, scorePercent)
            prefs[Keys.QUIZZES_TAKEN] = count + 1
        }
    }
}
