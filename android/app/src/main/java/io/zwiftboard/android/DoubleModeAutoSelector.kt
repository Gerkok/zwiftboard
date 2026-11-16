package io.zwiftboard.android

internal class DoubleModeAutoSelector {
    private var selectionMade = false

    fun onState(
        leftConnected: Boolean,
        rightConnected: Boolean,
        leftUnlocked: Boolean,
        currentMode: MyWhooshProtocol.Mode = MyWhooshProtocol.Mode.SINGLE,
    ): MyWhooshProtocol.Mode? {
        if (selectionMade || !leftConnected || !rightConnected || !leftUnlocked) return null
        selectionMade = true
        return if (currentMode == MyWhooshProtocol.Mode.SINGLE) {
            MyWhooshProtocol.Mode.DOUBLE
        } else {
            null
        }
    }
}
