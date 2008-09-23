import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

abstract class GameObject(session: Session) {
  val body = createBody

  def collide(other: GameObject, contact: ContactPoint) {}
  
  def shapes: List[ShapeDef]
  def bodyDef = new BodyDef
  
  def createBody = {
    println("Creating a " + getClass())
    session.createBody(this, bodyDef)
  }
  
  def addShapes = {
    shapes.foreach(body.createShape(_))
  }
  
  def removeShapes = {
    var currentShape = body.getShapeList
    while (null != currentShape) {
      body.destroyShape(currentShape)
      currentShape = currentShape.getNext
    }
  }
}
