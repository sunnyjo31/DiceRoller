package com.tokus.diceroller.model

import kotlin.random.Random

object Dice {



    fun roll(): Int {
        return Random.nextInt(1, 7)
    }

    /**
     * Keeps doubles possible but uncommon: a second die matches the first 10% of the time.
     */
    fun rollSecondDie(firstDie: Int): Int {
        if (Random.nextInt(10) == 0) return firstDie

        val roll = Random.nextInt(1, 6)
        return if (roll >= firstDie) roll + 1 else roll
    }
}
