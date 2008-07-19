import org.newdawn.slick
import net.phys2d

class Projectile(session : Session, tank : Tank, val body : phys2d.raw.Body, radius : Float) extends Collider {
  // val GRACE = 0.1
  val COLOR = new slick.Color(1.0f, 1.0f, 1.0f)
  
  // var grace = GRACE
  
  def x = body.getPosition.getX
  def y = body.getPosition.getY
  
  def update(container : slick.GameContainer, delta : Int) {
    if (x < 0 || x > container.getWidth || y > container.getHeight) {
      session.removeProjectile(this)
    }
    
    // if (grace > 0) {
    //   grace = grace - delta / 1000f
    // }
  }
  
  def render(container : slick.GameContainer, g : slick.Graphics) {
    g.setColor(COLOR)
    g.fillOval(x - radius, y - radius, radius*2, radius*2)
  }
  
  override def collide(obj : Collider, event : phys2d.raw.CollisionEvent) {
    // if (grace > 0) return
    
    session.removeProjectile(this)
    
    if (obj.isInstanceOf[Tank] && session.isInstanceOf[Server]) {
      val tank = obj.asInstanceOf[Tank]
      val server = session.asInstanceOf[Server]
      
      tank.damage(20)
      server.broadcastDamageUpdate(tank, 20)
    }
  }
}