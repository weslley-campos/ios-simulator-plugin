package br.com.weslleycampos.simpanel.simulators

import java.nio.file.Files
import kotlin.io.path.createDirectories
import kotlin.test.Test
import kotlin.test.assertEquals

class DeviceHubAppTest {
    @Test
    fun opensDeviceHubFromXcode27() {
        val contents = Files.createTempDirectory("Xcode.app")
        val developer = contents.resolve("Developer").createDirectories()
        val deviceHub = contents.resolve("Applications/DeviceHub.app").createDirectories()

        assertEquals(deviceHub, appIn(developer))
    }

    @Test
    fun opensSimulatorBeforeXcode27() {
        val contents = Files.createTempDirectory("Xcode.app")
        val developer = contents.resolve("Developer").createDirectories()

        assertEquals(developer.resolve("Applications/Simulator.app"), appIn(developer))
    }
}
