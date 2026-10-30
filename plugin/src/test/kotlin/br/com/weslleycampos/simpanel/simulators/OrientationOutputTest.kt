package br.com.weslleycampos.simpanel.simulators

import kotlin.test.Test
import kotlin.test.assertEquals

class OrientationOutputTest {
    private fun output(orientation: String, nonFlat: String) = """
        {
          "info" : {
            "commandType" : "devicectl.device.orientation.get",
            "jsonVersion" : 5,
            "outcome" : "success",
            "version" : "651.13.5"
          },
          "result" : {
            "deviceIdentifier" : "B2",
            "deviceIsOrientationLocked" : false,
            "deviceOrientation" : "$orientation",
            "deviceOrientationNonFlat" : "$nonFlat"
          }
        }
    """.trimIndent()

    @Test
    fun readsTheWayTheSimulatorIsHeld() {
        assertEquals(Orientation.LandscapeLeft, parseOrientation(output("landscapeLeft", "landscapeLeft")))
        assertEquals(Orientation.PortraitUpsideDown, parseOrientation(output("faceUp", "portraitUpsideDown")))
    }

    @Test
    fun takesAnOrientationItDoesntKnowForPortrait() {
        assertEquals(Orientation.Portrait, parseOrientation(output("unknown", "unknown")))
        assertEquals(Orientation.Portrait, parseOrientation("""{ "result" : { } }"""))
    }
}
