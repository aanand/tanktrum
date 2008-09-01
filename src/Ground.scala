import org.newdawn.slick
import org.newdawn.slick._
import org.newdawn.slick.geom._

import net.phys2d

import sbinary.Instances._
import sbinary.Operations

class Ground(session : Session, width : Int, height : Int) extends Collider {
  val MIN_HEIGHT = 20
  val granularity = 5

  val topsoilColor = new Color(0.5f, 0.5f, 0f)
  val earthColor = new Color(0.8f, 0.8f, 0f)
  
  val topsoilDepth = 20f
  
  var points : Array[Vector2f] = _
  var drawShape : Shape = _
  var physShape : phys2d.raw.shapes.Shape = _
  var body : phys2d.raw.Body = _

  var doDeform: (Int, Int, Int) = _

  var initialised = false

  def buildPoints() {
    val rand = new Random()
    var x = 0f
    val p = new PerlinNoise(width/granularity + 1, 6, 0.95f)

    points = p.generate.map((h) => {
        val v = new Vector2f(x, height*(h+1)/2.0f)
        x += granularity
        v
      }
    ).toArray
    
    initPoints()
  }
  
  def initPoints() {
    val shapePoints = (List(new Vector2f(-20, height), new Vector2f(0, -height)) ++ 
                      points ++ 
                      List(new Vector2f(width, -height), new Vector2f(width+20, height), new Vector2f(-20, height))).toArray
    
    val drawShapePoints = new Array[float](shapePoints.length*2)
    for (i <- 0 until shapePoints.length) {
      drawShapePoints(i*2) = shapePoints(i).getX
      drawShapePoints(i*2+1) = shapePoints(i).getY
    }
    
    drawShape = new Polygon(drawShapePoints.toArray)
    
    val physShapePoints = shapePoints.map(p => new phys2d.math.Vector2f(p.x, p.y))
    
    physShape = new phys2d.raw.shapes.Polygon(physShapePoints.toArray)
    
    body = new phys2d.raw.StaticBody(physShape)
    body.setFriction(1f)
    session.addBody(this, body)
    
    initialised = true
  }

  def deform(x: Int, y : Int, radius: Int) {
    doDeform = (x, y, radius)
  }

  def update(delta: Int) {
    if (doDeform != null) {
      val (x, y, radius) = doDeform

      session.removeBody(body)

      for (i <- -radius/granularity until radius/granularity+1) {
        val pointInd = (x/granularity)+i
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
          if (points(pointInd).y > Main.HEIGHT-MIN_HEIGHT) {
            points(pointInd).y = Main.HEIGHT-MIN_HEIGHT
          }
        }
      }
      initPoints()
      doDeform = null
      if (session.isInstanceOf[Server]) {
        session.asInstanceOf[Server].broadcastGround()
      }
    }
  }
  
  def render(g: Graphics) {
    g.setColor(earthColor)
    g.fill(drawShape)
    
    new GL {
      quadStrip {
        for (p <- points) {
          color(topsoilColor.r, topsoilColor.g, topsoilColor.b, 1f)
          vertex(p.x, p.y)
          
          color(earthColor.r, earthColor.g, earthColor.b, 1f)
          vertex(p.x, p.y + topsoilDepth)
        }
      }
    }

    g.setColor(earthColor)
    g.fillRect(0, Main.HEIGHT-MIN_HEIGHT, Main.WIDTH, MIN_HEIGHT)
  }
  
  def serialise() = {
    Operations.toByteArray(points.map((p) => p.getY.toShort).toArray)
  }
  
  def loadFrom(data: Array[byte]) = {
    val shortPoints = Operations.fromByteArray[Array[short]](data)
    var i = 0f
    points = shortPoints.map((s) => {
        val v = new Vector2f(i, s)
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
