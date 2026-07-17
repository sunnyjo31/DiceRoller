package com.tokus.diceroller.model

import com.tokus.diceroller.R
import kotlin.random.Random

object Dice {

    fun getImage(value: Int): Int {

        return when (value) {
            1 -> R.drawable.dice_1
            2 -> R.drawable.dice_2
            3 -> R.drawable.dice_3
            4 -> R.drawable.dice_4
            5 -> R.drawable.dice_5
            else -> R.drawable.dice_6
        }

    }

    fun roll(): Int {
        return Random.nextInt(1, 7)
    }
}