package fr.moxxie.prefixblock

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import fr.moxxie.prefixblock.ui.PrefixBlockRoot
import fr.moxxie.prefixblock.ui.theme.PrefixBlockTheme

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            PrefixBlockTheme {
                PrefixBlockRoot()
            }
        }
    }
}
