package com.carlospinan.raycasting

import android.content.Context
import android.content.res.Resources
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import com.carlospinan.raycasting.common.GameView
import com.carlospinan.raycasting.common.Vector
import com.carlospinan.raycasting.common.degreesToRadians
import kotlin.math.*

private val map = arrayOf(
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1),
    intArrayOf(1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 1, 0, 0, 0, 0, 2, 2, 2, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 1, 1, 0, 0, 0, 0, 0, 2, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 1),
    intArrayOf(1, 0, 2, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 3, 1, 0, 0, 0, 3, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 3, 0, 0, 0, 0, 3, 3, 1),
    intArrayOf(1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1),
    intArrayOf(1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1)
)

private const val MINI_MAP_FACTOR = 0.2F
private const val TILE_SIZE = 64
private const val FOV = 60F
private const val HALF_FOV = FOV / 2

private val mapWidth = map[0].size
private val mapHeight = map.size

private val virtualScreenWidth = TILE_SIZE * mapWidth
private val virtualScreenHeight = TILE_SIZE * mapHeight

fun normalizeAngle(angle: Float): Float {
    var rotation = angle
    if (rotation < 0F)
        rotation += 360F
    rotation %= 360F
    return rotation
}

fun distanceBetweenPoints(x1: Float, y1: Float, x2: Float, y2: Float): Float {
    val dX = x1 - x2
    val dY = y1 - y2
    return sqrt(dX * dX + dY * dY)
}

fun facingDirections(rotation: Float): BooleanArray {
    val rotation = normalizeAngle(rotation)

    val isFacingUp = rotation > 180 && rotation < 360
    val isFacingDown = !isFacingUp

    val isFacingRight = rotation > 270 || rotation < 90
    val isFacingLeft = !isFacingRight

    return booleanArrayOf(
        isFacingUp, // 0
        isFacingDown, // 1
        isFacingLeft, // 2
        isFacingRight // 3
    )
}

fun collides(x: Float, y: Float): Boolean {
    val tile = tile(x, y)
    return tile >= 1
}

fun tile(x: Float, y: Float): Int {
    val tiles = toMapCoordinates(x, y)
    if (isSafe(tiles)) {
        return map[tiles.second][tiles.first]
    }
    return -1
}

fun isSafe(tiles: Pair<Int, Int>): Boolean {
    return (tiles.first in 0 until mapWidth) &&
            (tiles.second in 0 until mapHeight)
}

fun toMapCoordinates(x: Float, y: Float): Pair<Int, Int> {
    return Pair(
        floor(x / TILE_SIZE).toInt(),
        floor(y / TILE_SIZE).toInt()
    )
}

class Ray(rotation: Float) {

