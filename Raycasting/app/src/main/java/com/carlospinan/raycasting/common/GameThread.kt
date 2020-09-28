package com.carlospinan.raycasting.common

import android.view.SurfaceHolder

/**
 * @author Carlos Piñan
 */

private const val MILLISECOND = 1000L

class GameThread(
    private val graphics: Graphics,
    private val holder: SurfaceHolder,
    private val fps: Long = 60L
) : Thread() {

    private var lastTimeStamp = System.currentTimeMillis()
    private var currentFPS: Float = 0f

    var running: Boolean = true
    var pause: Boolean = false

    override fun run() {
        val tickFPS = (MILLISECOND / fps)

        while (running) {
            synchronized(holder) {
                val currentTime = System.currentTimeMillis()

                val diffTime = (currentTime - lastTimeStamp)
                val deltaTime = diffTime / MILLISECOND.toFloat()

                currentFPS = 1.0f / deltaTime

                if (!pause) {
                    val canvas = holder.lockCanvas()

                    graphics.update(deltaTime)
                    graphics.render(canvas)

                    holder.unlockCanvasAndPost(canvas)
                }

                lastTimeStamp = currentTime

                val sleepTime = tickFPS - diffTime
                sleep(if (sleepTime > 0) sleepTime else tickFPS)
            }
        }
    }

}