package com.ajinkyabadve.kmmmywatchlist.features.backup.screen

import com.ajinkyabadve.kmmmywatchlist.core.notification.FakeReminderScheduler
import com.ajinkyabadve.kmmmywatchlist.features.auth.repository.FakeSettings
import com.ajinkyabadve.kmmmywatchlist.features.backup.repository.BackupRepositoryImpl
import com.ajinkyabadve.kmmmywatchlist.features.movies.repository.FakeFavoriteCollectionRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.NotificationJobSync
import com.ajinkyabadve.kmmmywatchlist.features.notifications.ReleaseReminderCoordinator
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.FakeReleaseReminderRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FakeFavoritePersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.settings.repository.FakeNotificationSettingsRepository
import kotlinx.datetime.LocalDate

/** One device's worth of fakes behind a real [BackupRepositoryImpl] and [BackupScreenModel], shared
 *  by the screen-model and UI tests. [scheduleJobCalls] counts background-job (re)starts. */
class BackupTestFixture {
    val people = FakeFavoritePersonRepository()
    val collections = FakeFavoriteCollectionRepository()
    val reminders = FakeReleaseReminderRepository()
    val settings = FakeSettings()
    var scheduleJobCalls = 0
        private set

    val repository =
        BackupRepositoryImpl(
            favoritePersonRepository = people,
            favoriteCollectionRepository = collections,
            releaseReminderRepository = reminders,
            settings = settings,
            now = { BackupTestConstant.EXPORTED_AT },
        )

    val screenModel =
        BackupScreenModel(
            backupRepository = repository,
            releaseReminderRepository = reminders,
            releaseReminderCoordinator = ReleaseReminderCoordinator(reminders, FakeReminderScheduler()),
            notificationJobSync =
                NotificationJobSync(
                    notificationSettingsRepository = FakeNotificationSettingsRepository(),
                    releaseReminderRepository = reminders,
                    scheduleJob = { scheduleJobCalls++ },
                    cancelJob = {},
                ),
            today = { BackupTestConstant.TODAY },
        )
}

object BackupTestConstant {
    const val EXPORTED_AT = 1_000L
    val TODAY = LocalDate(2026, 10, 4)
    const val FILE_NAME = "mywatchlist-backup-2026-10-04.json"
    const val PERSON_ID = 10L
    const val PERSON_NAME = "Gary Oldman"
    const val COLLECTION_ID = 20L
    const val COLLECTION_NAME = "Mission: Impossible Collection"
    const val NOT_A_BACKUP = "not a backup"
}
