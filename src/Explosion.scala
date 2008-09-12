import org.newdawn.slick._
import sbinary.Instances._
import sbinary.Operations
import net.phys2d

class Explosion (var x: Float, var y: Float, var radius: Float, session: Session, projectile: Projectile) {
  val LIFETIME = 10f //second
  var timeToDie = LIFETIME

  val SOUND = "explosion1.wav"
  if (session.isInstanceOf[Client]) {
    SoundPlayer ! PlaySound(SOUND)
  }
  else {
    val explodeBody = new phys2d.raw.StaticBody(new phys2d.raw.shapes.Circle(radius))
    explodeBody.setPosition(x, y)
    for (tank <- session.tanks) {
      if (tank.isAlive) {
        val contacts = new Array[phys2d.raw.Contact](10)
        for (i <- 0 until contacts.length) contacts(i) = new phys2d.raw.Contact
        val numContacts = phys2d.raw.Collide.collide(contacts, explodeBody, tank.body, 0f) 
        
        if (numContacts > 0) {
          var maxOverlap = 0f
          for (i <- 0 until numContacts) {
            if (-contacts(i).getSeparation > maxOverlap) {
              maxOverlap = -contacts(i).getSeparation
            }
          }
          val damage = maxOverlap.toInt
          tank.damage(damage, projectile)
          if (null != projectile) {
            println(projectile.tank.player.name + " hit " + tank.player.name + " with splash damage from a " + projectile.getClass.getName + " for " + damage + " damage.")
            projectile.tank.player.awardHit(tank, damage)
          }
        }
      }
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