    private val rayPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0x22D6893A
        strokeWidth = 2F
    }

    val angle = normalizeAngle(rotation)

    private val radians = degreesToRadians(angle)
    private val facingDirections = facingDirections(angle)
    private val isFacingUp = facingDirections[0]
    private val isFacingDown = facingDirections[1]
    private val isFacingLeft = facingDirections[2]
    private val isFacingRight = facingDirections[3]

    private var wallHitX: Float = -1F

    private var wallHitY: Float = -1F

    var distance: Float = -1F
        private set

    var hitWallColor: Float = -1F
        private set

    var wasVerticalHit: Boolean = false
        private set

    fun cast(player: Player) {
        var xIntercept: Float
        var yIntercept: Float

        var xStep = 0F
        var yStep = 0F

        // HORIZONTAL RAY-GRID INTERSECTION CODE
        var foundHorizontalWallHit = false
        var horizontalWallHitX = -1F
        var horizontalWallHitY = -1F
        var horizontalWallColor = 0F

        val mapCoords = toMapCoordinates(player.x, player.y)

        // Find the y-coordinate of the closest horizontal grid intersection
        yIntercept = mapCoords.second * TILE_SIZE.toFloat()
        yIntercept += if (isFacingDown) TILE_SIZE else 0

        // Find the x-coordinate of the closest horizontal grid intersection
        xIntercept = player.x + (yIntercept - player.y) / tan(radians)

        yStep = TILE_SIZE.toFloat()
        yStep *= if (isFacingUp) -1F else 1F

        xStep = TILE_SIZE / tan(radians)
        xStep *= if (isFacingLeft && xStep > 0) -1F else 1F
        xStep *= if (isFacingRight && xStep < 0) -1F else 1F

        var nextHorizontalTouchX = xIntercept
        var nextHorizontalTouchY = yIntercept

        // Increment xstep and ystep until we find a wall

        while (nextHorizontalTouchX >= 0 &&
            nextHorizontalTouchX <= virtualScreenWidth &&
            nextHorizontalTouchY >= 0 &&
            nextHorizontalTouchY <= virtualScreenHeight
        ) {
            val evalX = nextHorizontalTouchX
            val evalY = nextHorizontalTouchY + if (isFacingUp) -1 else 0
            val tile = tile(evalX, evalY)
            if (tile >= 1) {
                foundHorizontalWallHit = true
                horizontalWallHitX = nextHorizontalTouchX
                horizontalWallHitY = nextHorizontalTouchY
                horizontalWallColor = tile.toFloat()
                break
            } else {
                nextHorizontalTouchX += xStep
                nextHorizontalTouchY += yStep
            }
        }

        // VERTICAL RAY-GRID INTERSECTION CODE
        var foundVerticalWallHit = false
        var verticalWallHitX = -1F
        var verticalWallHitY = -1F
        var verticalWallColor = 0F

        // Find the x-coordinate of the closest vertical grid intersection
        xIntercept = mapCoords.first * TILE_SIZE.toFloat()
        xIntercept += if (isFacingRight) TILE_SIZE else 0

        // Find the y-coordinate of the closest vertical grid intersection
        yIntercept = player.y + (xIntercept - player.x) * tan(radians)

        // Calculate the increment xstep and ystep

        xStep = TILE_SIZE.toFloat()
        xStep *= if (isFacingLeft) -1 else 1

        yStep = TILE_SIZE * tan(radians)
        yStep *= if (isFacingUp && yStep > 0) -1 else 1
        yStep *= if (isFacingDown && yStep < 0) -1 else 1

        var nextVerticalTouchX = xIntercept
        var nextVerticalTouchY = yIntercept

        // Increment xstep and ystep until we find a wall

        while (nextVerticalTouchX >= 0 &&
            nextVerticalTouchX <= virtualScreenWidth &&
            nextVerticalTouchY >= 0 &&
            nextVerticalTouchY <= virtualScreenHeight
        ) {
            val evalX = nextVerticalTouchX + if (isFacingLeft) -1 else 0
            val evalY = nextVerticalTouchY
            val tile = tile(evalX, evalY)
            if (tile >= 1) {
                foundVerticalWallHit = true
                verticalWallHitX = nextVerticalTouchX
                verticalWallHitY = nextVerticalTouchY
                verticalWallColor = tile.toFloat()
                break
            } else {
                nextVerticalTouchX += xStep
                nextVerticalTouchY += yStep
            }
        }

        // Calculate both horizontal and vertical distances and choose the smallest value
        val horizontalHitDistance = if (foundHorizontalWallHit)
            distanceBetweenPoints(player.x, player.y, horizontalWallHitX, horizontalWallHitY)
        else
            Float.MAX_VALUE

        val verticalHitDistance = if (foundVerticalWallHit)
            distanceBetweenPoints(player.x, player.y, verticalWallHitX, verticalWallHitY)
        else
            Float.MAX_VALUE

        // only store the smallest distance
        if (verticalHitDistance < horizontalHitDistance) {
            wallHitX = verticalWallHitX
            wallHitY = verticalWallHitY
            distance = verticalHitDistance
            hitWallColor = verticalWallColor
            wasVerticalHit = true
        } else {
            wallHitX = horizontalWallHitX
            wallHitY = horizontalWallHitY
            distance = horizontalHitDistance
            hitWallColor = horizontalWallColor
            wasVerticalHit = false
        }
    }

    fun render(canvas: Canvas, startX: Float, startY: Float) {
        canvas.drawLine(
            startX * MINI_MAP_FACTOR,
            startY * MINI_MAP_FACTOR,
            wallHitX * MINI_MAP_FACTOR,
            wallHitY * MINI_MAP_FACTOR,
            rayPaint
        )
    }

}

data class Player(
    var x: Float = 5 * TILE_SIZE.toFloat(),
    var y: Float = 6 * TILE_SIZE.toFloat(),
    val radius: Float = 4F,
    var rotation: Float = 0F,
    val rotationSpeed: Float = 3F,
    val speed: Float = 40F,
    var move: Boolean = false
) {

    private val lineSize = 50F

    private val playerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFF0000.toInt()
    }

    private val directionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF0000FF.toInt()
        strokeWidth = 2F
    }

    fun renderMinimap(canvas: Canvas) {
        val mapX = x
        val mapY = y

        // Draw Player Rect
        val rect = RectF(
            mapX * MINI_MAP_FACTOR,
            mapY * MINI_MAP_FACTOR,
            (mapX + radius) * MINI_MAP_FACTOR,
            (mapY + radius) * MINI_MAP_FACTOR,
        )
        canvas.drawRect(rect, playerPaint)

        val startX = mapX + radius / 2
        val startY = mapY + radius / 2

        val radianDirectionAngle = degreesToRadians(rotation)
        val endX = startX + cos(radianDirectionAngle) * lineSize
        val endY = startY + sin(radianDirectionAngle) * lineSize

        // Draw Player Direction
        canvas.drawLine(
            startX * MINI_MAP_FACTOR,
            startY * MINI_MAP_FACTOR,
            endX * MINI_MAP_FACTOR,
            endY * MINI_MAP_FACTOR,
            directionPaint
        )
    }

    fun update(dt: Float) {
        if (move) {
            val radianDirectionAngle = degreesToRadians(rotation)
            val newX = x + cos(radianDirectionAngle) * speed * dt
            val newY = y + sin(radianDirectionAngle) * speed * dt

            if (!collides(newX + 0, newY + 0)) {
                x = newX
                y = newY
            }
        }
    }

    fun updateDirectionAngle(delta: Float) {
        rotation += (delta * rotationSpeed)
        rotation = normalizeAngle(rotation)
    }
}

