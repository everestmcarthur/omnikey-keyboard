package com.omnkey.keyboard.core.engine

import android.content.Context
import android.graphics.Color
import com.omnkey.keyboard.core.database.OmniKeyDatabase
import com.omnkey.keyboard.core.model.KeyboardTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class ThemeEngine(
    private val context: Context,
    private val database: OmniKeyDatabase
) {
    private val themeDao = database.themeDao()

    suspend fun initializeDefaultThemes() = withContext(Dispatchers.IO) {
        val existingThemes = themeDao.getAllThemes().first()
        if (existingThemes.isNotEmpty()) return@withContext

        val defaultThemes = listOf(
            createLightTheme(),
            createDarkTheme(),
            createMaterialYouTheme(),
            createNordTheme(),
            createMonokaiTheme(),
            createSolarizedDarkTheme(),
            createSolarizedLightTheme(),
            createDraculaTheme(),
            createGruvboxTheme(),
            createHighContrastTheme()
        )

        defaultThemes.forEach { themeDao.insertTheme(it) }
    }

    private fun createLightTheme() = KeyboardTheme(
        id = 1,
        name = "Light",
        isBuiltIn = true,
        backgroundColor = Color.parseColor("#F5F5F5"),
        keyBackgroundColor = Color.WHITE,
        keyPressedColor = Color.parseColor("#E0E0E0"),
        keyTextColor = Color.parseColor("#212121"),
        keySecondaryTextColor = Color.parseColor("#757575"),
        accentColor = Color.parseColor("#2196F3"),
        borderColor = Color.parseColor("#E0E0E0")
    )

    private fun createDarkTheme() = KeyboardTheme(
        id = 2,
        name = "Dark",
        isBuiltIn = true,
        backgroundColor = Color.parseColor("#121212"),
        keyBackgroundColor = Color.parseColor("#1E1E1E"),
        keyPressedColor = Color.parseColor("#2C2C2C"),
        keyTextColor = Color.parseColor("#E0E0E0"),
        keySecondaryTextColor = Color.parseColor("#A0A0A0"),
        accentColor = Color.parseColor("#BB86FC"),
        borderColor = Color.parseColor("#2C2C2C")
    )

    private fun createMaterialYouTheme() = KeyboardTheme(
        id = 3,
        name = "Material You",
        isBuiltIn = true,
        backgroundColor = Color.parseColor("#FEF7FF"),
        keyBackgroundColor = Color.parseColor("#FFFFFF"),
        keyPressedColor = Color.parseColor("#E8DEF8"),
        keyTextColor = Color.parseColor("#1C1B1F"),
        keySecondaryTextColor = Color.parseColor("#49454F"),
        accentColor = Color.parseColor("#6750A4"),
        borderColor = Color.parseColor("#CAC4D0"),
        keyCornerRadius = 16f,
        gradientKeys = true,
        gradientStartColor = Color.parseColor("#6750A4"),
        gradientEndColor = Color.parseColor("#9575DE")
    )

    private fun createNordTheme() = KeyboardTheme(
        id = 4,
        name = "Nord",
        isBuiltIn = true,
        backgroundColor = Color.parseColor("#2E3440"),
        keyBackgroundColor = Color.parseColor("#3B4252"),
        keyPressedColor = Color.parseColor("#434C5E"),
        keyTextColor = Color.parseColor("#ECEFF4"),
        keySecondaryTextColor = Color.parseColor("#D8DEE9"),
        accentColor = Color.parseColor("#88C0D0"),
        borderColor = Color.parseColor("#4C566A")
    )

    private fun createMonokaiTheme() = KeyboardTheme(
        id = 5,
        name = "Monokai",
        isBuiltIn = true,
        backgroundColor = Color.parseColor("#272822"),
        keyBackgroundColor = Color.parseColor("#3E3D32"),
        keyPressedColor = Color.parseColor("#49483E"),
        keyTextColor = Color.parseColor("#F8F8F2"),
        keySecondaryTextColor = Color.parseColor("#75715E"),
        accentColor = Color.parseColor("#F92672"),
        borderColor = Color.parseColor("#49483E")
    )

    private fun createSolarizedDarkTheme() = KeyboardTheme(
        id = 6,
        name = "Solarized Dark",
        isBuiltIn = true,
        backgroundColor = Color.parseColor("#002b36"),
        keyBackgroundColor = Color.parseColor("#073642"),
        keyPressedColor = Color.parseColor("#586e75"),
        keyTextColor = Color.parseColor("#839496"),
        keySecondaryTextColor = Color.parseColor("#657b83"),
        accentColor = Color.parseColor("#268bd2"),
        borderColor = Color.parseColor("#073642")
    )

    private fun createSolarizedLightTheme() = KeyboardTheme(
        id = 7,
        name = "Solarized Light",
        isBuiltIn = true,
        backgroundColor = Color.parseColor("#fdf6e3"),
        keyBackgroundColor = Color.parseColor("#eee8d5"),
        keyPressedColor = Color.parseColor("#93a1a1"),
        keyTextColor = Color.parseColor("#657b83"),
        keySecondaryTextColor = Color.parseColor("#839496"),
        accentColor = Color.parseColor("#268bd2"),
        borderColor = Color.parseColor("#eee8d5")
    )

    private fun createDraculaTheme() = KeyboardTheme(
        id = 8,
        name = "Dracula",
        isBuiltIn = true,
        backgroundColor = Color.parseColor("#282a36"),
        keyBackgroundColor = Color.parseColor("#44475a"),
        keyPressedColor = Color.parseColor("#6272a4"),
        keyTextColor = Color.parseColor("#f8f8f2"),
        keySecondaryTextColor = Color.parseColor("#6272a4"),
        accentColor = Color.parseColor("#bd93f9"),
        borderColor = Color.parseColor("#44475a")
    )

    private fun createGruvboxTheme() = KeyboardTheme(
        id = 9,
        name = "Gruvbox",
        isBuiltIn = true,
        backgroundColor = Color.parseColor("#282828"),
        keyBackgroundColor = Color.parseColor("#3c3836"),
        keyPressedColor = Color.parseColor("#504945"),
        keyTextColor = Color.parseColor("#ebdbb2"),
        keySecondaryTextColor = Color.parseColor("#a89984"),
        accentColor = Color.parseColor("#fe8019"),
        borderColor = Color.parseColor("#3c3836")
    )

    private fun createHighContrastTheme() = KeyboardTheme(
        id = 10,
        name = "High Contrast",
        isBuiltIn = true,
        backgroundColor = Color.BLACK,
        keyBackgroundColor = Color.WHITE,
        keyPressedColor = Color.parseColor("#FFFF00"),
        keyTextColor = Color.BLACK,
        keySecondaryTextColor = Color.parseColor("#404040"),
        accentColor = Color.parseColor("#FFFF00"),
        borderColor = Color.BLACK,
        keyBorderWidth = 2f
    )

    fun getAllThemes(): Flow<List<KeyboardTheme>> {
        return themeDao.getAllThemes()
    }

    suspend fun getTheme(id: Long): KeyboardTheme? {
        return themeDao.getThemeById(id)
    }

    suspend fun createTheme(theme: KeyboardTheme): Long {
        return themeDao.insertTheme(theme)
    }

    suspend fun updateTheme(theme: KeyboardTheme) {
        themeDao.updateTheme(theme)
    }

    suspend fun deleteTheme(theme: KeyboardTheme) {
        if (!theme.isBuiltIn) {
            themeDao.deleteTheme(theme)
        }
    }

    suspend fun duplicateTheme(theme: KeyboardTheme): Long {
        val newTheme = theme.copy(
            id = 0,
            name = "${theme.name} (Copy)",
            isBuiltIn = false,
            createdAt = System.currentTimeMillis(),
            modifiedAt = System.currentTimeMillis()
        )
        return themeDao.insertTheme(newTheme)
    }
}

suspend fun <T> Flow<T>.first(): T {
    var result: T? = null
    this.collect {
        result = it
        return@collect
    }
    return result!!
}
