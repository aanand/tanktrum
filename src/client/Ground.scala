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
    initPoints()
  }

  def initPoints() {
    val shapePoints = (List(new Vector2f(-5, height), new Vector2f(-1, -height)) ++
                      points ++ 
                      List(new Vector2f(width+1, -height), new Vector2f(width+5, height))).toArray
    
    val drawShapePoints = new Array[float](shapePoints.length*2)
    for (i <- 0 until shapePoints.length) {
      drawShapePoints(i*2) = shapePoints(i).getX
      drawShapePoints(i*2+1) = shapePoints(i).getY
    }
    
    drawShape = new Polygon(drawShapePoints.toArray)
    
    initialised = true
  }

  def render(g: Graphics, image: Image) {
    g.setColor(new Color(1f, 1f, 1f))
    g.texture(drawShape, image, image.getTextureWidth/Main.GAME_WIDTH, image.getTextureHeight/Main.GAME_HEIGHT)

    renderShading(g)
    renderOutline(g)
  }
  
  def renderShading(g: Graphics) {
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
    g.setColor(new Color(0f, 0f, 0f))
    g.setLineWidth(2)
    g.setAntiAlias(true)
    for (i <- 0 until points.length-1) {
      line(points(i).x, points(i).y, points(i+1).x, points(i+1).y)
    }
    g.setAntiAlias(false)
  }

}
