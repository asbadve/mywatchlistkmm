package com.ajinkyabadve.kmmmywatchlist.features.auth.repository

import com.ajinkyabadve.kmmmywatchlist.features.auth.model.UserSession
import com.ajinkyabadve.kmmmywatchlist.network.client.TmdbClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class AuthRepositoryTest {
    private val settings = FakeSettings()

    @BeforeTest
    fun setUp() {
        settings.clear()
    }

    @AfterTest
    fun tearDown() {
        settings.clear()
    }

    @Test
    fun testSaveAndClearSession() {
        val repository = AuthRepositoryImpl(settings = settings)
        assertNull(repository.getUserSession())

        val session =
            UserSession(
                sessionId = "session_123",
                accountId = 456L,
                username = "testuser",
                name = "Test User",
                avatarPath = "/avatar.jpg",
            )

        repository.saveSession(session)
        val loaded = repository.getUserSession()
        assertNotNull(loaded)
        assertEquals("session_123", loaded.sessionId)
        assertEquals(456L, loaded.accountId)
        assertEquals("testuser", loaded.username)

        repository.clearSession()
        assertNull(repository.getUserSession())
    }

    @Test
    fun testCreateRequestTokenSuccess() =
        kotlinx.coroutines.test.runTest {
            val mockEngine =
                MockEngine { request ->
                    assertEquals("/3/authentication/token/new", request.url.encodedPath)
                    respond(
                        content = """{"success":true,"expires_at":"2026-08-15 16:00:00 UTC","request_token":"token_abc"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val client = TmdbClient(engine = mockEngine)
            val repository = AuthRepositoryImpl(tmdbClient = client, settings = settings)

            val token = repository.createRequestToken()
            assertEquals("token_abc", token)
        }

    @Test
    fun testCreateSessionSuccess() =
        kotlinx.coroutines.test.runTest {
            val mockEngine =
                MockEngine { request ->
                    when (request.url.encodedPath) {
                        "/3/authentication/session/new" ->
                            respond(
                                content = """{"success":true,"session_id":"session_xyz"}""",
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        "/3/account" ->
                            respond(
                                content =
                                    """
                                    {"id":789,"name":"John Doe","username":"johndoe","avatar":{"tmdb":{"avatar_path":"/john.png"}}}
                                    """.trimIndent(),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json"),
                            )
                        else -> respond("", HttpStatusCode.NotFound)
                    }
                }
            val client = TmdbClient(engine = mockEngine)
            val repository = AuthRepositoryImpl(tmdbClient = client, settings = settings)

            val userSession = repository.createSession("token_abc")
            assertEquals("session_xyz", userSession.sessionId)
            assertEquals(789L, userSession.accountId)
            assertEquals("johndoe", userSession.username)
            assertEquals("John Doe", userSession.name)
            assertEquals("/john.png", userSession.avatarPath)
        }
}
