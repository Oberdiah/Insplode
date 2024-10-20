package com.oberdiah

import com.badlogic.gdx.utils.Align
import com.oberdiah.upgrades.Upgrade
import com.oberdiah.utils.ScreenShakeSettings
import com.oberdiah.utils.StatefulBoolean
import com.oberdiah.utils.StatefulEnum
import com.oberdiah.utils.StatefulInt

val statefulRenderParticles = StatefulBoolean("renderParticles", true)
val statefulPauseSide = StatefulInt("pauseSide", Align.right)
val statefulScreenShakeSetting =
    StatefulEnum(
        "screenShakeSetting", ScreenShakeSettings.Normal,
        ScreenShakeSettings.entries.toTypedArray()
    )
val statefulVibrationSetting = StatefulBoolean("doVibration", true)
val statefulEasyMode = StatefulBoolean("easyMode", false)
val statefulPlayMusicSetting = StatefulBoolean("playMusic", true)
val statefulPlaySoundSetting = StatefulBoolean("playSound", true)
val currentlyPlayingUpgrade =
    StatefulEnum<Upgrade>(
        "currentlyPlayingUpgrade", Upgrade.Movement,
        Upgrade.entries.toTypedArray()
    )