package client

import GL._
import shared._

import org.newdawn.slick
import org.newdawn.slick._
import org.newdawn.slick.geom._

import sbinary.Instances._
import sbinary.Operations

class Ground(width: Int, height: Int) extends GameObject() {
  val granularity = Config("ground.granularity").toInt
  val topsoilDepth = Config("ground.topsoilDepth").toFloat
  var points : Array[Vector2f] = _
  var drawShape : Shape = _
  var initialised = false


  def loadFrom(shortPoints: Array[Short]) = {
    var i = 0f
    points = shortPoints.map((s) => {
        val v = new Vector2f(i, s.toFloat/Math.MAX_SHORT*Main.GAME_WIDTH)
        i += granularity
        v
      }
    )
    initialised = true
  }

  def render(g: Graphics, image: Image) {
    color(1f, 1f, 1f)
    val imageTexture = image.getTexture
    val texWidth = imageTexture.getWidth
    val texHeight = imageTexture.getHeight

    texture(imageTexture.getTextureID) {
      triStrip {
        for (point <- points) {
          textureVertex(texWidth*point.x/width, texHeight*point.y/height)
          vertex(point.x, point.y)

          textureVertex(texWidth*point.x/width, texHeight)
          vertex(point.x, height)
        }
      }
    }

    renderShading
    renderOutline(g)
  }
  
  def renderShading {
    val lightAngle = Math.toRadians(135)
    val lightVector = new Vector2f(Math.sin(lightAngle).toFloat, -Math.cos(lightAngle).toFloat)
    val shadingDepth = 4f
    val shadingAlpha = 0.35f

    val projections = new Array[Float](points.length)
    
    for (i <- 0 until points.length-1) {
      val groundVector = new Vector2f(points(i+1).x - points(i).x, points(i+1).y - points(i).y)
      val shadeVector = new Vector2f
    
      groundVector.projectOntoUnit(lightVector, shadeVector)
      projections(i) = shadeVector.length
    }

    quadStrip {
      for (i <- 0 until points.length) {
        val projection = if (i == 0) {
          projections(0)
        } else if (i == points.length-1) {
          projections(i)
        } else {
          (projections(i-1) + projections(i)) / 2
        }

        val intensity = 1f - projection
        val alpha = Math.abs(projection * 2 - 1) * shadingAlpha
        
        color(intensity, intensity, intensity, alpha)
        vertex(points(i).x, points(i).y)
      
        color(intensity, intensity, intensity, 0f)
        vertex(points(i).x, points(i).y + shadingDepth)
      }
    }
  }

  def renderOutline(g: Graphics) {
    color(0f, 0f, 0f)
    g.setLineWidth(2)
    g.setAntiAlias(true)
    lines(points.map(point => (point.x, point.y)))
    g.setAntiAlias(false)
  }

}
