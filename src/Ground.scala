import org.newdawn.slick
import org.newdawn.slick._
import org.newdawn.slick.geom._

import net.phys2d

import sbinary.Instances._
import sbinary.Operations

class Ground(session : Session, width : Int, height : Int) extends Collider {
  val granularity = 1

  val topsoilColor = new Color(0.5f, 0.5f, 0f)
  val color = new Color(0.8f, 0.8f, 0f)
  
  val topsoilDepth = 20f
  
  var points : Seq[Vector2f] = _
  var drawShape : Shape = _
  var physShape : phys2d.raw.shapes.Shape = _
  var body : phys2d.raw.Body = _

  var doDeform: (Int, Int, Int) = _

  var initialised = false

  def buildPoints() {
    val rand = new Random()
    var x = 0f
    val p = new PerlinNoise(width/granularity, 6, 0.95f)

    points = p.generate.map((h) => {
        val v = new Vector2f(x, height*(h+1)/2.0f)
        x += granularity
        v
      }
    )
    
    initPoints()
  }
  
  def initPoints() {
    val shapePoints = List(new Vector2f(0, height)) ++ points ++ List(new Vector2f(width, height), new Vector2f(0, height))
    
    val drawShapePoints = shapePoints.foldLeft[List[Float]](List())((list, v) => list ++ List(v.getX(), v.getY()))
    
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

      val point = points(x/granularity)
      
      for (i <- -radius until radius) {
        if (x+i >= 0 && x+i < points.length) {
          val groundHeight = heightAt(x+i)

          val yOffset = Math.sqrt(Math.pow(radius,2) - Math.pow(i,2))
          val yTop = y - yOffset
          val yBottom = y + yOffset

          if (yBottom > groundHeight) {
            if (yTop < groundHeight) {
              points(x+i).y += (yBottom - groundHeight).toFloat
            } else {
              points(x+i).y += (yBottom - yTop).toFloat
            }
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
    g.setColor(color)
    g.fill(drawShape)
    
    import slick.opengl.renderer.SGL
    
    new GL {
      def draw(gl : SGL) {
        import gl._
        
        shape(org.lwjgl.opengl.GL11.GL_QUAD_STRIP) {
          for (p <- points) {
            glColor4f(topsoilColor.r, topsoilColor.g, topsoilColor.b, 1f)
            glVertex2f(p.x, p.y)
            glColor4f(color.r, color.g, color.b, 1f)
            glVertex2f(p.x, p.y + topsoilDepth)
          }
        }
      }
    }
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

  def heightAt(x: Double): Double = {
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

    val r = x % granularity
    
    val f = r/granularity

    h1*(1-r) + h2*r
  }

  def normalAt(x: Float) = {
    0f
  }
}
