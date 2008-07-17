import org.newdawn.slick
import java.util.Random
import slick.geom._
import sbinary.Instances._
import sbinary.Operations

class Ground(session : Session, width : Int, height : Int) {
  val granularity = 1

  val color = new slick.Color(0.6f, 0.5f, 0.0f)
  
  var points : Seq[slick.geom.Vector2f] = _
  var drawShape : Shape = _

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
  }
  
  def render(g : slick.Graphics) {
    g.setColor(color)
    g.fill(drawShape)
  }
  
  def serialise() = {
    Operations.toByteArray(points.map((p) => p.getX).toArray)
  }
}
