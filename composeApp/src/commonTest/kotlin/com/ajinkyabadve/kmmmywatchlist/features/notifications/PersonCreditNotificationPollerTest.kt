package com.ajinkyabadve.kmmmywatchlist.features.notifications

import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.FakeNotificationLedgerRepository
import com.ajinkyabadve.kmmmywatchlist.features.notifications.repository.NotificationReason
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonCombinedCredits
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonCredit
import com.ajinkyabadve.kmmmywatchlist.features.person.model.PersonDetail
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.FakeFavoritePersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.repository.PersonRepository
import com.ajinkyabadve.kmmmywatchlist.features.person.screen.FakePersonRepository
import io.ktor.utils.io.errors.IOException
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private object PersonCreditNotificationPollerTestConstant {
    const val PERSON_ID = 701L
    const val PERSON_NAME = "Fake Favorite Actor"
}

class PersonCreditNotificationPollerTest {
    private fun buildPoller(
        favoritePersonRepository: FakeFavoritePersonRepository,
        personRepository: PersonRepository,
        notificationLedgerRepository: FakeNotificationLedgerRepository,
    ) = PersonCreditNotificationPoller(
        favoritePersonRepository = favoritePersonRepository,
        personRepository = personRepository,
        notificationLedgerRepository = notificationLedgerRepository,
        now = { 0L },
    )

    /** The first poll after following someone must never notify for their existing filmography -
     *  only baseline it - or following a prolific actor would flood the user with one notification
     *  per past credit. */
    @Test
    fun testFirstPollAfterFollowingSeedsBaselineWithoutNotifying() =
        runTest {
            val favoritePersonRepository = FakeFavoritePersonRepository()
            favoritePersonRepository.seedFavorite(PersonCreditNotificationPollerTestConstant.PERSON_ID)
            val personRepository = FakePersonRepository()
            personRepository.getPersonDetailsResult =
                Result.success(
                    PersonDetail(
                        id = PersonCreditNotificationPollerTestConstant.PERSON_ID,
                        name = PersonCreditNotificationPollerTestConstant.PERSON_NAME,
                        combinedCredits =
                            PersonCombinedCredits(
                                cast =
                                    listOf(
                                        PersonCredit(id = 1, creditId = "credit_1"),
                                        PersonCredit(id = 2, creditId = "credit_2"),
                                        PersonCredit(id = 3, creditId = "credit_3"),
                                    ),
                            ),
                    ),
                )
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(favoritePersonRepository, personRepository, notificationLedgerRepository)

            poller.poll()

            assertTrue(notificationLedgerRepository.recordNotifiedCalls.isEmpty())
            assertEquals(
                listOf(PersonCreditNotificationPollerTestConstant.PERSON_ID to "credit_1,credit_2,credit_3"),
                favoritePersonRepository.updateLastKnownCreditIdsCalls,
            )
        }

    @Test
    fun testCreditAddedAfterBaselineNotifiesOnce() =
        runTest {
            val favoritePersonRepository = FakeFavoritePersonRepository()
            favoritePersonRepository.seedFavorite(PersonCreditNotificationPollerTestConstant.PERSON_ID, lastKnownCreditIds = "credit_1")
            val personRepository = FakePersonRepository()
            personRepository.getPersonDetailsResult =
                Result.success(
                    PersonDetail(
                        id = PersonCreditNotificationPollerTestConstant.PERSON_ID,
                        name = PersonCreditNotificationPollerTestConstant.PERSON_NAME,
                        combinedCredits =
                            PersonCombinedCredits(
                                cast = listOf(PersonCredit(id = 1, creditId = "credit_1"), PersonCredit(id = 2, creditId = "credit_2")),
                            ),
                    ),
                )
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(favoritePersonRepository, personRepository, notificationLedgerRepository)

            poller.poll()

            assertEquals(1, notificationLedgerRepository.recordNotifiedCalls.size)
            assertEquals(NotificationReason.PERSON_NEW_CREDIT, notificationLedgerRepository.recordNotifiedCalls.single().reason)
        }

    @Test
    fun testRepollingSameCreditsDoesNotReNotify() =
        runTest {
            val favoritePersonRepository = FakeFavoritePersonRepository()
            favoritePersonRepository.seedFavorite(PersonCreditNotificationPollerTestConstant.PERSON_ID, lastKnownCreditIds = "credit_1")
            val personRepository = FakePersonRepository()
            personRepository.getPersonDetailsResult =
                Result.success(
                    PersonDetail(
                        id = PersonCreditNotificationPollerTestConstant.PERSON_ID,
                        name = PersonCreditNotificationPollerTestConstant.PERSON_NAME,
                        combinedCredits =
                            PersonCombinedCredits(
                                cast = listOf(PersonCredit(id = 1, creditId = "credit_1"), PersonCredit(id = 2, creditId = "credit_2")),
                            ),
                    ),
                )
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(favoritePersonRepository, personRepository, notificationLedgerRepository)

            poller.poll()
            poller.poll()

            assertEquals(1, notificationLedgerRepository.recordNotifiedCalls.size)
        }

