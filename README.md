# Transactify — Cash Point Management System

Application Android (Kotlin natif) qui automatise la capture et la gestion des
transactions SMS Orange Money, Airtel Money et M-Vola pour les cash points à
Madagascar.

## Stack

- **Langage** : Kotlin
- **Architecture** : MVVM + Clean Architecture (`data` / `domain` / `ui`), DI manuelle (`AppContainer`)
- **Persistance** : Room (SQLite) + DataStore Preferences (réglages)
- **Async** : Coroutines + Flow, WorkManager pour le traitement des SMS en tâche de fond
- **UI** : Material Components, Navigation Component, ViewBinding, MPAndroidChart
- **Export** : PDF (`android.graphics.pdf.PdfDocument`), Excel (Apache POI)
- **Min SDK** : 26 (Android 8.0) — **Target SDK** : 35 (Android 15)

## Structure du projet

```
com.tinah.transactify/
├── data/
│   ├── db/              # Entités Room, DAOs, AppDatabase, migrations
│   ├── datastore/        # PreferencesManager (taux, curseur SMS, service)
│   ├── repository/       # TransactionRepository, ClientRepository
│   └── service/          # SMSBroadcastReceiver, SMSProcessingWorker,
│                            CashPointForegroundService, BootReceiver, SmsWorkScheduler
├── domain/
│   ├── model/            # OperatorType, TransactionType, ClientClassification,
│   │                        CommissionRates, TransactionItem, ClientItem,
│   │                        TransactionSummary, ReportPeriod, ReportData
│   └── usecase/          # Un use case par action métier (voir ci-dessous)
├── ui/
│   ├── dashboard/         # Tableau de bord
│   ├── transactions/      # Liste filtrable + détail
│   ├── clients/           # Répertoire + détail
│   ├── reports/           # Synthèse, graphiques, export PDF/Excel
│   └── settings/          # Taux de commission, service, maintenance
├── di/                   # AppContainer (ServiceLocator)
└── utils/                # SMSParser, BonusMatchingService, DateUtils, MoneyFormatter, Constants
```

## Fonctionnalités implémentées

- **Capture SMS temps réel** : `SMSBroadcastReceiver` → `WorkManager` → `SMSProcessingWorker`,
  curseur persistant (pas de re-scan à chaque SMS), filet de rattrapage périodique (15 min)
- **`SMSParser`** : Orange Money / Airtel Money / M-Vola (montant, numéro, sens, référence, bonus)
- **Déduplication** : index unique en base + contrôle applicatif (opérateur, horodatage, montant, sens, numéro)
- **`BonusMatchingService`** : rattache un SMS bonus à sa transaction mère, recalcule le bénéfice et les stats client
- **Tableau de bord** : totaux, compteur du jour, répartition par opérateur, dernières transactions
- **Transactions** : liste filtrable par opérateur, détail avec correction manuelle du bénéfice et suppression
- **Clients** : répertoire trié par volume avec recherche, classification (VIP/Régulier/Ponctuel), détail avec historique et renommage
- **Rapports** : périodes prédéfinies, graphiques (barres/camembert), export PDF et Excel partageables
- **Paramètres** : taux de commission éditables, activation du service, recalcul des bénéfices, re-scan SMS
- **Persistance 24/7** : `CashPointForegroundService` (type `dataSync`) + `BootReceiver`, activable/désactivable

## Build

```bash
./gradlew assembleDebug        # APK debug
./gradlew testDebugUnitTest    # tests unitaires
./gradlew jacocoTestReport     # rapport de couverture (app/build/reports/jacoco/)
./gradlew lintDebug            # analyse statique
```

> Si `gradlew` / `gradle/wrapper/gradle-wrapper.jar` sont absents (premier
> clone d'un environnement sans réseau), régénérez-les une fois avec un Gradle
> local : `gradle wrapper --gradle-version 8.7`, puis commitez-les.

Room exporte le schéma versionné de la base dans `app/schemas/` (requis pour les
migrations et leurs tests).

### Tests et couverture

140 tests unitaires : `domain/model` (93 %), `domain/usecase` (83 %),
`data/repository` (79 %), `utils` (85 %) — DAO testés sur une vraie base
SQLite en mémoire (Robolectric), tous les ViewModels testés (`MainDispatcherRule`
pour `viewModelScope`), migration Room vérifiée sur une base réelle.

**Non couvert, volontairement** : `data/service` (`SMSBroadcastReceiver`,
`SMSProcessingWorker`, `CashPointForegroundService`) nécessiterait
`androidx.work:work-testing` + du mock `ContentResolver` — pas encore fait ;
`ui/*/Fragment` (liaison de vues Android, pas de logique à vérifier
unitairement) et les classes `Factory` de `ViewModelProvider` (wiring DI
trivial) sont explicitement exclus du rapport de couverture.

**Limitation d'outillage connue** : le rapport JaCoCo affiche 0 % pour
`data/db/dao` et `data/db/migration` alors que ce code est abondamment testé
(voir `TransactionDaoTest`, `ClientDaoTest`, `DatabaseMigrationsTest`) — JaCoCo
n'attribue pas correctement la couverture du code exécuté dans le classloader
sandboxé de Robolectric, limitation connue de leur interaction, pas un vrai
trou de test.

`ExportReportToPdfUseCase` n'a pas de test Robolectric : le shadow `PdfDocument`
de Robolectric 4.13 ne le supporte pas correctement (`document is closed!` dès
`startPage()`). Ce n'est pas un bug du code — API Android standard, à vérifier
manuellement sur appareil/émulateur.

## Permissions requises

`RECEIVE_SMS`, `READ_SMS`, `POST_NOTIFICATIONS`, `RECEIVE_BOOT_COMPLETED`,
`INTERNET`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_DATA_SYNC`.
