package server

import shared._

import scala.collection.mutable.Queue

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

import sbinary.Instances._
import sbinary.Operations


class Ground(server: Server, width: Int, height: Int) extends GameObject(server) {
  val MIN_HEIGHT = Config("ground.minHeight").toFloat
  val granularity = Config("ground.granularity").toInt
  val FLATNESS_WIDTH = 10
  val TANK_WIDTH = Config("tank.width").toInt

  var points : Array[Vec2] = _
  
  var physShapes: List[ShapeDef] = _
  override def shapes = physShapes

  var deformQueue = new Queue[(Float, Float, Float)]

  var initialised = false
    
  val friction = 1f
  val restitution = 0.0f

  def buildPoints() {
    val rand = new Random()
    var x = 0f
    val p = new PerlinNoise(width/granularity, 12, 0.95f)
    
    points = p.generate.map((h) => {
        val v = new Vec2(x, height*(h+1)/2.0f)
        x += granularity
        v
      }
    ).toArray
    
    initPoints()
  }
  
  def initPoints() {
    val shapePoints = (List(new Vec2(-5, height), new Vec2(-1, -height)) ++
                      points ++ 
                      List(new Vec2(width+1, -height), new Vec2(width+5, height))).toArray

    val physShapePoints = shapePoints.map(p => new Vec2(p.x, p.y))
    
    physShapes = List[ShapeDef]()
    
    for (i <- 0 until physShapePoints.length-1) {
      val polyDef = new PolygonDef
      val vert1 = physShapePoints(i+1)
      val vert2 = physShapePoints(i)
      val vert3 = new Vec2(vert2.x, height)
      val vert4 = new Vec2(vert1.x, height)
      polyDef.addVertex(vert4)
      polyDef.addVertex(vert3)
      polyDef.addVertex(vert2)
      polyDef.addVertex(vert1)
      polyDef.friction = friction
      polyDef.restitution = restitution
      physShapes += polyDef
    }
    
    loadShapes

    initialised = true
  }

  def deform(x: Float, y: Float, radius: Float) {
    deformQueue += (x, y, radius)
  }

  def update(delta: Int) {
    while (!deformQueue.isEmpty) {
      val (x, y, radius) = deformQueue.dequeue
      
      var i = -radius/granularity
      while (i < radius/granularity+1) {
        
        val pointInd = (x/granularity+i).toInt

        if (pointInd >= 0 && pointInd < points.length) {
          val groundHeight = points(pointInd).y

          val yOffset = Math.sqrt(Math.pow(radius,2) - Math.pow(i*granularity - x%granularity,2))
          val yTop = y - yOffset
          val yBottom = y + yOffset

          if (yBottom > groundHeight) {
            if (yTop < groundHeight) {
              points(pointInd).y += (yBottom - groundHeight).toFloat
            } else {
              points(pointInd).y += (yBottom - yTop).toFloat
            }
          }
          if (points(pointInd).y > Main.GAME_HEIGHT-MIN_HEIGHT) {
            points(pointInd).y = Main.GAME_HEIGHT-MIN_HEIGHT
          }
        }
        i += granularity
      }
      initPoints()
      server.broadcastGround()
    }
  }

  def flatten(x: Float) {
    val h = heightAt(x)
    
    val range = new Range(x.toInt - FLATNESS_WIDTH/2, x.toInt + FLATNESS_WIDTH/2, 1)
    
    for (i <- range) {
      val index = (i/granularity).toInt + 1
      points(index).y = h
    }
    initPoints
  }
  
  def serialise(seq: Short) = {
    Operations.toByteArray((seq, points.map((p) => (p.y/Main.GAME_WIDTH*Math.MAX_SHORT).toShort).toArray))
  }


  def heightAt(x: Float): Float = {
    if (null == points) {
      return 0f
    }

    val i = (x/granularity).toInt
    
    if (i < 0) {
      return heightAt(0)
    } else if (i >= points.length-1) {
      return heightAt(points.length-2)
    }
    
    val h1 = points(i).y
    val h2 = points(i+1).y

    val dist: Float = x % granularity //Pixels right of h1.
    
    val factor = dist/granularity //Interpolation factor.

    h1*(1-factor) + h2*factor
  }

  override def collide(other: GameObject, contact: ContactPoint) {
    var power = other.body.getLinearVelocity.length * other.body.getMass
    //println("Impact with ground of power: " + power)
    if (other.isInstanceOf[Projectile]) {
      power *= 4
    }
    if (power > 500) {
      server.broadcastImpact(contact.position.x, contact.position.y, power)
    }
  }
}
