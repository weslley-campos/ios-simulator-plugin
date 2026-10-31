package br.com.weslleycampos.simpanel.runs

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class RunTargetTest {
    private val udid = "770D07E6-559C-4029-BED8-98A0F81E9784"

    @Test
    fun readsTheSimulatorOfAnXcodeApplication() {
        assertEquals(udid, simulatorUdid("iphonesimulator_$udid"))
    }

    @Test
    fun readsTheSimulatorOfAKotlinMultiplatformApplication() {
        assertEquals(udid, simulatorUdid(udid))
        assertEquals(udid, simulatorUdid(udid.lowercase()))
    }

    @Test
    fun ignoresOtherTargets() {
        for (target in listOf("iphoneos_00008130-001A2D3C0E10001C", "emulator-5554", "watchsimulator_$udid", "")) {
            assertNull(simulatorUdid(target), target)
        }
    }
}
