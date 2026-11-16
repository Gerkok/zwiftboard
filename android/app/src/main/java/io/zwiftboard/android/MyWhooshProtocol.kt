package io.zwiftboard.android

internal object MyWhooshProtocol {
    const val PORT = 21587

    enum class Mode {
        SINGLE,
        DOUBLE,
    }

    val connectedDevicesList =
        """{"ConnectedDevices":[],"MessageType":"ConnectedDevicesList"}""" + "\n"

    fun controlLine(button: String, pressed: Boolean, mode: Mode): String? {
        val control = when (button to pressed) {
            "PLUS" to true -> if (mode == Mode.SINGLE) {
                """"GearShifting":"-1""""
            } else {
                """"GearShifting":"1""""
            }
            "B" to true -> """"GearShifting":"1""""
            "MIN" to true -> if (mode == Mode.DOUBLE) {
                """"GearShifting":"-1""""
            } else {
                return null
            }
            "A" to true -> """"Steering":"1""""
            "A" to false, "Z" to false -> """"Steering":"0""""
            "Z" to true -> """"Steering":"-1""""
            "LEFT" to true -> if (mode == Mode.DOUBLE) {
                """"Steering":"-1""""
            } else {
                return null
            }
            "LEFT" to false -> if (mode == Mode.DOUBLE) {
                """"Steering":"0""""
            } else {
                return null
            }
            "RIGHT" to true -> if (mode == Mode.DOUBLE) {
                """"Steering":"1""""
            } else {
                return null
            }
            "RIGHT" to false -> if (mode == Mode.DOUBLE) {
                """"Steering":"0""""
            } else {
                return null
            }
            "Y" to true -> """"UTurn":"true""""
            else -> return null
        }
        return """{"MessageType":"Controls","InGameControls":{$control}}""" + "\n"
    }
}
