package com.safari.zyra.core.data.memory

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecretDetectorTest {
    @Test fun rejectsLabeledCredentialsAndLongCardLikeNumbers() {
        assertTrue(SecretDetector.looksSensitive("api_key=AIzaSyExampleCredentialValue123456789"))
        assertTrue(SecretDetector.looksSensitive("password: hunter2"))
        assertTrue(SecretDetector.looksSensitive("My password is hunter2"))
        assertTrue(SecretDetector.looksSensitive("AIzaSyExampleCredentialValue123456789"))
        assertTrue(SecretDetector.looksSensitive("4111 1111 1111 1111"))
    }

    @Test fun allowsOrdinaryPreferences() {
        assertFalse(SecretDetector.looksSensitive("Prefers concise Roman Urdu replies"))
        assertFalse(SecretDetector.looksSensitive("Uses Android and Arch Linux"))
    }
}
