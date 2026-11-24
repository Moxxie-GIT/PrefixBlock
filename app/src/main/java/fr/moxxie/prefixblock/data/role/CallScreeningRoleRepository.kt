package fr.moxxie.prefixblock.data.role

import android.app.role.RoleManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * État du rôle système « application de filtrage des appels » (ROLE_CALL_SCREENING).
 *
 * Android n'appelle le CallScreeningService de PrefixBlock que si l'utilisateur
 * a désigné l'application pour ce rôle. Sans lui, aucun appel n'est filtré.
 */
interface CallScreeningRoleRepository {
    /** `true` si PrefixBlock détient actuellement le rôle de filtrage des appels. */
    fun isRoleHeld(): Boolean
}

@Singleton
class AndroidCallScreeningRoleRepository @Inject constructor(
    @ApplicationContext context: Context,
) : CallScreeningRoleRepository {

    private val roleManager: RoleManager? = context.getSystemService(RoleManager::class.java)

    override fun isRoleHeld(): Boolean =
        roleManager?.isRoleHeld(RoleManager.ROLE_CALL_SCREENING) == true
}
