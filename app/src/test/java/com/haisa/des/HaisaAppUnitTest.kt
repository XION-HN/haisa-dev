package com.haisa.des

import org.junit.Assert.assertNotNull
import org.junit.Test

class HaisaAppUnitTest {

    @Test
    fun haisaAppClass_exists() {
        assertNotNull(HaisaApp::class.java)
    }

    @Test
    fun basicArithmetic_verification() {
        assert(2 + 2 == 4)
    }
}