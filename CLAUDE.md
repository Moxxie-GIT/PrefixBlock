# PrefixBlock — instructions pour Claude

Application Android native (usage personnel, architecture prête pour une distribution publique)
qui **rejette les appels indésirables avant que le téléphone ne sonne** (surtout les appels
Wangiri : indicatifs internationaux exotiques, numéros changeants, sonnerie de quelques
secondes, personne au bout), **sans jamais gêner les appels légitimes de numéros inconnus**.
Le problème à résoudre est la nuisance des sonneries, pas le risque de rappel surtaxé.

## 1. Règles non négociables

1. **Fail-open** : erreur interne, délai dépassé, numéro absent ou illisible → l'appel passe.
2. **Ne jamais bloquer tous les inconnus.** La détection automatique observe et signale ;
   elle ne bloque jamais seule sans règle explicite ou validation de l'utilisateur.
3. **Confidentialité** : fonctionnement hors ligne, aucune transmission de numéro, pas de
   publicité, pas de télémétrie, pas de sauvegarde cloud. Données uniquement locales.
4. **Permissions minimales** : pas d'accès aux SMS, contacts ou journal d'appels sans
   justification technique **et** consentement explicite (opt-in).
5. **Ne jamais affirmer qu'une fonctionnalité marche sans l'avoir vérifiée** (build, tests,
   appareil réel). Dire explicitement ce qui n'a pas pu être vérifié.
6. **Avancer phase par phase** (§9), sans générer le projet d'un bloc. Ne pas passer à la
   phase suivante tant que la phase courante ne fonctionne pas.
7. Face à une limite Android : l'expliquer précisément et proposer une solution reposant sur
   les API officielles. Pas de contournement non documenté.

## 2. Environnement et commandes

- Windows 11, Android Studio 2026.1.1 (JDK 21 embarqué), SDK dans `%LOCALAPPDATA%\Android\Sdk`
  (plateformes jusqu'à 37.0). Aucun émulateur installé : tests réels sur le téléphone de
  l'utilisateur, un **Samsung Galaxy S26 Ultra** (SM-S948B, Android 17 / API 37, One UI 9.0,
  1440×3120), branché en débogage USB. Captures : `adb exec-out screencap -p > fichier.png`.
- `java`, `gradle` et `adb` ne sont pas dans le PATH :

```bash
export JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"
./gradlew assembleDebug          # APK : app/build/outputs/apk/debug/app-debug.apk
./gradlew test                   # tous les tests JVM (:core:rules + :app)
./gradlew :core:rules:test       # moteur de règles seul (rapide)
./gradlew lintDebug              # rapport : app/build/reports/lint-results-debug.txt
ADB="$LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
"$ADB" install -r app/build/outputs/apk/debug/app-debug.apk
"$ADB" logcat -s PrefixBlockScreening PrefixBlockDebug   # décisions du filtrage (debug, sans numéro)
```

Commandes de test sur appareil (**version debug uniquement**, `app/src/debug/`, réservées à adb par la
permission `DUMP`). Le résultat s'affiche dans `data="…"` :

```bash
R="-n fr.moxxie.prefixblock/.debug.DebugCommandReceiver -a fr.moxxie.prefixblock.debug"
MSYS_NO_PATHCONV=1 "$ADB" shell "am broadcast $R.SCREEN --es number '00870773111632'"  # décision simulée
"$ADB" shell am broadcast $R.LIST_RULES
"$ADB" shell am broadcast $R.ADD_RULE --es list BLACKLIST --es type NUMBER --es pattern +590690123456
"$ADB" shell am broadcast $R.DELETE_RULE --el id 12
```

## 3. Stack technique

| Élément | Choix |
|---|---|
| Langage / build | Kotlin 2.3.21, Gradle 9.4.1 (Kotlin DSL + `gradle/libs.versions.toml`) |
| Plugin Android | AGP 9.2.1 (Kotlin intégré : **pas** de plugin `org.jetbrains.kotlin.android`) |
| UI | Jetpack Compose (BOM 2026.09.00), Material 3 1.4.0, Navigation Compose 2.10 à routes typées |
| Architecture | MVVM : ViewModel + `StateFlow<UiState>`, Coroutines/Flow |
| DI | Hilt 2.60.1 via KSP 2.3.14 |
| Données | Room 2.8.5 (règles, historique), DataStore (préférences, à partir de la phase 3) |
| Numéros | Google libphonenumber 9.0.44 (métadonnées hors ligne, ~360 Ko, sans géocodeur) |
| Sérialisation | kotlinx-serialization 1.11.0 (routes, export JSON) |
| SDK | minSdk 29 (minimum pour `ROLE_CALL_SCREENING`), targetSdk 36, compileSdk 37 |

