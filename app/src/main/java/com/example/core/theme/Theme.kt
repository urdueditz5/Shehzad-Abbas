package com.example.core.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.core.i18n.LocalApnaStrings
import com.example.core.i18n.stringsFor
import com.example.core.preferences.AppLanguage
import com.example.core.preferences.ThemeMode

private val ApnaLightColorScheme = lightColorScheme(
    primary = DeepNavy,
    onPrimary = PureWhite,
    primaryContainer = Color(0xFFE3EBFF),
    onPrimaryContainer = DeepNavy,
    secondary = RoyalBlue,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFFDBE4FF),
    onSecondaryContainer = DeepNavy,
    tertiary = ElectricCyan,
    onTertiary = DeepNavy,
    tertiaryContainer = Color(0xFFD6F7FB),
    onTertiaryContainer = DeepNavy,
    background = LightBg,
    onBackground = DarkNavyText,
    surface = PureWhite,
    onSurface = DarkNavyText,
    surfaceVariant = Color(0xFFEAEEF6),
    onSurfaceVariant = MutedSlate,
    outline = SoftBorder,
    error = ErrorRed,
    onError = PureWhite
)

private val ApnaDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = DeepNavy,
    primaryContainer = Color(0xFF1D355E),
    onPrimaryContainer = DarkText,
    secondary = RoyalBlue,
    onSecondary = PureWhite,
    secondaryContainer = Color(0xFF1D2D56),
    onSecondaryContainer = DarkText,
    tertiary = ElectricCyan,
    onTertiary = DeepNavy,
    tertiaryContainer = Color(0xFF123B47),
    onTertiaryContainer = Color(0xFFA7F3FC),
    background = DarkBg,
    onBackground = DarkText,
    surface = DarkSurface,
    onSurface = DarkText,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkMuted,
    outline = Color(0xFF283655),
    error = ErrorRed,
    onError = PureWhite
)

val ApnaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun ApnaHubTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    language: AppLanguage = AppLanguage.ENGLISH,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colorScheme = if (darkTheme) ApnaDarkColorScheme else ApnaLightColorScheme
    val isUrdu = language == AppLanguage.URDU
    val layoutDirection = if (isUrdu) LayoutDirection.Rtl else LayoutDirection.Ltr
    val typography = buildApnaTypography(isUrdu = isUrdu)
    val strings = stringsFor(language)

    CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection,
        LocalApnaStrings provides strings
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = typography,
            shapes = ApnaShapes,
            content = content
        )
    }
}
