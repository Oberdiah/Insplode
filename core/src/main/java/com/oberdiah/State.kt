package com.oberdiah

import com.badlogic.gdx.utils.Align
import com.oberdiah.utils.ScreenShakeSettings
import com.oberdiah.utils.StatefulBoolean
import com.oberdiah.utils.StatefulEnum
import com.oberdiah.utils.StatefulInt

val statefulHighScore = StatefulInt("highScore", 0)
val statefulRenderParticles = StatefulBoolean("renderParticles", true)
val statefulPauseSide = StatefulInt("pauseSide", Align.right)
val statefulScreenShakeSetting =
    StatefulEnum("screenShakeSetting", ScreenShakeSettings.Normal, ScreenShakeSettings.values())
val statefulVibrationSetting = StatefulBoolean("doVibration", true)
val statefulEasyMode = StatefulBoolean("easyMode", false)
val statefulCoinBalance = StatefulInt("coinBalance", 0)
val statefulPlayMusicSetting = StatefulBoolean("playMusic", true)