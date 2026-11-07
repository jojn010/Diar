package com.example.diar

import org.junit.Assert.*
import org.junit.Test

class AuthErrorTest {
    @Test fun sameHttpStatusDistinguishesCredentialsConfirmationAndEmailDelivery() {
        assertEquals(R.string.cloud_invalid_credentials, SupabaseBackend.HttpFailure(400, "invalid_credentials").messageId())
        assertEquals(R.string.cloud_email_unconfirmed, SupabaseBackend.HttpFailure(400, "email_not_confirmed").messageId())
        assertEquals(R.string.cloud_email_delivery_setup, SupabaseBackend.HttpFailure(400, "email_address_not_authorized").messageId())
        assertEquals(R.string.cloud_rate_limit, SupabaseBackend.HttpFailure(429, "over_email_send_rate_limit").messageId())
    }
    @Test fun unknownCodesRemainGenericAndDoNotExposeServerContent() {
        val error = SupabaseBackend.HttpFailure(500, "untrusted server text")
        assertEquals(R.string.cloud_error, error.messageId())
        assertFalse(error.message!!.contains("untrusted"))
    }
}
