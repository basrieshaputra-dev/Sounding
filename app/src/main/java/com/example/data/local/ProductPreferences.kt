package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.example.data.CalibrationEngine

class ProductPreferences(context: Context) {

    private val prefs: SharedPreferences? = runCatching {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }.onFailure { err ->
        Log.e(TAG, "Failed opening product SharedPreferences", err)
    }.getOrNull()

    fun loadFavoriteProducts(): Set<String> {
        return runCatching {
            val p = prefs ?: return@runCatching CalibrationEngine.DEFAULT_FAVORITE_PRODUCTS.toSet()
            val stored = runCatching {
                p.getStringSet(KEY_FAVORITE_PRODUCTS, emptySet())
                    ?.filterNotNull()
                    ?.toSet()
                    .orEmpty()
            }.getOrDefault(emptySet())
            val normalized = stored.map { CalibrationEngine.normalizeProductName(it) }.toSet()
            val isSeeded = runCatching {
                p.getBoolean(KEY_DEFAULT_FAVORITES_SEEDED_V1, false)
            }.getOrDefault(false)
            if (!isSeeded) {
                val merged = LinkedHashSet<String>(CalibrationEngine.DEFAULT_FAVORITE_PRODUCTS).apply {
                    addAll(normalized)
                }
                runCatching {
                    p.edit()
                        .putStringSet(KEY_FAVORITE_PRODUCTS, merged)
                        .putBoolean(KEY_DEFAULT_FAVORITES_SEEDED_V1, true)
                        .apply()
                }
                return@runCatching merged
            }
            if (normalized != stored) {
                saveFavoriteProducts(normalized)
            }
            normalized
        }.onFailure { err ->
            Log.w(TAG, "Failed loading favorite products; using defaults", err)
        }.getOrDefault(CalibrationEngine.DEFAULT_FAVORITE_PRODUCTS.toSet())
    }

    fun saveFavoriteProducts(favorites: Set<String>) {
        runCatching {
            val normalized = favorites.map { CalibrationEngine.normalizeProductName(it) }.toSet()
            prefs?.edit()
                ?.putStringSet(KEY_FAVORITE_PRODUCTS, normalized)
                ?.putBoolean(KEY_DEFAULT_FAVORITES_SEEDED_V1, true)
                ?.apply()
        }.onFailure { err ->
            Log.w(TAG, "Failed saving favorite products", err)
        }
    }

    fun loadDarkTheme(defaultValue: Boolean = false): Boolean {
        return runCatching {
            val p = prefs ?: return@runCatching defaultValue
            if (p.contains(KEY_DARK_THEME)) {
                p.getBoolean(KEY_DARK_THEME, defaultValue)
            } else {
                defaultValue
            }
        }.onFailure { err ->
            Log.w(TAG, "Failed loading dark theme preference; using default", err)
        }.getOrDefault(defaultValue)
    }

    fun saveDarkTheme(isDark: Boolean) {
        runCatching {
            prefs?.edit()
                ?.putBoolean(KEY_DARK_THEME, isDark)
                ?.apply()
        }.onFailure { err ->
            Log.w(TAG, "Failed saving dark theme preference", err)
        }
    }

    fun loadLastOperator(): String {
        return runCatching {
            prefs?.getString(KEY_LAST_OPERATOR, "").orEmpty()
        }.onFailure { err ->
            Log.w(TAG, "Failed loading last operator preference; using empty string", err)
        }.getOrDefault("")
    }

    fun saveLastOperator(operatorName: String) {
        runCatching {
            prefs?.edit()
                ?.putString(KEY_LAST_OPERATOR, operatorName.trim())
                ?.apply()
        }.onFailure { err ->
            Log.w(TAG, "Failed saving last operator preference", err)
        }
    }

    companion object {
        private const val TAG = "ProductPreferences"
        private const val PREFS_NAME = "sounding_product_prefs"
        private const val KEY_FAVORITE_PRODUCTS = "favorite_products"
        private const val KEY_DEFAULT_FAVORITES_SEEDED_V1 = "default_favorites_seeded_v1"
        private const val KEY_DARK_THEME = "dark_theme_enabled"
        private const val KEY_LAST_OPERATOR = "last_operator_name"
    }
}
