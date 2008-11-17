import org.newdawn.slick
import org.newdawn.slick._
import org.newdawn.slick.geom._

import scala.collection.mutable.Queue

import sbinary.Instances._
import sbinary.Operations

import org.jbox2d.collision.ShapeDef
import org.jbox2d.collision.PolygonDef
import org.jbox2d.common._

class Ground(session : Session, width : Int, height : Int) extends GameObject(session) {
  val MIN_HEIGHT = Config("ground.minHeight").toFloat
  val granularity = Config("ground.granularity").toInt

  val topsoilDepth = Config("ground.topsoilDepth").toFloat
  
  var points : Array[Vector2f] = _
  var drawShape : Shape = _
  
  var physShapes: List[ShapeDef] = _
  override def shapes = {
    if (null == physShapes) {
      List()
    }
    else {
      physShapes
    }
  }

  var deformQueue = new Queue[(Float, Float, Float)]

  var initialised = false
    
  val friction = 1f
  val restitution = 0.0f

  def buildPoints() {
    val rand = new Random()
    var x = 0f
    val p = new PerlinNoise(width/granularity, 6, 0.95f)
    
    points = p.generate.map((h) => {
        val v = new Vector2f(x, height*(h+1)/2.0f)
        x += granularity
        v
      }
    ).toArray
    
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
      if (session.isInstanceOf[Server]) {
        session.asInstanceOf[Server].broadcastGround()
      }
    }
  }
  
  def render(g: Graphics, image: Image) {
    val lightAngle = Math.toRadians(135)
    val lightVector = new slick.geom.Vector2f(Math.sin(lightAngle).toFloat, -Math.cos(lightAngle).toFloat)
    val shadingDepth = 2f
    val shadingAlpha = 0.5f
    
    g.setColor(new Color(1f, 1f, 1f))
    g.texture(drawShape, image, image.getTextureWidth/Main.GAME_WIDTH, image.getTextureHeight/Main.GAME_HEIGHT)

    import GL._

    quadStrip {
      for (i <- 0 until points.length-1) {
        val groundVector = new slick.geom.Vector2f(points(i+1).x - points(i).x, points(i+1).y - points(i).y)
        val shadeVector = new slick.geom.Vector2f
      
        groundVector.projectOntoUnit(lightVector, shadeVector)
      
        val intensity = 1f - shadeVector.length
        val alpha = Math.abs(shadeVector.length * 2 - 1) * shadingAlpha
      
        color(intensity, intensity, intensity, alpha)
        vertex(points(i).x, points(i).y)
        
        color(intensity, intensity, intensity, 0f)
        vertex(points(i).x, points(i).y + shadingDepth)
      
        color(intensity, intensity, intensity, alpha)
        vertex(points(i+1).x, points(i+1).y)
        
        color(intensity, intensity, intensity, 0f)
        vertex(points(i+1).x, points(i+1).y + shadingDepth)
      }
    }
  }
  
  def serialise(seq: Short) = {
    Operations.toByteArray((seq, points.map((p) => (p.getY/Main.GAME_WIDTH*Math.MAX_SHORT).toShort).toArray))
  }
  
  def loadFrom(shortPoints: Array[Short]) = {
    if (null != body) {
      session.removeBody(body)
    }
    var i = 0f
    points = shortPoints.map((s) => {
        val v = new Vector2f(i, s.toFloat/Math.MAX_SHORT*Main.GAME_WIDTH)
        i += granularity
        v
      }
    )
    initPoints()
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
}
