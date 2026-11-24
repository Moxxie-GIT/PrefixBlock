package fr.moxxie.prefixblock.screening

import android.os.Build
import android.os.SystemClock
import android.telecom.Call
import android.telecom.CallScreeningService
import android.telecom.TelecomManager
import android.util.Log
import dagger.hilt.android.AndroidEntryPoint
import fr.moxxie.prefixblock.BuildConfig
import fr.moxxie.prefixblock.data.history.BlockedCallRepository
import fr.moxxie.prefixblock.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Service appelé par Telecom pour chaque appel, avant la sonnerie, quand PrefixBlock détient
 * le rôle ROLE_CALL_SCREENING (CLAUDE.md §5).
 *
 * Telecom n'appelle pas ce service pour les numéros des contacts (pas de READ_CONTACTS), ni pour
 * un appel déjà rejeté par la liste de blocage du système.
 */
@AndroidEntryPoint
class PrefixBlockScreeningService : CallScreeningService() {

    @Inject lateinit var screener: CallScreener
    @Inject lateinit var blockedCalls: BlockedCallRepository
    @Inject @ApplicationScope lateinit var appScope: CoroutineScope

    override fun onScreenCall(callDetails: Call.Details) {
        val startedAt = SystemClock.elapsedRealtime()
        try {
            val call = callDetails.toIncomingCall()
            FailOpenScreening.launch(
                scope = appScope,
                decide = { screener.screen(call) },
                respond = { verdict -> send(callDetails, verdict, startedAt) },
            )
        } catch (e: RuntimeException) {
            Log.w(TAG, "Filtrage impossible, appel autorisé", e)
            send(callDetails, ScreeningVerdict.Allow(AllowReason.ERROR), startedAt)
        }
    }

    private fun send(details: Call.Details, verdict: ScreeningVerdict, startedAt: Long) {
        try {
            respondToCall(details, verdict.toCallResponse())
        } catch (e: RuntimeException) {
            // Sans réponse, Telecom laisse sonner l'appel au bout de 5 s : toujours fail-open.
            Log.w(TAG, "Réponse à Telecom impossible", e)
            return
        }
        if (BuildConfig.DEBUG) {
            val elapsed = SystemClock.elapsedRealtime() - startedAt
            Log.d(TAG, "${verdict.describe()} en $elapsed ms")
        }
        if (verdict is ScreeningVerdict.Block) {
            appScope.launch {
                try {
                    blockedCalls.record(verdict.number, verdict.rule)
                } catch (e: Exception) {
                    Log.w(TAG, "Appel rejeté non enregistré dans l'historique", e)
                }
            }
        }
    }

    private companion object {
        const val TAG = "PrefixBlockScreening"
    }
}

private fun Call.Details.toIncomingCall(): IncomingCall {
    val presented = Build.VERSION.SDK_INT < Build.VERSION_CODES.R ||
        handlePresentation == TelecomManager.PRESENTATION_ALLOWED
    return IncomingCall(
        incoming = callDirection == Call.Details.DIRECTION_INCOMING,
        number = handle?.let { numberFromAddress(it.scheme, it.schemeSpecificPart) },
        numberPresented = presented,
    )
}

private fun ScreeningVerdict.toCallResponse(): CallScreeningService.CallResponse = when (this) {
    is ScreeningVerdict.Allow -> CallScreeningService.CallResponse.Builder().build()
    is ScreeningVerdict.Block -> CallScreeningService.CallResponse.Builder()
        .setDisallowCall(true)
        .setRejectCall(true)
        .setSkipNotification(true)
        // Sans effet pour une application choisie par l'utilisateur sur AOSP (CLAUDE.md §5).
        .setSkipCallLog(true)
        .build()
}

/** Résumé pour le journal de débogage, sans le numéro de l'appelant. */
private fun ScreeningVerdict.describe(): String = when (this) {
    is ScreeningVerdict.Allow -> "Autorisé ($reason${rule?.let { ", règle ${it.pattern}" } ?: ""})"
    is ScreeningVerdict.Block -> "Bloqué (règle ${rule.type} ${rule.pattern})"
}
