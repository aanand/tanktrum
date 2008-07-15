import org.newdawn.slick

class Ground(session : Session, width : Int, height : Int) {
  val color = new slick.Color(0.6f, 0.5f, 0.0f)
  
  var ready = false
  
  var points : Option[List[slick.geom.Vector2f]] = None
  var drawShape : Option[slick.geom.Shape] = None
  
  def buildPoints() {
    points = Some(List(new slick.geom.Vector2f(0f, (height/2.0).toFloat), new slick.geom.Vector2f(width, (height/2.0).toFloat)))

    initPoints()
  }
  
  def initPoints() {
    val shapePoints = points.get ++ List(new slick.geom.Vector2f(width, height), new slick.geom.Vector2f(0, height))
    
    val drawShapePoints = shapePoints.foldLeft[List[Float]](List())((list, v) => list ++ List(v.getX(), v.getY()))
    
    drawShape = Some(new slick.geom.Polygon(drawShapePoints.toArray))
    
    ready = true
  }
  
  def render(g : slick.Graphics) {
    if (!ready) return
    
    g.setColor(color)
    g.fill(drawShape.get)
  }
}