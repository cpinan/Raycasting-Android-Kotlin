package com.carlospinan.raycasting.common

import android.content.Context
import android.graphics.Canvas
import android.view.SurfaceHolder
import android.view.SurfaceView

/**
 * @author Carlos Piñan
 */

abstract class GameView(
    context: Context,
    fps: Long = 60
) : SurfaceView(context) {

    private val surfaceHolderCallback = object : SurfaceHolder.Callback {
        override fun surfaceChanged(holder: SurfaceHolder?, format: Int, width: Int, height: Int) {
            onSurfaceChanged()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder?) {
            thread.running = false
            thread.pause = true
            thread.join()
            onSurfaceDestroyed()
        }

        override fun surfaceCreated(holder: SurfaceHolder?) {
            onSurfaceCreated()
            thread.running = true
            thread.pause = false
            thread.start()
        }

    }

    private val graphicsCallback = object : Graphics {
        override fun render(canvas: Canvas) {
            dispatchDraw(canvas)
        }

        override fun update(dt: Float) {
            gameUpdate(dt)
        }

    }

    private val thread = GameThread(
        graphicsCallback,
        holder,
        fps
    )

    init {
        holder.addCallback(surfaceHolderCallback)
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        gameRender(canvas)
        postInvalidate()
    }

    protected open fun onSurfaceChanged() {}

    protected open fun onSurfaceCreated() {}

    protected open fun onSurfaceDestroyed() {}

    abstract fun gameRender(canvas: Canvas)

    abstract fun gameUpdate(dt: Float)

}