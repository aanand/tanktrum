import org.newdawn.slick._
import sbinary.Instances._
import sbinary.Operations

class Explosion (var x: Float, var y: Float, var radius: Float, session: Session) {
  val LIFETIME = 10f //second
  var timeToDie = LIFETIME

  val SOUND = "media/explosion.ogg"
  if (session.isInstanceOf[Client]) {
    new OggPlayer(SOUND).start
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
