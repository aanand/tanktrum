import org.newdawn.slick._

class Explosion (x: Float, y: Float, radius: Float, session: Session) {
  val LIFETIME = 1f //second
  var timeToDie = LIFETIME

  def update(delta: Int) {
    timeToDie -= delta/1000f
    if (timeToDie < 0) {
      session.removeExplosion(this)
    }
  }

  def render(g: Graphics) {
    g.setColor(new Color(0.5f, 0.5f, 0.8f))
    g.fillOval(x - radius, y - radius, radius*2, radius*2)
  }
}
