package br.com.weslleycampos.simpanel.simulators

import java.nio.file.Path
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class ScreenshotFileTest {
    @Test
    fun namesTheFileAsSimulatorDoes() {
        val time = LocalDateTime.of(2026, 10, 30, 9, 5, 7)

        val file = screenshotFile(Path.of("/Users/me/Desktop"), "iPhone 18 Pro", time)

        assertEquals(
            Path.of("/Users/me/Desktop/Simulator Screenshot - iPhone 18 Pro - 2026-10-30 at 09.05.07.png"),
            file,
        )
    }
}
