package br.com.weslleycampos.simpanel.simulators

import kotlin.test.Test
import kotlin.test.assertEquals

class SimulatorListTest {
    private val output = """
        {
          "devices" : {
            "com.apple.CoreSimulator.SimRuntime.iOS-26-5" : [
              { "udid" : "A1", "name" : "iPhone 17 Pro", "state" : "Shutdown", "isAvailable" : true },
              { "udid" : "A2", "name" : "iPhone 17e", "state" : "Booted", "isAvailable" : true }
            ],
            "com.apple.CoreSimulator.SimRuntime.watchOS-12-0" : [
              { "udid" : "W1", "name" : "Apple Watch Ultra 4", "state" : "Booted", "isAvailable" : true }
            ],
            "com.apple.CoreSimulator.SimRuntime.iOS-27-0" : [
              { "udid" : "B1", "name" : "iPhone 18 Pro", "state" : "Shutdown", "isAvailable" : true,
                "dataPath" : "/Users/me/Library/Developer/CoreSimulator/Devices/B1/data" },
              { "udid" : "B2", "name" : "iPhone 18 Pro Max", "state" : "Shutdown", "isAvailable" : false },
              { "udid" : "B3", "name" : "iPad Pro 13-inch (M6)", "state" : "Shutdown", "isAvailable" : true }
            ]
          }
        }
    """.trimIndent()

    @Test
    fun listsAvailableIosSimulatorsBootedFirstThenNewestRuntime() {
        assertEquals(
            listOf(
                Simulator("A2", "iPhone 17e", "iOS 26.5", booted = true),
                Simulator("B1", "iPhone 18 Pro", "iOS 27.0", booted = false),
                Simulator("B3", "iPad Pro 13-inch (M6)", "iOS 27.0", booted = false),
                Simulator("A1", "iPhone 17 Pro", "iOS 26.5", booted = false),
            ),
            parseSimulators(output),
        )
    }

    @Test
    fun listsNothingWhenNoRuntimeIsInstalled() {
        assertEquals(emptyList(), parseSimulators("""{ "devices" : { } }"""))
    }
}
