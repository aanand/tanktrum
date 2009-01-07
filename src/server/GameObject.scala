package server
import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

abstract class GameObject(server: Server) {
  var body = createBody
  loadShapes

  def collide(other: GameObject, contact: ContactPoint) {}
  def persist(other: GameObject, contact: ContactPoint) {}
  
  def x = body.getPosition.x
  def y = body.getPosition.y
  
  def shapes: List[ShapeDef]
  def bodyDef = new BodyDef
  
  def createBody = {
    server.createBody(this, bodyDef)
  }
  
  def loadShapes = {
    removeShapes
    if (null != shapes) {
      for (shape <- shapes) {
        body.createShape(shape)
      }
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
