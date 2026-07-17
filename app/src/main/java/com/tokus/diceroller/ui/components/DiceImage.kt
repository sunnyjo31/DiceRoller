package com.tokus.diceroller.ui.components

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.view.animation.LinearInterpolator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.size
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

@Composable
fun DiceImage(
    value: Int,
    size: Int = 180,
    rolling: Boolean = false
) {
    AndroidView(
        factory = { context -> Dice3DView(context) },
        update = { die ->
            die.value = value
            die.rolling = rolling
        },
        modifier = Modifier.size(size.dp)
    )
}

private class Dice3DView(context: Context) : View(context) {
    private data class Vector3(val x: Float, val y: Float, val z: Float) {
        operator fun plus(other: Vector3) = Vector3(x + other.x, y + other.y, z + other.z)
        operator fun times(scale: Float) = Vector3(x * scale, y * scale, z * scale)
    }

    private data class Face(val vertices: IntArray, val value: Int)

    private val facePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val edgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = Color.argb(85, 0, 0, 0)
    }
    private val pipPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = Color.rgb(28, 28, 32) }

    private val cube = arrayOf(
        Vector3(-1f, -1f, -1f), Vector3(1f, -1f, -1f),
        Vector3(1f, 1f, -1f), Vector3(-1f, 1f, -1f),
        Vector3(-1f, -1f, 1f), Vector3(1f, -1f, 1f),
        Vector3(1f, 1f, 1f), Vector3(-1f, 1f, 1f)
    )
    private val faces = arrayOf(
        Face(intArrayOf(4, 5, 6, 7), 1), Face(intArrayOf(1, 0, 3, 2), 6),
        Face(intArrayOf(0, 1, 5, 4), 2), Face(intArrayOf(7, 6, 2, 3), 5),
        Face(intArrayOf(1, 2, 6, 5), 3), Face(intArrayOf(0, 4, 7, 3), 4)
    )
    private val pipLocations = arrayOf(
        0f to 0f,
        -0.5f to -0.5f, 0.5f to 0.5f,
        -0.5f to -0.5f, 0f to 0f, 0.5f to 0.5f,
        -0.5f to -0.5f, 0.5f to -0.5f, -0.5f to 0.5f, 0.5f to 0.5f,
        -0.5f to -0.5f, 0.5f to -0.5f, 0f to 0f, -0.5f to 0.5f, 0.5f to 0.5f,
        -0.5f to -0.5f, -0.5f to 0f, -0.5f to 0.5f, 0.5f to -0.5f, 0.5f to 0f, 0.5f to 0.5f
    )

    var value = 1
        set(newValue) {
            field = newValue
            if (!_rolling) settleOn(newValue)
        }

    private var _rolling = false
    var rolling: Boolean
        get() = _rolling
        set(newValue) {
            if (_rolling == newValue) return
            _rolling = newValue
            if (newValue) startTumble() else settleOn(value)
        }

    private var rotationX = -20f
    private var rotationY = -28f
    private var rotationZ = 0f
    private var animator: ValueAnimator? = null

    override fun onDetachedFromWindow() {
        animator?.cancel()
        super.onDetachedFromWindow()
    }

    private fun startTumble() {
        animator?.cancel()
        val startX = rotationX
        val startY = rotationY
        val startZ = rotationZ
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 2_200L
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                // Every axis completes a whole number of turns so repeat mode is seamless.
                rotationX = startX + progress * 360f
                rotationY = startY + progress * 720f
                rotationZ = startZ + progress * 360f
                invalidate()
            }
            start()
        }
    }

    private fun settleOn(number: Int) {
        animator?.cancel()
        val (targetX, targetY) = when (number) {
            1 -> 0f to 0f
            2 -> -90f to 0f
            3 -> 0f to -90f
            4 -> 0f to 90f
            5 -> 90f to 0f
            else -> 0f to 180f
        }
        val startX = rotationX
        val startY = rotationY
        val startZ = rotationZ
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = 420L
            interpolator = DecelerateInterpolator()
            addUpdateListener { animation ->
                val progress = animation.animatedValue as Float
                rotationX = startX + shortestDelta(startX, targetX) * progress
                rotationY = startY + shortestDelta(startY, targetY) * progress
                rotationZ = startZ + shortestDelta(startZ, 0f) * progress
                invalidate()
            }
            start()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val centerX = width / 2f
        val centerY = height / 2f
        // Keep visible space around the die, including while it is tumbling.
        val scale = min(width, height) * 0.31f
        if (!_rolling) {
            drawRestingFace(canvas, centerX, centerY, scale)
            return
        }
        val rotated = cube.map(::rotate)
        val orderedFaces = faces.sortedBy { face -> face.vertices.map { rotated[it].z }.average() }

        orderedFaces.forEach { face ->
            val points = face.vertices.map { project(rotated[it], centerX, centerY, scale) }
            val path = roundedFacePath(points)
            val normal = normal(rotated[face.vertices[0]], rotated[face.vertices[1]], rotated[face.vertices[2]])
            val light = max(0f, normal.x * -0.25f + normal.y * -0.35f + normal.z * 0.9f)
            val shade = (115 + light * 140).toInt()
            facePaint.color = Color.rgb(shade, shade, min(255, shade + 6))
            canvas.drawPath(path, facePaint)
            canvas.drawPath(path, edgePaint)
            drawPips(canvas, face, rotated, centerX, centerY, scale)
        }
    }

    private fun drawPips(canvas: Canvas, face: Face, rotated: List<Vector3>, cx: Float, cy: Float, scale: Float) {
        val indexes = when (face.value) { 1 -> 0..0; 2 -> 1..2; 3 -> 3..5; 4 -> 6..9; 5 -> 10..14; else -> 15..20 }
        val v = face.vertices.map { rotated[it] }
        indexes.forEach { index ->
            val (u, w) = pipLocations[index]
            val point = bilinear(v[0], v[1], v[2], v[3], (u + 1f) / 2f, (w + 1f) / 2f)
            val projected = project(point, cx, cy, scale)
            val radius = scale * 0.38f / (4.5f - point.z)
            canvas.drawCircle(projected.first, projected.second, radius, pipPaint)
        }
    }

    private fun drawRestingFace(canvas: Canvas, cx: Float, cy: Float, scale: Float) {
        val halfSize = scale * 1.10f
        val cornerRadius = halfSize * 0.20f
        facePaint.color = Color.rgb(235, 235, 241)
        canvas.drawRoundRect(
            cx - halfSize, cy - halfSize, cx + halfSize, cy + halfSize,
            cornerRadius, cornerRadius, facePaint
        )
        canvas.drawRoundRect(
            cx - halfSize, cy - halfSize, cx + halfSize, cy + halfSize,
            cornerRadius, cornerRadius, edgePaint
        )
        pipIndexes(value).forEach { index ->
            val (x, y) = pipLocations[index]
            canvas.drawCircle(
                cx + x * halfSize * 0.74f,
                cy + y * halfSize * 0.74f,
                halfSize * 0.17f,
                pipPaint
            )
        }
    }

    private fun pipIndexes(number: Int): IntRange = when (number) {
        1 -> 0..0
        2 -> 1..2
        3 -> 3..5
        4 -> 6..9
        5 -> 10..14
        else -> 15..20
    }

    private fun roundedFacePath(points: List<Pair<Float, Float>>): Path {
        val cornerFraction = 0.11f
        fun pointTowards(from: Pair<Float, Float>, to: Pair<Float, Float>) =
            from.first + (to.first - from.first) * cornerFraction to
                from.second + (to.second - from.second) * cornerFraction

        return Path().apply {
            val start = pointTowards(points[0], points[1])
            moveTo(start.first, start.second)
            for (index in 1 until points.size) {
                val previous = points[index - 1]
                val corner = points[index]
                val next = points[(index + 1) % points.size]
                val beforeCorner = pointTowards(previous, corner)
                val afterCorner = pointTowards(corner, next)
                lineTo(beforeCorner.first, beforeCorner.second)
                quadTo(corner.first, corner.second, afterCorner.first, afterCorner.second)
            }
            val beforeFirstCorner = pointTowards(points.last(), points.first())
            lineTo(beforeFirstCorner.first, beforeFirstCorner.second)
            quadTo(points.first().first, points.first().second, start.first, start.second)
            close()
        }
    }

    private fun rotate(point: Vector3): Vector3 {
        val x = Math.toRadians(rotationX.toDouble()).toFloat()
        val y = Math.toRadians(rotationY.toDouble()).toFloat()
        val z = Math.toRadians(rotationZ.toDouble()).toFloat()
        val afterX = Vector3(point.x, point.y * cos(x) - point.z * sin(x), point.y * sin(x) + point.z * cos(x))
        val afterY = Vector3(afterX.x * cos(y) + afterX.z * sin(y), afterX.y, -afterX.x * sin(y) + afterX.z * cos(y))
        return Vector3(afterY.x * cos(z) - afterY.y * sin(z), afterY.x * sin(z) + afterY.y * cos(z), afterY.z)
    }

    private fun project(point: Vector3, cx: Float, cy: Float, scale: Float): Pair<Float, Float> {
        val perspective = 4.5f / (4.5f - point.z)
        return cx + point.x * scale * perspective to cy + point.y * scale * perspective
    }

    private fun bilinear(a: Vector3, b: Vector3, c: Vector3, d: Vector3, u: Float, v: Float): Vector3 =
        a * ((1f - u) * (1f - v)) + b * (u * (1f - v)) + c * (u * v) + d * ((1f - u) * v)

    private fun normal(a: Vector3, b: Vector3, c: Vector3): Vector3 {
        val ab = b + a * -1f
        val ac = c + a * -1f
        val cross = Vector3(ab.y * ac.z - ab.z * ac.y, ab.z * ac.x - ab.x * ac.z, ab.x * ac.y - ab.y * ac.x)
        val length = sqrt(cross.x * cross.x + cross.y * cross.y + cross.z * cross.z)
        return cross * (1f / length)
    }

    private fun shortestDelta(from: Float, to: Float): Float {
        val normalizedFrom = ((from % 360f) + 360f) % 360f
        var delta = to - normalizedFrom
        if (delta > 180f) delta -= 360f
        if (delta < -180f) delta += 360f
        return delta
    }
}
