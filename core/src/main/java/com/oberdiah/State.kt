package com.oberdiah

import com.oberdiah.utils.StatefulBoolean
import com.oberdiah.utils.StatefulInt

val statefulHighScore = StatefulInt("highScore", 0)
val statefulSwipeToSlam = StatefulBoolean("swipeToSlam", true)