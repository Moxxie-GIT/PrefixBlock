package fr.moxxie.prefixblock.ui.components

import androidx.annotation.StringRes
import fr.moxxie.prefixblock.R
import fr.moxxie.prefixblock.rules.RuleType

/** Libellé affiché pour un type de règle. */
@get:StringRes
val RuleType.labelRes: Int
    get() = when (this) {
        RuleType.NUMBER -> R.string.rule_type_number
        RuleType.PREFIX -> R.string.rule_type_prefix
        RuleType.REGEX -> R.string.rule_type_regex
    }
