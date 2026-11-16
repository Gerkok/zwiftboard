package io.zwiftboard.android

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MyWhooshProtocolTest {
    @Test
    fun mapsSingleModeButtonsToMyWhooshControls() {
        assertEquals(
            """{"MessageType":"Controls","InGameControls":{"GearShifting":"-1"}}""" + "\n",
            MyWhooshProtocol.controlLine("PLUS", true, MyWhooshProtocol.Mode.SINGLE),
        )
        assertNull(MyWhooshProtocol.controlLine("MIN", true, MyWhooshProtocol.Mode.SINGLE))
        assertEquals(
            """{"MessageType":"Controls","InGameControls":{"GearShifting":"1"}}""" + "\n",
            MyWhooshProtocol.controlLine("B", true, MyWhooshProtocol.Mode.SINGLE),
        )
        assertEquals(
            """{"MessageType":"Controls","InGameControls":{"UTurn":"true"}}""" + "\n",
            MyWhooshProtocol.controlLine("Y", true, MyWhooshProtocol.Mode.SINGLE),
        )
        assertNull(MyWhooshProtocol.controlLine("MIN", true, MyWhooshProtocol.Mode.SINGLE))
        assertNull(MyWhooshProtocol.controlLine("UP", true, MyWhooshProtocol.Mode.SINGLE))
        assertNull(MyWhooshProtocol.controlLine("DOWN", true, MyWhooshProtocol.Mode.SINGLE))
    }

    @Test
    fun mapsDoubleModeLeftPodAndRightPodButtons() {
        assertEquals(
            """{"MessageType":"Controls","InGameControls":{"Steering":"-1"}}""" + "\n",
            MyWhooshProtocol.controlLine("LEFT", true, MyWhooshProtocol.Mode.DOUBLE),
        )
        assertEquals(
            """{"MessageType":"Controls","InGameControls":{"Steering":"1"}}""" + "\n",
            MyWhooshProtocol.controlLine("RIGHT", true, MyWhooshProtocol.Mode.DOUBLE),
        )
        assertEquals(
            """{"MessageType":"Controls","InGameControls":{"Steering":"0"}}""" + "\n",
            MyWhooshProtocol.controlLine("LEFT", false, MyWhooshProtocol.Mode.DOUBLE),
        )
        assertEquals(
            """{"MessageType":"Controls","InGameControls":{"GearShifting":"1"}}""" + "\n",
            MyWhooshProtocol.controlLine("PLUS", true, MyWhooshProtocol.Mode.DOUBLE),
        )
        assertEquals(
            """{"MessageType":"Controls","InGameControls":{"GearShifting":"-1"}}""" + "\n",
            MyWhooshProtocol.controlLine("MIN", true, MyWhooshProtocol.Mode.DOUBLE),
        )
        assertEquals(
            """{"MessageType":"Controls","InGameControls":{"GearShifting":"1"}}""" + "\n",
            MyWhooshProtocol.controlLine("B", true, MyWhooshProtocol.Mode.DOUBLE),
        )
        assertEquals(
            """{"MessageType":"Controls","InGameControls":{"UTurn":"true"}}""" + "\n",
            MyWhooshProtocol.controlLine("Y", true, MyWhooshProtocol.Mode.DOUBLE),
        )
        assertNull(MyWhooshProtocol.controlLine("UP", true, MyWhooshProtocol.Mode.DOUBLE))
        assertNull(MyWhooshProtocol.controlLine("DOWN", true, MyWhooshProtocol.Mode.DOUBLE))
    }

    @Test
    fun mapsRightPodSteeringAndShiftReleaseEvents() {
        assertEquals(
            """{"MessageType":"Controls","InGameControls":{"Steering":"1"}}""" + "\n",
            MyWhooshProtocol.controlLine("A", true, MyWhooshProtocol.Mode.SINGLE),
        )
        assertEquals(
            """{"MessageType":"Controls","InGameControls":{"Steering":"-1"}}""" + "\n",
            MyWhooshProtocol.controlLine("Z", true, MyWhooshProtocol.Mode.SINGLE),
        )
        assertEquals(
            """{"MessageType":"Controls","InGameControls":{"Steering":"-1"}}""" + "\n",
            MyWhooshProtocol.controlLine("Z", true, MyWhooshProtocol.Mode.DOUBLE),
        )
        assertEquals(
            """{"MessageType":"Controls","InGameControls":{"Steering":"0"}}""" + "\n",
            MyWhooshProtocol.controlLine("Z", false, MyWhooshProtocol.Mode.DOUBLE),
        )
    }

    @Test
    fun ignoresUnmappedAndMomentaryReleaseEvents() {
        assertNull(MyWhooshProtocol.controlLine("MIN", true, MyWhooshProtocol.Mode.SINGLE))
        assertNull(MyWhooshProtocol.controlLine("PLUS", false, MyWhooshProtocol.Mode.SINGLE))
        assertNull(MyWhooshProtocol.controlLine("MIN", false, MyWhooshProtocol.Mode.DOUBLE))
        assertNull(MyWhooshProtocol.controlLine("Y", false, MyWhooshProtocol.Mode.DOUBLE))
    }
}
