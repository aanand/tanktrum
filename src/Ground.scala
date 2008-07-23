import org.newdawn.slick._
import org.newdawn.slick.geom._

import net.phys2d

import sbinary.Instances._
import sbinary.Operations

class Ground(session : Session, width : Int, height : Int) extends Collider {
  val granularity = 1

  val color = new Color(0.6f, 0.5f, 0.0f)
  
  var points : Seq[Vector2f] = _
  var drawShape : Shape = _
  var physShape : phys2d.raw.shapes.Shape = _
  var body : phys2d.raw.Body = _

  var doDeform: (Int, Int) = _

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

  def deform(x: Int, radius: Int) {
    doDeform = (x, radius)
  }
  
  def render(g: Graphics) {
    //TODO: This is messy and really in the wrong place...
    if (doDeform != null) {
      val x = doDeform._1
      val radius = doDeform._2
      session.removeBody(body)
      val point = points(x/granularity)
      
      for (i <- -radius until radius) {
        if (x+i >= 0 && x+i < points.length) {
          val dist = (radius - Math.pow(i, 2)/radius.toFloat).toFloat
          points(x+i).y += dist
        }
      }
      initPoints()
      doDeform = null
      if (session.isInstanceOf[Server]) {
        session.asInstanceOf[Server].broadcastGround()
      }
    }
    
    g.setColor(color)
    g.fill(drawShape)
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

  def heightAt(x: Double) = {
    val h1 = points((x/granularity).toInt).y
    val h2 = points((x/granularity + 1).toInt).y

    val r = x % granularity
    
    val f = r/granularity

    h1*(1-r) + h2*r
  }

  def normalAt(x: Float) = {
    0f
  }
}
