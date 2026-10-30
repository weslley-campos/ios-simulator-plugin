package br.com.weslleycampos.simpanel.simulators

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SimulatorPickTest {
    private val pro = Simulator("B849", "iPhone 18 Pro", "iOS 27.0", booted = false)
    private val proMax = Simulator("770D", "iPhone 18 Pro Max", "iOS 27.0", booted = false)
    private val air = Simulator("A1R0", "iPhone Air", "iOS 27.0", booted = false)

    @Test
    fun keepsTheSimulatorShownLastWhileItsBooted() {
        val simulators = listOf(proMax.copy(booted = true), pro.copy(booted = true))

        assertEquals(pro.udid, pickSimulator(simulators, lastUdid = pro.udid)?.udid)
    }

    @Test
    fun showsABootedSimulatorOverTheOneShownLast() {
        val simulators = listOf(air.copy(booted = true), pro, proMax)

        assertEquals(air.udid, pickSimulator(simulators, lastUdid = pro.udid)?.udid)
    }

    @Test
    fun offersToBootTheSimulatorShownLastWhenNoneIsBooted() {
        assertEquals(proMax, pickSimulator(listOf(pro, proMax), lastUdid = proMax.udid))
    }

    @Test
    fun offersTheFirstSimulatorWhenNoneIsBootedOrWasShown() {
        assertEquals(pro, pickSimulator(listOf(pro, proMax), lastUdid = null))
        assertEquals(pro, pickSimulator(listOf(pro, proMax), lastUdid = "deleted"))
    }

    @Test
    fun picksNothingWithoutSimulators() {
        assertNull(pickSimulator(emptyList(), lastUdid = pro.udid))
    }
}