**Contraintes de versions** (détail en tête de `libs.versions.toml`) : AGP plafonné par la
version d'Android Studio ; Kotlin aligné sur Hilt et l'IDE ; Compose 1.13+ exige compileSdk 37.1,
donc AGP plus récent, donc mise à jour de Studio. Toujours revérifier ces couples avant de
monter une version.

## 4. Architecture

**`:core:rules`** — Kotlin/JVM pur, **aucune dépendance Android** (package `fr.moxxie.prefixblock.rules`).
Toute la logique de décision vit ici et se teste sur la JVM :
- `Rule`, `RuleList` (BLACKLIST/WHITELIST), `RuleType` (NUMBER/PREFIX/REGEX), `Decision` (Allow/Block).
- `PhoneNumberNormalizer` → `NormalizedNumber(raw, international, countryCode)`.
- `RuleValidator` : normalise la saisie (`00870` → `+870`), valide les regex, refuse une regex
  de blocage qui toucherait tous les pays.
- `RuleEngine` : instantané immuable et thread-safe (regex précompilées) appliquant le §7.
- `DefaultRules` : configuration initiale (§8).

**`:app`** — package `fr.moxxie.prefixblock` :
- `ui/` : écrans Compose + ViewModels. Convention : `XxxScreen` (avec état, récupère le
  ViewModel par `hiltViewModel()`) → `XxxContent` (sans état, prévisualisable).
  `ui/navigation/` (routes typées, barre de navigation), `ui/theme/`, `ui/components/`.
