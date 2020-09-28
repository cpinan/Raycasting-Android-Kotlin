package com.carlospinan.raycasting.common

import android.graphics.Canvas

/**
 * @author Carlos Piñan
 */

/**
 * @author Carlos Piñan
 */
data class Vector(var x: Float, var y: Float) {

    fun set(_x: Float, _y: Float) {
        x = _x
        y = _y
    }

    fun plus(_x: Float, _y: Float) {
        x += _x
        y += _y
    }
}

interface Graphics {

    fun render(canvas: Canvas)

    fun update(dt: Float)

}

fun degreesToRadians(degrees: Float) = (degrees * Math.PI / 180F).toFloat()