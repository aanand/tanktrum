package client

import shared._

import org.newdawn.slick

import sbinary.Instances._
import sbinary.Operations

import GL._

class Projectile extends GameObject with shared.Projectile {
  var image: slick.Image = _
  
  initImage
  
  val trailLifetime = Config("projectile.trail.lifetime").toInt
  var trail: List[(Float, Float, Int)] = Nil
  var stationaryTime = 0
  var dead = false

  def trailDead = stationaryTime > trailLifetime
  
  def update(delta : Int) {
    updateTrail(delta)
  }

  def updateTrail(delta: Int) {
    if (shouldDrawTrail) {    
      trail = (x, y, delta + stationaryTime) :: trail
      stationaryTime = 0
    } else {
      stationaryTime += delta
    }
  }
  
  def shouldDrawTrail: Boolean = {
    if (!trail.isEmpty) {
      val (lastX, lastY, _) = trail.head

      if ((x, y) == (lastX, lastY)) {
        return false
      }
    }
    
    return true
  }
  
  def initImage {
    image = new slick.Image(imagePath)
  }
  
  def imageScale = (imageWidth.toFloat / image.getWidth) / Main.GAME_WINDOW_RATIO
 

  def render(g : slick.Graphics) {
    renderTrail(g)
    renderBody(g)
  }

  def renderBody(g: slick.Graphics) {
    import GL._

    translate(x, y) {
      rotate(0, 0, angle.toDegrees) {
        scale(imageScale, imageScale) {
          image.draw(-image.getWidth/2f, -image.getHeight/2f)
        }
      }
    }
  }
  
  def renderTrail(g: slick.Graphics) {
    var prevX: Float = 0
    var prevY: Float = 0
    var t = stationaryTime
    
    for ((x, y, delta) <- trail) {
      if (t > trailLifetime) {
        return
      }
      
      if (prevX > 0 && Math.abs(x-prevX) < Main.GAME_WIDTH/2) {
        g.setColor(new slick.Color(1f, 1f, 1f, 0.5f - (t.toFloat / trailLifetime)*0.5f))
        g.setLineWidth(2f)
        g.setAntiAlias(true)
        line(x, y, prevX, prevY)
        g.setAntiAlias(false)
      }
      
      prevX = x
      prevY = y
      t += delta
    }
  }
  
  def updateFromTuple(tuple: (Int, Float, Float, Float, Float, Float, Float, Byte)) {
    val (id, x, y, xVel, yVel, rot, angVel, projectileType) = tuple
    
    this.x = x
    this.y = y
    this.angle = rot
  }

}
