# Transactify — Cash Point Management System

Application Android (Kotlin natif) qui automatise la capture et la gestion des
transactions SMS Orange Money, Airtel Money et M-Vola pour les cash points à
Madagascar.

## Stack

- **Langage** : Kotlin
- **Architecture** : MVVM + Clean Architecture (`data` / `domain` / `ui`)
- **Persistance** : Room (SQLite)
- **Async** : Coroutines + Flow, WorkManager pour le traitement des SMS en tâche de fond
- **UI** : Material Components, Navigation Component, ViewBinding
- **Min SDK** : 26 (Android 8.0) — **Target SDK** : 35 (Android 15)

## Structure du projet

```
com.tinah.transactify/
├── data/
│   ├── db/            # Entités Room, DAOs, AppDatabase
│   ├── repository/     # TransactionRepository, ClientRepository
│   └── service/         # SMSBroadcastReceiver, SMSProcessingWorker,
│                          CashPointForegroundService, BootReceiver
├── domain/
│   └── model/, usecase/ # réservé aux phases suivantes
├── ui/
│   ├── dashboard/       # Tableau de bord (implémenté)
│   ├── transactions/    # Placeholder de navigation (Phase 7)
│   ├── clients/         # Placeholder de navigation (Phase 7)
│   ├── reports/         # Placeholder de navigation (Phase 7)
│   └── settings/        # Placeholder de navigation (Phase 7)
└── utils/               # SMSParser, BonusMatchingService
```

## Ce qui est implémenté (Phases 1-6 du prompt de développement)

- Squelette Gradle complet, dépendances, structure de packages, manifest, CI GitHub Actions
- Entités et DAOs Room pour `Transaction` et `Client`, `AppDatabase`, repositories
- Capture SMS en temps réel (`SMSBroadcastReceiver` → `WorkManager` → `SMSProcessingWorker`)
- `SMSParser` pour Orange Money / Airtel Money / M-Vola (montant, numéro, type, référence, bonus)
- `BonusMatchingService` : rattache les SMS bonus à la transaction mère et calcule le bénéfice
- Dashboard (total reçu / envoyé / bénéfice) avec `Flow`/`StateFlow`
- Persistance 24/7 : `CashPointForegroundService` (notification, `START_STICKY`, type `dataSync`) + `BootReceiver` + rattrapage périodique WorkManager
- Tests unitaires pour `SMSParser` et `BonusMatchingService`

Les fragments Transactions / Clients / Rapports / Paramètres sont des
placeholders de navigation : le prompt fourni ne détaille leur UI (listes,
export PDF/Excel, graphiques MPAndroidChart, taux de commission) que dans les
phases suivantes non couvertes par ce prompt.

## Build

```bash
./gradlew assembleDebug        # APK debug
./gradlew testDebugUnitTest    # tests unitaires
./gradlew lintDebug            # analyse statique
```

> Si `gradlew` / `gradle/wrapper/gradle-wrapper.jar` sont absents (premier
> clone d'un environnement sans réseau), régénérez-les une fois avec un Gradle
> local : `gradle wrapper --gradle-version 8.7`, puis commitez-les.

Room exporte le schéma versionné de la base dans `app/schemas/` (requis pour les
migrations et leurs tests).

## Permissions requises

`RECEIVE_SMS`, `READ_SMS`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`,
`INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`.
