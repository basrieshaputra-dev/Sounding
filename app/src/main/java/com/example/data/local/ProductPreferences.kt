package com.example.data.local

import android.content.Context
import com.example.data.CalibrationEngine

class ProductPreferences(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun loadFavoriteProducts(): Set<String> {
        val stored = prefs.getStringSet(KEY_FAVORITE_PRODUCTS, emptySet())
            ?.toSet()
            .orEmpty()
        val normalized = stored.map { CalibrationEngine.normalizeProductName(it) }.toSet()
        val isSeeded = prefs.getBoolean(KEY_DEFAULT_FAVORITES_SEEDED_V1, false)
        if (!isSeeded) {
            val merged = LinkedHashSet<String>(CalibrationEngine.DEFAULT_FAVORITE_PRODUCTS).apply {
                addAll(normalized)
            }
            prefs.edit()
                .putStringSet(KEY_FAVORITE_PRODUCTS, merged)
                .putBoolean(KEY_DEFAULT_FAVORITES_SEEDED_V1, true)
                .apply()
            return merged
        }
        if (normalized != stored) {
            saveFavoriteProducts(normalized)
        }
        return normalized
    }

    fun saveFavoriteProducts(favorites: Set<String>) {
        val normalized = favorites.map { CalibrationEngine.normalizeProductName(it) }.toSet()
        prefs.edit()
            .putStringSet(KEY_FAVORITE_PRODUCTS, normalized)
            .putBoolean(KEY_DEFAULT_FAVORITES_SEEDED_V1, true)
            .apply()
    }

    fun loadDarkTheme(defaultValue: Boolean = false): Boolean {
        return if (prefs.contains(KEY_DARK_THEME)) {
            prefs.getBoolean(KEY_DARK_THEME, defaultValue)
        } else {
            defaultValue
        }
    }

    fun saveDarkTheme(isDark: Boolean) {
        prefs.edit()
            .putBoolean(KEY_DARK_THEME, isDark)
            .apply()
    }

    fun loadLastOperator(): String {
        return prefs.getString(KEY_LAST_OPERATOR, "").orEmpty()
    }

    fun saveLastOperator(operatorName: String) {
        prefs.edit()
            .putString(KEY_LAST_OPERATOR, operatorName.trim())
            .apply()
    }

    companion object {
        private const val PREFS_NAME = "sounding_product_prefs"
        private const val KEY_FAVORITE_PRODUCTS = "favorite_products"
        private const val KEY_DEFAULT_FAVORITES_SEEDED_V1 = "default_favorites_seeded_v1"
        private const val KEY_DARK_THEME = "dark_theme_enabled"
        private const val KEY_LAST_OPERATOR = "last_operator_name"
    }
}
