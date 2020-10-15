package com.audiogram.videogenerator

import java.awt.Color
import java.awt.Graphics2D
import java.awt.geom.Arc2D

interface CanRender {
    fun render(g2d: Graphics2D)
    fun render(ampData: ArrayList<FloatArray>, currentPoint: Int, g2d: Graphics2D)
}

class EffectsManager(data: AudioGramData) {

    private val effectsRack = ArrayList<CanRender>()

    init {
        for (effect in data.effects) {
            when (effect.effectType) {
                AudioGramEffectType.PARTICLE -> effectsRack.add(ParticleEffect(effect))
            }
        }
    }

    fun render(g2d: Graphics2D) {
        for (effect in effectsRack) {
            effect.render(g2d)
        }
    }

    fun render(ampData: ArrayList<FloatArray>, currentPoint: Int, g2d: Graphics2D) {
        for (effect in effectsRack) {
            effect.render(ampData, currentPoint, g2d)
        }
    }
}

private class ParticleEffect(data: Effect) : CanRender {

    var particles = ArrayList<Particle>()
    var data = data

    override fun render(g2d: Graphics2D) {

        for (i in 0 until 100 - particles.size) {
            particles.add(Particle(data))
        }

        var it = particles.iterator()
        while (it.hasNext()) {
            var p = it.next()
            if (p.lifespan < 0)
                it.remove()
            else {
                p.draw(g2d)
                p.tick(0)
            }
        }
    }

    override fun render(ampData: ArrayList<FloatArray>, currentPoint: Int, g2d: Graphics2D) {

        for (i in 0 until 1000 - particles.size) {
            particles.add(Particle(data))
        }
        var vel = (ampData[currentPoint][0] + ampData[currentPoint][1]) / 20

        var it = particles.iterator()
        while (it.hasNext()) {
            var p = it.next()
            if (p.lifespan < 0)
                it.remove()
            else {
                p.draw(g2d)
                p.tick(vel.toInt())
            }
        }
    }

    private inner class Particle(data: Effect) {

        var dx = 0
        var dy = Math.random() * 4 + 1
        var x = data.posX!! + Math.random() * data.width!! + 1
        var y = data.posY!! + Math.random() * 200 + 1

        var radius = Math.random() * (5 - 1 + 1) + 1
        var lifespan = Math.random() * 300 + 1

        fun draw(g2d: Graphics2D) {

            var color = Color.decode(data.fill)

            g2d.color = Color(color.red, color.green, color.blue, (255 * (10 / 100.0)).toInt())
            var circle: Arc2D.Double = Arc2D.Double(x - radius, y - radius, radius * 2, radius * 2, 0.0, 360.0, Arc2D.CHORD)
            g2d.fill(circle)

            g2d.color = Color(color.red, color.green, color.blue, (255 * (30 / 100.0)).toInt())
            circle = Arc2D.Double(x - radius * 3 / 4, y - radius * 3 / 4, radius * 1.5, radius * 1.5, 0.0, 360.0, Arc2D.CHORD)
            g2d.fill(circle)

            g2d.color = Color(color.red, color.green, color.blue, (255 * (60 / 100.0)).toInt())
            circle = Arc2D.Double(x - radius / 2, y - radius / 2, radius, radius, 0.0, 360.0, Arc2D.CHORD)
            g2d.fill(circle)

        }

        fun tick(vel: Int) {
            x += dx
            y += (dy + vel).toInt()
            lifespan--
        }
    }
}



