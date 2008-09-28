import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

abstract class GameObject(session: Session) {
  var body = createBody
  loadShapes

  def collide(other: GameObject, contact: ContactPoint) {}
  def persist(other: GameObject, contact: ContactPoint) {}
  
  def shapes: List[ShapeDef]
  def bodyDef = new BodyDef
  
  def createBody = {
    session.createBody(this, bodyDef)
  }
  
  def loadShapes = {
    removeShapes
    for (shape <- shapes) {
      body.createShape(shape)
    }
  }
  
  def removeShapes = {
    var currentShape = body.getShapeList
    while (null != currentShape) {
      val nextShape = currentShape.getNext
      body.destroyShape(currentShape)
      currentShape = nextShape
    }
  }
}
