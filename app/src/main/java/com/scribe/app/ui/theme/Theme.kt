package com.scribe.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Mode clair imposé par défaut (cohérent avec toutes les instances SCRIBE).
private val ScribeLightColors = lightColorScheme(
    primary = BleuFrance,
    onPrimary = Blanc,
    secondary = BleuFranceClair,
    onSecondary = Blanc,
    error = RougeMarianne,
    onError = Blanc,
    background = FondClair,
    onBackground = TexteSombre,
    surface = Blanc,
    onSurface = TexteSombre,
    surfaceVariant = GrisLeger,
)

@Composable
fun ScribeTheme(
    // Volontairement ignoré : SCRIBE force le mode clair.
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = ScribeLightColors,
        typography = MaterialTheme.typography,
        content = content,
    )
}
