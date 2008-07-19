import org.newdawn.slick
import net.phys2d
import scala.collection.mutable.HashMap

abstract class Session(container: slick.GameContainer) {
  val world = new phys2d.raw.World(new phys2d.math.Vector2f(0.0f, 100.0f), 10)
  var ground: Ground = _
  var active = false
  var tanks = List[Tank]()
  val bodies = new HashMap[Object, phys2d.raw.Body]

  def enter() {
    ground = new Ground(this, container.getWidth(), container.getHeight())
    active = true
  }
  
  def leave() {
    active = false
  }
  
  def render(g: slick.Graphics) {
    if (ground.initialised) {
      ground.render(g)
    }
    for (tank <- tanks) {
      tank.render(g)
    }
  }
  
  def update(delta: Int) {
    for (tank <- tanks) {
      tank.update(delta)
    }
  }
  
  def keyPressed(key : Int, char : Char) {
  }

  def charToByteArray(c: Char) = {
    val a = new Array[byte](1)
    a(0) = c.toByte
    a
  }

  def addBody(obj: Object, body: phys2d.raw.Body) = {
    world.add(body)
    bodies.put(this, body)
    bodies
  }

  def getGround = ground

  def isActive = active
}