/**
 * @author Carlos Piñan
 */
class RaycastingView(context: Context) : GameView(context, 30L) {

    private var currentTouch = Pair(MotionEvent.ACTION_CANCEL, Vector(0F, 0F))

    private val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        color = 0xFF000000.toInt()
    }

    private val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = Color.argb(255, 128, 128, 128)
    }

    private val walkPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xFFFFFFFF.toInt()
    }

    private val wallPaintFullColor = arrayOf(
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(255, 255, 0, 0)
        },
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(255, 0, 255, 0)
        },
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(255, 0, 0, 255)
        }
    )

    private val wallPaintHalfColor = arrayOf(
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(255, 200, 0, 0)
        },
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(255, 0, 200, 0)
        },
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL
            color = Color.argb(255, 0, 0, 200)
        }
    )

    private val screenWidth = Resources.getSystem().displayMetrics.widthPixels
    private val screenHeight = Resources.getSystem().displayMetrics.heightPixels

    private val heightRatio = (screenHeight.toFloat() / virtualScreenHeight.toFloat())

    private val tX = ((screenWidth - virtualScreenWidth) / heightRatio) / 4
    private val tY = 0F//(screenHeight - virtualScreenHeight) / 2

    private val player = Player()
    private val raycastList = arrayOfNulls<Ray>(virtualScreenWidth)

    override fun gameRender(canvas: Canvas) {
        canvas.drawColor(0xFF000000.toInt())

        canvas.save()
        canvas.translate(tX, tY)
        canvas.scale(heightRatio, heightRatio)

        render3DProjectedWalls(canvas)
        drawMinimap(canvas)

        canvas.restore()
    }

    override fun gameUpdate(dt: Float) {
        // Player direction increase clockwise
        player.move = false
        if (currentTouch.first == MotionEvent.ACTION_DOWN) {
            val touchArea = currentTouch.second
            if (touchArea.x <= -0.5F) {
                player.updateDirectionAngle(-1F)
            } else if (touchArea.x >= 0.5F) {
                player.updateDirectionAngle(1F)
            }

            player.move = touchArea.y <= 0F

        }

        player.update(dt)

        var angle = player.rotation - HALF_FOV
        val angleIncrease = FOV / virtualScreenWidth
        for (i in 0 until virtualScreenWidth) {
            val ray = Ray(angle)
            ray.cast(player)
            raycastList[i] = ray
            angle += angleIncrease
        }

    }

    private fun drawMinimap(canvas: Canvas) {

        for (row in 0 until mapHeight) {
            for (col in 0 until mapWidth) {
                val rowTile = (row * TILE_SIZE) * MINI_MAP_FACTOR
                val colTile = (col * TILE_SIZE) * MINI_MAP_FACTOR

                val rect = RectF(
                    colTile,
                    rowTile,
                    colTile + TILE_SIZE * MINI_MAP_FACTOR,
                    rowTile + TILE_SIZE * MINI_MAP_FACTOR
                )

                val tile = map[row][col]
                val paintToUse = if (tile >= 1) wallPaint else walkPaint

                // canvas.drawRect(rect, borderPaint)
                canvas.drawRect(rect, paintToUse)

            }
        }

        for (ray in raycastList) {
            ray?.render(canvas, player.x + player.radius / 2, player.y + player.radius / 2)
        }

        player.renderMinimap(canvas)
    }

    private fun render3DProjectedWalls(canvas: Canvas) {
        for (col in 0 until virtualScreenWidth) {
            raycastList[col]?.let { ray ->
                // get the perpendicular distance to the wall to fix fishbowl distortion
                val correctedAngle = degreesToRadians(ray.angle - player.rotation)
                val correctedWallDistance = ray.distance * cos(correctedAngle)

                // calculate the distance to the projection plane
                val distanceProjectionPlane = (virtualScreenWidth / 2) /
                        tan(
                            degreesToRadians(
                                HALF_FOV
                            )
                        )

                // projected wall height
                val wallStripHeight = (TILE_SIZE / correctedWallDistance) * distanceProjectionPlane

                val left = col.toFloat()
                val top = (virtualScreenHeight / 2) - (wallStripHeight / 2)

                val right = left + 1F
                val bottom = top + wallStripHeight

                val rect = RectF(
                    left, top, right, bottom
                )

                val index = ray.hitWallColor.toInt() - 1
                if (ray.wasVerticalHit) {
                    canvas.drawRect(rect, wallPaintHalfColor[index])
                } else {
                    canvas.drawRect(rect, wallPaintFullColor[index])
                }
            }

        }
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val y = event.y

        val unitX = 2 * (x / screenWidth) - 1F
        val unitY = 2 * (y / screenHeight) - 1F

        currentTouch = Pair(event.action, Vector(unitX, unitY))

        return true
    }

}