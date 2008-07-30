import org.newdawn.slick._
import sbinary.Instances._
import sbinary.Operations
import net.phys2d

class Explosion (var x: Float, var y: Float, var radius: Float, session: Session) {
  val LIFETIME = 10f //second
  var timeToDie = LIFETIME

  val SOUND = "explosion1.wav"
  if (session.isInstanceOf[Client]) {
    new SoundPlayer(SOUND).start
  }

  for (tank <- session.tanks) {
    val contacts = new Array[phys2d.raw.Contact](1)
    contacts(0) = new phys2d.raw.Contact
    val explodeBody = new phys2d.raw.StaticBody(new phys2d.raw.shapes.Circle(radius))
    explodeBody.setPosition(x, y)
    if (phys2d.raw.Collide.collide(contacts, explodeBody, tank.body, 0f) > 0) {
      println("Tank in explosion.")
    }
  }
    
  def update(delta: Int) {
    timeToDie -= delta/1000f
    if (timeToDie < 0) {
      session.removeExplosion(this)
    }
  }

  def render(g: Graphics) {
    g.setColor(new Color(0.5f, 0.5f, 0.8f, timeToDie/LIFETIME))
    g.fillOval(x - radius, y - radius, radius*2, radius*2)
  }

  def serialise = {
    Operations.toByteArray((
      x,
      y,
      radius
    ))
  }

  def loadFrom(data: Array[Byte]) = {
    val (newX, newY, newRadius) = Operations.fromByteArray[(Float, Float, Float)](data)
    x = newX
    y = newY
    radius = newRadius
  }
}
