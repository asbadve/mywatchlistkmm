package com.ajinkyabadve.kmmmywatchlist.features.backup.transfer

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class TransferTicketTest {
    private val secret = ByteArray(SECRET_SIZE) { (it * SECRET_STEP).toByte() }

    @Test
    fun testEncodeThenDecode_roundTrips() {
        val decoded = assertNotNull(TransferTicket.decode(TransferTicket(HOST, PORT, secret, EXPIRES_AT).encode()))

        assertEquals(HOST, decoded.host)
        assertEquals(PORT, decoded.port)
        assertContentEquals(secret, decoded.secret)
        assertEquals(EXPIRES_AT, decoded.expiresAtEpochSeconds)
    }

    @Test
    fun testDecode_rejectsAnythingThatIsNotOurTicket() {
        val valid = TransferTicket(HOST, PORT, secret, EXPIRES_AT).encode()
        listOf(
            URL,
            valid.replace(TransferTicketConstant.PREFIX, OTHER_PREFIX),
            valid.replace(HOST, NOT_AN_IP),
            valid.replace(";$PORT;", ";$BAD_PORT;"),
            TransferTicket(HOST, PORT, ByteArray(SHORT_SECRET_SIZE), EXPIRES_AT).encode(),
            valid.substringBeforeLast(TransferTicketConstant.SEPARATOR),
        ).forEach { assertNull(TransferTicket.decode(it), it) }
    }

    @Test
    fun testIsExpired_atAndAfterTheExpiry() {
        val ticket = TransferTicket(HOST, PORT, secret, EXPIRES_AT)

        assertFalse(ticket.isExpired(EXPIRES_AT - 1))
        assertTrue(ticket.isExpired(EXPIRES_AT))
    }

    @Test
    fun testShortCode_roundTripsUsingTheReceiversAddressPrefix() {
        val shortCode = TransferTicket(HOST, PORT, secret, EXPIRES_AT).toShortCode()

        assertTrue(SHORT_CODE_SHAPE.matches(shortCode), shortCode)
        val decoded = assertNotNull(TransferTicket.fromShortCode(shortCode, RECEIVER_IP))
        assertEquals(HOST, decoded.host)
        assertEquals(PORT, decoded.port)
        assertContentEquals(secret, decoded.secret)
    }

    @Test
    fun testShortCode_forgivesHowItWasTyped() {
        val shortCode = TransferTicket(HOST, PORT, secret, EXPIRES_AT).toShortCode()
        // Lowercase, no dashes, spaces, and O/I/L typed for 0/1.
        val typed =
            shortCode
                .replace(TransferTicketConstant.SHORT_CODE_GROUP_SEPARATOR, " ")
                .lowercase()
                .replace('0', 'o')
                .replace('1', 'l')

        assertContentEquals(secret, assertNotNull(TransferTicket.fromShortCode(typed, RECEIVER_IP)).secret)
    }

    @Test
    fun testShortCode_rejectsGarbageAndWrongLengths() {
        val shortCode = TransferTicket(HOST, PORT, secret, EXPIRES_AT).toShortCode()

        listOf(NOT_A_CODE, shortCode.dropLast(TRUNCATE_BY), shortCode + EXTRA, BAD_CHARACTERS).forEach {
            assertNull(TransferTicket.fromShortCode(it, RECEIVER_IP), it)
        }
    }

    private companion object {
        const val SECRET_SIZE = 8
        const val SECRET_STEP = 37
        const val SHORT_SECRET_SIZE = 4
        const val RECEIVER_IP = "192.168.1.50"
        val SHORT_CODE_SHAPE = Regex("^[0-9A-HJKMNP-TV-Z]{4}(-[0-9A-HJKMNP-TV-Z]{4}){4}$")
        const val NOT_A_CODE = "hello world"
        const val TRUNCATE_BY = 5
        const val EXTRA = "ABCD"
        const val BAD_CHARACTERS = "UUUU-UUUU-UUUU-UUUU-UUUU"
        const val HOST = "192.168.1.23"
        const val PORT = 40123
        const val BAD_PORT = 70000
        const val EXPIRES_AT = 1_791_000_000L
        const val URL = "https://example.com"
        const val OTHER_PREFIX = "otherapp1"
        const val NOT_AN_IP = "example.com"
    }
}
