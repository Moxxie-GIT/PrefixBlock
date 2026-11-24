package fr.moxxie.prefixblock.ui.components

import fr.moxxie.prefixblock.data.history.BlockedCall
import fr.moxxie.prefixblock.rules.PhoneNumberNormalizer

/** Appel rejeté prêt à afficher : numéro groupé (`+870 773 111 632`). */
data class DisplayedCall(val call: BlockedCall, val displayNumber: String)

fun BlockedCall.displayed(normalizer: PhoneNumberNormalizer) =
    DisplayedCall(this, normalizer.formatForDisplay(number))
