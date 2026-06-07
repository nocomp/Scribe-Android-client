package com.scribe.app

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.scribe.app.ui.navigation.ScribeNavHost
import com.scribe.app.ui.theme.ScribeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Données de crise sensibles : empêcher captures d'écran et aperçu
        // dans le sélecteur d'apps (exigence HDS/RGPD).
        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()
        setContent {
            ScribeTheme {
                ScribeNavHost()
            }
        }
    }
}
