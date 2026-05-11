package com.haisa.des

import org.junit.Assert.assertEquals
import org.junit.Test

class HaisaAppUnitTest {

    @Test
    fun appConstants_areCorrect() {
        assertEquals("HaisaApp", HaisaApp.TAG)
    }

    @Test
    fun basicArithmetic_verification() {
        assertEquals(4, 2 + 2)
    }
}