    @Test
    fun testASecondNewCreditNotifiesAgain() =
        runTest {
            val favoritePersonRepository = FakeFavoritePersonRepository()
            favoritePersonRepository.seedFavorite(PersonCreditNotificationPollerTestConstant.PERSON_ID, lastKnownCreditIds = "credit_1")
            val personRepository = FakePersonRepository()
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(favoritePersonRepository, personRepository, notificationLedgerRepository)

            personRepository.getPersonDetailsResult =
                Result.success(
                    PersonDetail(
                        id = PersonCreditNotificationPollerTestConstant.PERSON_ID,
                        name = PersonCreditNotificationPollerTestConstant.PERSON_NAME,
                        combinedCredits =
                            PersonCombinedCredits(
                                cast = listOf(PersonCredit(id = 1, creditId = "credit_1"), PersonCredit(id = 2, creditId = "credit_2")),
                            ),
                    ),
                )
            poller.poll()

            personRepository.getPersonDetailsResult =
                Result.success(
                    PersonDetail(
                        id = PersonCreditNotificationPollerTestConstant.PERSON_ID,
                        name = PersonCreditNotificationPollerTestConstant.PERSON_NAME,
                        combinedCredits =
                            PersonCombinedCredits(
                                cast =
                                    listOf(
                                        PersonCredit(id = 1, creditId = "credit_1"),
                                        PersonCredit(id = 2, creditId = "credit_2"),
                                        PersonCredit(id = 3, creditId = "credit_3"),
                                    ),
                            ),
                    ),
                )
            poller.poll()

            assertEquals(2, notificationLedgerRepository.recordNotifiedCalls.size)
        }

    @Test
    fun testPerPersonExceptionDoesNotStopPollingTheRestOfTheBatch() =
        runTest {
            val failingId = 1L
            val healthyId = 2L
            val favoritePersonRepository = FakeFavoritePersonRepository()
            favoritePersonRepository.seedFavorite(failingId, lastKnownCreditIds = "")
            favoritePersonRepository.seedFavorite(healthyId, lastKnownCreditIds = "")
            val personRepository =
                object : PersonRepository by FakePersonRepository() {
                    override suspend fun getPersonDetails(personId: Long): PersonDetail {
                        if (personId == failingId) throw IOException("network down")
                        return PersonDetail(
                            id = personId,
                            name = PersonCreditNotificationPollerTestConstant.PERSON_NAME,
                            combinedCredits = PersonCombinedCredits(cast = listOf(PersonCredit(id = 1, creditId = "credit_1"))),
                        )
                    }
                }
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(favoritePersonRepository, personRepository, notificationLedgerRepository)

            poller.poll()

            assertEquals(1, notificationLedgerRepository.recordNotifiedCalls.size)
            assertTrue(notificationLedgerRepository.recordNotifiedCalls.single().id == healthyId.toInt())
        }

    /** The debug "force a test notification" path must notify once per favorited person, not once
     *  per credit - unlike a blanket reset to "", which would flood anyone with a real filmography. */
    @Test
    fun testSeedOneNewCreditForDebugThenPollNotifiesExactlyOncePerPerson() =
        runTest {
            val favoritePersonRepository = FakeFavoritePersonRepository()
            favoritePersonRepository.seedFavorite(PersonCreditNotificationPollerTestConstant.PERSON_ID, lastKnownCreditIds = "credit_1")
            val personRepository = FakePersonRepository()
            personRepository.getPersonDetailsResult =
                Result.success(
                    PersonDetail(
                        id = PersonCreditNotificationPollerTestConstant.PERSON_ID,
                        name = PersonCreditNotificationPollerTestConstant.PERSON_NAME,
                        combinedCredits =
                            PersonCombinedCredits(
                                cast =
                                    listOf(
                                        PersonCredit(id = 1, creditId = "credit_1"),
                                        PersonCredit(id = 2, creditId = "credit_2"),
                                        PersonCredit(id = 3, creditId = "credit_3"),
                                        PersonCredit(id = 4, creditId = "credit_4"),
                                    ),
                            ),
                    ),
                )
            val notificationLedgerRepository = FakeNotificationLedgerRepository()
            val poller = buildPoller(favoritePersonRepository, personRepository, notificationLedgerRepository)

            poller.seedOneNewCreditForDebug()
            poller.poll()

            assertEquals(1, notificationLedgerRepository.recordNotifiedCalls.size)
        }
}
