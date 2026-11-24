package fr.moxxie.prefixblock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import fr.moxxie.prefixblock.data.settings.SettingsRepository
import fr.moxxie.prefixblock.ui.PrefixBlockRoot
import fr.moxxie.prefixblock.ui.theme.PrefixBlockTheme
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var settings: SettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val dynamicColor by settings.dynamicColor.collectAsStateWithLifecycle(initialValue = false)
            PrefixBlockTheme(dynamicColor = dynamicColor) {
                PrefixBlockRoot()
            }
        }
    }
}
