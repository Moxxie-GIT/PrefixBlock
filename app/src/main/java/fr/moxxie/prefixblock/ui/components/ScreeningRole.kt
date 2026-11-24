package fr.moxxie.prefixblock.ui.components

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Retourne une action qui affiche la demande système « Définir PrefixBlock comme application
 * d'identification de l'appelant et de spam » (ROLE_CALL_SCREENING).
 *
 * Si Android ne peut pas afficher cette demande (rôle indisponible), ouvre les applications par
 * défaut. Après deux refus avec « Ne plus demander », Android rejette la demande sans rien
 * afficher : l'utilisateur passe alors par [openDefaultAppsSettings].
 *
 * @param onResult appelé au retour de la demande, qu'elle soit acceptée ou non.
 */
@Composable
fun rememberScreeningRoleRequest(onResult: () -> Unit): () -> Unit {
    val context = LocalContext.current
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        onResult()
    }
    return remember(context, launcher) {
        {
            val roleManager = context.getSystemService(RoleManager::class.java)
            val intent = roleManager
                ?.takeIf { it.isRoleAvailable(RoleManager.ROLE_CALL_SCREENING) }
                ?.createRequestRoleIntent(RoleManager.ROLE_CALL_SCREENING)
            if (intent != null) {
                try {
                    launcher.launch(intent)
                } catch (_: ActivityNotFoundException) {
                    context.openDefaultAppsSettings()
                }
            } else {
                context.openDefaultAppsSettings()
            }
        }
    }
}

/**
 * Ouvre les applications par défaut d'Android (sur Samsung : « Identification de l'appelant
 * et spam » s'y trouve), pour choisir ou retirer l'application de filtrage.
 */
fun Context.openDefaultAppsSettings() {
    try {
        startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
    } catch (_: ActivityNotFoundException) {
        startActivity(Intent(Settings.ACTION_SETTINGS))
    }
}
