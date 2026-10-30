package br.com.weslleycampos.simpanel.simulators

/** Which way a quarter turn of the simulator goes. */
enum class Rotation(internal val devicectlName: String) {
    Left("left"),
    Right("right"),
}
