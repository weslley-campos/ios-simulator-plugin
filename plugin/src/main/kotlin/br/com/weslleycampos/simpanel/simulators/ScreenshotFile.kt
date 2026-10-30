package br.com.weslleycampos.simpanel.simulators

import java.nio.file.Path
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val FileTime = DateTimeFormatter.ofPattern("yyyy-MM-dd 'at' HH.mm.ss")

/** Where a screenshot of [simulator] taken at [time] goes, named as Simulator names its own. */
fun screenshotFile(directory: Path, simulator: String, time: LocalDateTime): Path =
    directory.resolve("Simulator Screenshot - $simulator - ${FileTime.format(time)}.png")
