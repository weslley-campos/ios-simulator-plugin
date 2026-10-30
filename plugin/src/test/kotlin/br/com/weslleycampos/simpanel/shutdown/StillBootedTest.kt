package br.com.weslleycampos.simpanel.shutdown

import br.com.weslleycampos.simpanel.simulators.Simulator
import kotlin.test.Test
import kotlin.test.assertEquals

class StillBootedTest {
    private val pro = Simulator(udid = "A1", name = "iPhone 18 Pro", runtime = "iOS 27.0", booted = true)
    private val max = Simulator(udid = "A2", name = "iPhone 18 Pro Max", runtime = "iOS 27.0", booted = true)
    private val air = Simulator(udid = "A3", name = "iPhone Air", runtime = "iOS 27.0", booted = true)

    @Test
    fun offersOnlyTheOpenedSimulatorsThatAreStillBooted() {
        val listed = listOf(pro.copy(booted = false), max, air)

        assertEquals(listOf(max), stillBooted(opened = listOf(pro, max), listed = listed))
    }

    @Test
    fun offersEveryOpenedSimulatorWhenTheListCouldNotBeRead() {
        assertEquals(listOf(pro, max), stillBooted(opened = listOf(pro, max), listed = null))
    }
}
