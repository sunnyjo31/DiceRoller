package com.tokus.diceroller.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tokus.diceroller.model.Dice
import androidx.compose.material3.RadioButton
import androidx.compose.foundation.layout.Row
import com.tokus.diceroller.model.DiceMode
import androidx.compose.foundation.layout.width
import com.tokus.diceroller.ui.components.DiceImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import android.media.MediaPlayer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import com.tokus.diceroller.R
import android.view.ViewGroup
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun DiceScreen(modifier: Modifier = Modifier) {

    var diceNumber by remember {
        mutableStateOf(1)
    }

    var secondDiceNumber by remember {
        mutableStateOf(1)
    }

    var diceMode by remember {
        mutableStateOf(DiceMode.ONE_DIE)
    }

    var rolling by remember {
        mutableStateOf(false)
    }

    val scope = rememberCoroutineScope()

    val context = LocalContext.current
    val hapticFeedback = LocalHapticFeedback.current
    val diceSize = 160

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {


            Text(
                text = "🎲 Pocket Dice",
                fontSize = 32.sp
            )
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = diceMode == DiceMode.ONE_DIE,
                    onClick = {
                        diceMode = DiceMode.ONE_DIE
                    }
                )

                Text("1 Die")

                Spacer(modifier = Modifier.width(16.dp))

                RadioButton(
                    selected = diceMode == DiceMode.TWO_DICE,
                    onClick = {
                        diceMode = DiceMode.TWO_DICE
                    }
                )

                Text("2 Dice")
            }

            Spacer(modifier = Modifier.height(20.dp))

            if (diceMode == DiceMode.ONE_DIE) {

                DiceImage(
                    value = diceNumber,
                    size = diceSize,
                    rolling = rolling
                )

            } else {

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DiceImage(
                        value = diceNumber,
                        size = diceSize,
                        rolling = rolling
                    )

                    DiceImage(
                        value = secondDiceNumber,
                        size = diceSize,
                        rolling = rolling
                    )
                }

            }

            Spacer(modifier = Modifier.height(20.dp))

            Button(
                enabled = !rolling,
                onClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)

                    scope.launch {

                        rolling = true

                        MediaPlayer.create(context, R.raw.dice_roll).apply {
                            start()
                            setOnCompletionListener { it.release() }
                        }

                        val rollCount = (10..16).random()
                        var delayTime = 20L

                        repeat(rollCount) {

                            diceNumber = Dice.roll()

                            if (diceMode == DiceMode.TWO_DICE) {
                                secondDiceNumber = Dice.rollSecondDie(diceNumber)
                            }

                            delay(delayTime)

                            delayTime += 15
                        }

                        diceNumber = Dice.roll()

                        if (diceMode == DiceMode.TWO_DICE) {
                            secondDiceNumber = Dice.rollSecondDie(diceNumber)
                        }

                        rolling = false
                    }

                },
                modifier = Modifier
                    .fillMaxWidth(0.65f)
                    .height(56.dp)
            ) {
                Text(
                    if (rolling) "Rolling..." else "🎲 Roll Dice",
                    fontSize = 18.sp
                )
            }


        }
        AndroidView(
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    adUnitId = "ca-app-pub-3940256099942544/9214589741"

                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                    )

                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
