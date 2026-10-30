package br.com.weslleycampos.simpanel.helper

import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.test.Test
import kotlin.test.assertTrue

class HelperBinaryTest {
    @Test
    fun makesAHelperThatLostItsExecuteBitExecutable(@TempDir dir: Path) {
        val binary = Files.writeString(dir.resolve("simpanel-helper"), "#!/bin/sh\n")
        Files.setPosixFilePermissions(binary, PosixFilePermissions.fromString("rw-r--r--"))

        assertTrue(Files.isExecutable(HelperBinary.executable(binary)))
    }
}
