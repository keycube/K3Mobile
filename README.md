# K3AudioType

Application Android d'accessibilité pour l'entraînement à la frappe au clavier physique, conçue pour les personnes malvoyantes.

## Présentation

K3AudioType est née de l'idée de créer un outil d'entraînement à la frappe rapide sur téléphone, utilisable avec un clavier physique compact dans les transports en commun. En cours de développement, nous avons réalisé son potentiel pour les personnes en situation de handicap visuel : le retour audio complet (synthèse vocale, sons, vibrations) permet de s'entraîner sans regarder l'écran.

L'application propose deux modes d'utilisation :
- **Mode écran éteint** — pour les personnes malvoyantes, avec TTS, earcons et compte à rebours audio
- **Mode écran allumé** — pour les voyants, sans retour audio, démarrage immédiat

## Fonctionnalités

- Entraînement à la frappe avec clavier physique (Bluetooth ou USB-C)
- Synthèse vocale (TTS) pour la navigation et la dictée des phrases
- Sons de validation, suppression et victoire (earcons)
- Retour haptique (vibrations)
- Suivi des performances : vitesse (WPM), précision (%), durée
- Graphique de progression avec tendance sur les 10 dernières sessions
- Textes pré-intégrés : phrases courtes et histoires
- Ajout de textes personnalisés
- 3 langues : français, anglais, espagnol
- Choix de la voix TTS et de la vitesse de lecture
- Fonctionne écran éteint grâce au service d'accessibilité

## Technologies

| Technologie | Utilisation |
|------------|-------------|
| **Kotlin** | Langage principal |
| **Jetpack Compose** | Interface utilisateur |
| **Room (SQLite)** | Base de données locale (textes, sessions) |
| **Android TTS** | Synthèse vocale |
| **AccessibilityService** | Capture clavier écran éteint |
| **Foreground Service** | Maintien du processus écran éteint |
| **KDoc / Dokka** | Documentation du code |

## Prérequis

- Téléphone Android (API 26+, Android 8.0 minimum)
- Clavier physique Bluetooth ou USB-C
- Android Studio (pour la compilation)

## Installation

### Depuis l'APK

1. Téléchargez le fichier APK.
2. Installez-le sur votre téléphone (autoriser les sources inconnues si nécessaire)
3. Au premier lancement, l'app propose d'activer le service d'accessibilité (optionnel)

### Depuis les sources

```bash
git clone https://github.com/keycube/K3Mobile.git
cd K3Mobile
```

Ouvrez le projet dans Android Studio, puis Build → Build APK.

## Architecture

```
com.k3mobile.testk3
├── main/
│   ├── MainActivity.kt              # Point d'entrée, navigation Compose
│   ├── MainViewModel.kt             # ViewModel partagé (TTS, préférences, BDD)
│   ├── K3AccessibilityService.kt    # Capture clavier système (dead keys)
│   ├── K3AppState.kt                # Routage événements clavier (Channel)
│   ├── K3SoundManager.kt            # Earcons et vibrations
│   └── TypingForegroundService.kt   # Service maintien écran éteint
├── data/
│   ├── AppDatabase.kt               # Base Room + données initiales
│   ├── TextEntity.kt                # Entité texte
│   ├── SessionEntity.kt             # Entité session
│   └── TypingDao.kt                 # Requêtes SQL
└── ui/screens/
    ├── K3TopBar.kt                  # Barre de navigation réutilisable
    ├── HomeScreen.kt                # Écran d'accueil
    ├── CustomGameScreen.kt          # Configuration de partie
    ├── TextListScreen.kt            # Liste des textes
    ├── TypingScreen.kt              # Session de frappe + résultats
    ├── StatsScreen.kt               # Statistiques et historique
    ├── SettingsScreen.kt            # Menu paramètres
    ├── SoundScreen.kt               # Réglages audio/vibrations
    ├── LanguageScreen.kt            # Sélection de la langue
    ├── VoiceScreen.kt               # Sélection de la voix TTS
    └── AboutScreen.kt               # À propos / crédits
```

## Documentation

La documentation KDoc peut être générée avec Dokka :

```bash
# Windows (PowerShell)
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio\jbr"
.\gradlew.bat dokkaHtml
```

La documentation HTML sera disponible dans `app/build/dokka/html/index.html`.


# app

## Packages

| Name |
|---|
| [com.k3mobile.testk3.data](app/com.k3mobile.testk3.data/index.md) |
| [com.k3mobile.testk3.main](app/com.k3mobile.testk3.main/index.md) |
| [com.k3mobile.testk3.ui](app/com.k3mobile.testk3.ui/index.md) |
| [com.k3mobile.testk3.ui.screens](app/com.k3mobile.testk3.ui.screens/index.md) |
| [com.k3mobile.testk3.ui.theme](app/com.k3mobile.testk3.ui.theme/index.md) |
ding index.md…]()


## Auteur

**Mathis Marsande**
- BUT Informatique — IUT 2 Grenoble
- Mobilité internationale — UQAC (Université du Québec à Chicoutimi)

## Licence

Ce projet a été développé dans le cadre d'un projet universitaire.