- `data/db/` : Room (`rules`, `blocked_calls`). Règles par défaut insérées dans `onCreate` de la
  base. Schéma exporté dans `app/schemas/` : **toute évolution passe par une migration**, jamais
  de migration destructive (elle effacerait les règles de l'utilisateur).
- `data/rules/` : `RuleRepository` (toute saisie validée par `RuleValidator` ; doublons refusés).
- `data/history/` : `BlockedCallRepository` (historique des appels rejetés).
- `data/region/` (pays du réseau pour la normalisation), `data/role/` (rôle de filtrage).
- `di/` : modules Hilt ; `@ApplicationScope` = portée de coroutines du processus.
- `screening/` : filtrage.
  - `PrefixBlockScreeningService` : seule classe liée à Telecom ; convertit `Call.Details` → `IncomingCall`.
  - `FailOpenScreening` : une seule réponse, autorisation si erreur ou au-delà de 2 s.
  - `CallScreener` : décision, sans Android.
  - `RuleEngineProvider` : moteur en mémoire, recompilé à chaque changement de règle.
- `app/src/debug/` : `DebugCommandReceiver` (commandes adb ci-dessus), absent de la release.

Conventions :
- Textes de l'interface dans `res/values/strings.xml` (français par défaut, `tools:locale="fr"`).
- Icônes : **Material Symbols en XML** dans `res/drawable/` (pas `material-icons-extended`, déprécié
  et lourd). Source : `https://raw.githubusercontent.com/google/material-design-icons/master/symbols/android/<nom>/materialsymbolsoutlined/<nom>[_fill1]_24px.xml`.
  Retirer l'attribut `android:tint="?attr/colorControlNormal"` (attribut AppCompat absent).
- Couleurs dynamiques (Material You) désactivées par défaut ; réglage utilisateur en phase 4.
- Pas de sauvegarde cloud : `allowBackup=false` + règles d'exclusion (`res/xml/`).

## 5. Filtrage des appels : contraintes Android

- L'utilisateur doit désigner PrefixBlock comme application de filtrage : `RoleManager`,
  `ROLE_CALL_SCREENING` (API 29+). Sur Samsung : Paramètres > Applications > Applications par
  défaut > Identification de l'appelant et spam.
- Service déclaré avec `android:permission="android.permission.BIND_SCREENING_SERVICE"` et le
  filtre d'intention `android.telecom.CallScreeningService`.
- `onScreenCall()` : répondre **une seule fois** par `respondToCall()` et vite. Telecom abandonne
  au bout de 5 s (`CALL_SCREENING_FILTER_TIMEOUT = 5000`), puis l'appel sonne. Budget interne
  court avec repli « autoriser ». Règles gardées en mémoire (regex précompilées).
- Ne filtrer que les appels entrants (`callDirection == DIRECTION_INCOMING`). Les appels sortants
  sont toujours autorisés.
- Numéro absent ou masqué : **toujours autorisé** (décision utilisateur ; Samsung propose déjà
  son propre blocage des numéros masqués).
- Rejet : `CallResponse.Builder().setDisallowCall(true).setRejectCall(true).setSkipNotification(true)
  .setSkipCallLog(true)`. L'utilisateur veut que les appels bloqués n'encombrent pas le journal Samsung.
  **Limite Android vérifiée dans AOSP** (`CallScreeningServiceFilter.disallowCall`) :
  `shouldAddToCallLog = !skipCallLog || packageType != CARRIER`. Pour une application choisie par
  l'utilisateur, l'appel est donc **toujours** inscrit au journal avec le type `BLOCKED_TYPE`.
  `setSkipCallLog(true)` est envoyé quand même (sans effet sur AOSP ; comportement Samsung à vérifier).
  **Décision de l'utilisateur (2026-11-24) : option A**, accepter ces entrées « bloqué » dans le journal.
  Pas de nettoyage du journal (il exigerait `READ_CALL_LOG`/`WRITE_CALL_LOG`) ; à rediscuter
  seulement si le rendu réel sur le S26 Ultra le justifie.
  **Vérifié sur le S26 Ultra (2026-11-24)** : Samsung ignore aussi `setSkipCallLog` (`SkipSuppressed`)
  et inscrit l'appel avec `CallType: BLOCKED`, `CallBlockReason: CALL_SCREENING_SERVICE`, `PrefixBlock`.
- Samsung exécute son propre filtre (`com.samsung.android.incallui/…CallScreeningServiceImpl`) **après**
  PrefixBlock : la protection anti-spam de l'application Téléphone n'est pas remplacée (vérifié).
- Temps de décision mesuré sur appareil, process déjà démarré : 2 à 3 ms (budget interne 2 s).
- Contacts (vérifié dans AOSP, `startFilterLookup`) : sans `READ_CONTACTS`, Telecom n'appelle pas
  PrefixBlock pour un numéro présent dans les contacts. Ne pas demander cette permission.
  Telecom ne l'appelle pas non plus si un filtre précédent (liste de blocage Android/Samsung) a
  déjà rejeté l'appel.
- Limite connue : avant le premier déverrouillage après un redémarrage, le stockage chiffré
  (Room) est inaccessible. Le filtrage n'est alors pas appliqué (fail-open).
- La durée d'un appel n'est pas fournie par `CallScreeningService`. La mesurer demanderait
  `READ_CALL_LOG` (opt-in justifié seulement, cf. règle 4).

## 6. Normalisation des numéros

- libphonenumber avec la région par défaut du réseau (`TelephonyManager.networkCountryIso`,
  sans permission), puis celle de la SIM, puis la locale.
- Retirer les séparateurs. `+…` → E.164 ; `00…` ou national → analyse dans la région par défaut.
  En cas d'échec : garder la forme chiffres seuls, sans deviner l'indicatif.
- Les préfixes se comparent aux chiffres E.164. Les regex s'appliquent à la chaîne E.164 (avec `+`).
- **+1 n'est pas « les États-Unis »** : c'est le plan de numérotation nord-américain (États-Unis,
  Canada et ~20 pays des Caraïbes, dont +1 876 Jamaïque et +1 809 République dominicaine,
  fréquents en Wangiri).

## 7. Moteur de priorité (validé par l'utilisateur le 2026-11-24)

Évaluation dans l'ordre ; le premier niveau qui produit une correspondance décide :

1. Liste blanche, **numéro complet** → autorisé (priorité absolue).
2. Liste noire, numéro complet → bloqué.
3. Règles marquées « prioritaires » (préfixe ou regex) → appliquées avant les autres.
4. Préfixes : **le plus long (le plus précis) l'emporte** ; à longueur égale, l'autorisation l'emporte.
5. Regex : une autorisation l'emporte sur un blocage.
6. Aucune correspondance → autorisé.

Exemple : avec +1 en liste blanche et +1876 en liste noire, +1 876 … est bloqué et +1 212 … passe.

## 8. Fonctionnalités (cahier des charges)

- **Liste noire initiale** : +870 Inmarsat, +674 Nauru, +960 Maldives, +685 Samoa, +683 Niue.
- **Liste blanche initiale** (préfixes) : +33 France, +590 Guadeloupe / Saint-Barthélemy /
  Saint-Martin, +1 NANP. **+44 ne doit jamais être bloqué globalement.**
- **Règles** : ajout, modification, suppression ; types numéro complet / préfixe / regex
  (regex validée à la saisie) ; activation individuelle ; règles prioritaires. La liste
  blanche distingue numéros complets et préfixes.
- **Historique** (Room) : numéro, indicatif détecté, date et heure, règle déclenchée, type de
  règle. Suppression unitaire et totale, recherche, filtre par date, ajout direct d'un numéro
  ou préfixe en liste blanche. Aucune donnée superflue.
- **Détection** (observation seulement) : nombreux numéros différents sur un même préfixe,
  appels répétés, séquences de numéros proches. Signale et propose une règle ; ne bloque jamais seule.
- **Statistiques** : bloqués aujourd'hui / cette semaine / ce mois-ci, préfixes les plus
  bloqués, derniers appels rejetés.
- **Export/import JSON versionné** (phase 4). Format v1 retenu, typé pour distinguer numéro
  et préfixe et pour porter la priorité :

```json
{
  "format": "prefixblock-rules",
  "version": 1,
  "rules": [
    { "list": "BLACKLIST", "type": "PREFIX", "pattern": "+870", "label": "Inmarsat", "enabled": true, "priority": false },
    { "list": "WHITELIST", "type": "NUMBER", "pattern": "+590690000000", "enabled": true, "priority": false },
    { "list": "BLACKLIST", "type": "REGEX", "pattern": "^\\+4475\\d{8}$", "enabled": false, "priority": false }
  ]
}
```

- **Écrans** (barre de navigation, 5 entrées) : Accueil (état de la protection, compteurs,
  derniers appels, accès rapide), Règles (onglets Liste noire / Liste blanche), Historique,
  Détection, Paramètres (protection, permissions, export/import, à propos). Interface Material 3
  sobre, compatible avec les Samsung Galaxy récents.

## 9. Phases et état d'avancement

| Phase | Contenu | État |
|---|---|---|
| 1. Fondation | Projet, Gradle, MVVM, Navigation Compose, thème Material 3 | **Terminée** : build, tests, lint ; 5 écrans vérifiés sur le S26 Ultra (2026-11-24) |
| 2. Moteur de règles | Module `:core:rules`, normalisation, Room, listes noire/blanche, moteur de priorité, tests | **Terminée** : 65 tests, lint ; sur le S26 Ultra, base créée avec les 8 règles par défaut, activation persistante après redémarrage (2026-11-24) |
| 3. Filtrage | `CallScreeningService`, manifeste, demande du rôle, rejet, **test réel sur appareil** | **Terminée** : 89 tests, lint OK ; sur le S26 Ultra (2026-11-24), vrai appel rejeté sans sonnerie (l'appelant entend une sonnerie puis « occupé ») et appel autorisé qui sonne normalement |
| 4. Interface complète | Accueil, gestion des règles, historique, paramètres, export/import | À faire |
| 5. Détection | Répétitions, campagnes, statistiques, mode observation | À faire |
| 6. Finalisation | Tests, corrections, R8, optimisation, APK de release signé | À faire |

Un commit par phase validée (demande de l'utilisateur).

## 10. Qualité

- Séparation stricte UI / logique métier / données ; code modulaire, KDoc sur les composants importants.
- Tests unitaires : moteur de règles, normalisation, priorité liste blanche/noire, régressions ;
  ViewModels testés avec de faux dépôts.
- Gestion d'erreurs explicite. Le moins de dépendances possible.
