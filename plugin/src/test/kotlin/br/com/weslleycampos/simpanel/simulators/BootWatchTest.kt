package br.com.weslleycampos.simpanel.simulators

import kotlin.test.Test
import kotlin.test.assertEquals

class BootWatchTest {
    private val pro = Simulator("B849", "iPhone 18 Pro", "iOS 27.0", booted = false)
    private val proMax = Simulator("770D", "iPhone 18 Pro Max", "iOS 27.0", booted = false)
    private val air = Simulator("A1R0", "iPhone Air", "iOS 27.0", booted = false)

    @Test
    fun showsTheSimulatorItWaitsForOnceItBoots() {
        val simulators = listOf(pro.copy(booted = true), proMax.copy(booted = true))

        val step = BootWatch.after(simulators, waitingFor = proMax, bootedBefore = setOf(pro.udid))

        assertEquals(BootWatch.Show(proMax.copy(booted = true)), step)
    }

    @Test
    fun showsAnotherSimulatorThatBootedSinceTheLastPoll() {
        val simulators = listOf(air.copy(booted = true), pro, proMax)

        assertEquals(
            BootWatch.Show(air.copy(booted = true)),
            BootWatch.after(simulators, waitingFor = proMax, bootedBefore = emptySet()),
        )
    }

    @Test
    fun keepsWaitingWhileOnlySimulatorsBootedAllAlongAreBooted() {
        val simulators = listOf(pro.copy(booted = true), proMax)

        for (bootedBefore in listOf(setOf(pro.udid), null)) {
            assertEquals(BootWatch.KeepWaiting, BootWatch.after(simulators, waitingFor = proMax, bootedBefore))
        }
    }

    @Test
    fun waitsForAnotherSimulatorWhenTheOneItWaitedForIsDeleted() {
        assertEquals(
            BootWatch.WaitFor(pro),
            BootWatch.after(listOf(pro, air), waitingFor = proMax, bootedBefore = null),
        )
        assertEquals(BootWatch.WaitFor(null), BootWatch.after(emptyList(), waitingFor = proMax, bootedBefore = null))
    }

    @Test
    fun offersToBootASimulatorCreatedWhileThereWereNone() {
        assertEquals(BootWatch.WaitFor(pro), BootWatch.after(listOf(pro), waitingFor = null, bootedBefore = emptySet()))
        assertEquals(BootWatch.KeepWaiting, BootWatch.after(emptyList(), waitingFor = null, bootedBefore = emptySet()))
    }
}
