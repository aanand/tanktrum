import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

object Projectile {
  def deserialise(data: Array[byte]) = Operations.fromByteArray[(Int, Float, Float, Float, Float, Float, Float, Byte)](data)
  
  def newFromTuple(session: Session, tuple: (Int, Float, Float, Float, Float, Float, Float, Byte)) = {
    val (id, _, _, _, _, _, _, projectileType) = tuple
    
    val p = ProjectileTypes.newProjectile(session, null, ProjectileTypes(projectileType))
    p.id = id
    p.updateFromTuple(tuple)
    
    p
  }
}

class Projectile(session: Session, val tank: Tank) extends GameObject(session) {
  var id: Int = -1

  val color = new slick.Color(1.0f, 1.0f, 1.0f)
  val explosionRadius = 20f
  def radius = 3f
  val damage = 5
  val reloadTime = 4f
  val mass = 1f
  
  override def shapes: List[ShapeDef] = {
    val sDef = new CircleDef
    println("Creating projectile with radius: " + radius)
    sDef.radius = radius
    sDef.restitution = 0f
    sDef.density = 1f
    List(sDef)
  }

  override def bodyDef = {
    val bDef = new BodyDef
    bDef.isBullet = true
    bDef
  }
  
  val trailLifetime = Config("projectile.trail.lifetime").toInt

  var trail: List[(Float, Float, Int)] = Nil
  var stationaryTime = 0
  var dead = false
  
  addShapes

  def trailDead = stationaryTime > trailLifetime

  if (session.isInstanceOf[Server]) {
    body.setMassFromShapes
  }

  //body.addExcludedBody(session.ground.body)

  val projectileType = ProjectileTypes.PROJECTILE

  var destroy = false

  def x = body.getPosition.x
  def y = body.getPosition.y
  
  def update(delta : Int) {
    if (session.isInstanceOf[Client]) {
      updateTrail(delta)
    }
    else {
      if (x < 0 || x > Main.WIDTH || y > Main.HEIGHT || destroy) {
        session.removeProjectile(this)
      }
      if (y + radius > session.ground.heightAt(x)) {
        //collide(session.ground, null)
      }
    }
  }
  
  def updateTrail(delta: Int) {
    if (shouldDrawTrail) {    
      trail = (x, y, delta + stationaryTime) :: trail
      stationaryTime = 0
    } else {
      stationaryTime += delta
    }
  }
  
  def shouldDrawTrail: Boolean = {
    if (!trail.isEmpty) {
      val (lastX, lastY, _) = trail.head

      if ((x, y) == (lastX, lastY)) {
        return false
      }
    }
    
    return true
  }
  
  def render(g : slick.Graphics) {
    if (!dead) {
      renderBody(g)
    }
    
    renderTrail(g)
  }

  def renderBody(g: slick.Graphics) {
    g.setColor(color)
    g.fillOval(x - radius, y - radius, radius*2, radius*2)
  }
  
  def renderTrail(g: slick.Graphics) {
    var prevX: Float = 0
    var prevY: Float = 0
    var t = stationaryTime
    
    for ((x, y, delta) <- trail) {
      if (t > trailLifetime) {
        return
      }
      
      if (prevX > 0) {
        g.setColor(new slick.Color(color.r, color.g, color.b, 0.5f - (t.toFloat / trailLifetime)*0.5f))
        g.drawLine(x, y, prevX, prevY)
      }
      
      prevX = x
      prevY = y
      t += delta
    }
  }
  
  override def collide(obj: GameObject, contact: ContactPoint) {
    if (!obj.isInstanceOf[Projectile]) {
      explode(obj)
    }
  }
  
  def explode(obj: GameObject) {
    if (destroy) {
      return
    }
    
    destroy = true

    if (session.isInstanceOf[Server]) {
      session.addExplosion(x, y, explosionRadius, this)
      session.ground.deform(x.toInt, y.toInt, explosionRadius.toInt)
    }
    
    if (obj.isInstanceOf[Tank] && session.isInstanceOf[Server]) {
      val hitTank = obj.asInstanceOf[Tank]
      val server = session.asInstanceOf[Server]
      
      hitTank.damage(damage, this)
      if (tank != null && tank.player != null) {
        println(tank.player.name + " hit " + hitTank.player.name + " directly with a " + this.getClass.getName + " for " + damage + " damage.")
      }
    }
  }
  
  def onRemove {
    session.removeBody(body)
  }

  def serialise = {
    Operations.toByteArray((
      id,
      x,
      y,
      body.getLinearVelocity.x,
      body.getLinearVelocity.y,
      body.getAngle,
      body.getAngularVelocity,
      projectileType.id.toByte
    ))
  }
  
  def updateFromTuple(tuple: (Int, Float, Float, Float, Float, Float, Float, Byte)) {
    val (id, x, y, xVel, yVel, rot, angVel, projectileType) = tuple
    
    val velocityDelta = new Vec2(xVel, yVel)

    body.setXForm(new Vec2(x, y), rot)
    body.setLinearVelocity(new Vec2(xVel, yVel))
    body.setAngularVelocity(angVel)
  }
}

object ProjectileTypes extends Enumeration {
  val PROJECTILE, NUKE, ROLLER, MIRV, MIRV_CLUSTER, CORBOMITE, 
      MACHINE_GUN, DEATHS_HEAD, DEATHS_HEAD_CLUSTER = Value

  def newProjectile(session: Session, tank: Tank, projectileType: Value) : Projectile = {
    projectileType match {
      case PROJECTILE          => new Projectile(session, tank) 
      case NUKE                => new Nuke(session, tank) 
      case ROLLER              => new Roller(session, tank) 
      case MIRV                => new Mirv(session, tank) 
      case MIRV_CLUSTER        => new MirvCluster(session, tank)
      case CORBOMITE           => new Corbomite(session, tank)
      case MACHINE_GUN         => new MachineGun(session, tank) 
      case DEATHS_HEAD         => new DeathsHead(session, tank)
      case DEATHS_HEAD_CLUSTER => new DeathsHeadCluster(session, tank)
    }
  }

  def render(g: slick.Graphics, value: Value) {
    g.setColor(new slick.Color(1f, 1f, 1f))
    value match {
      case PROJECTILE => {
        g.fillOval(-3, -3, 6, 6)
      }

      case NUKE => {
        g.fillOval(-6, -6, 12, 12)
      }

      case ROLLER => {
        g.fillOval(-3, -3, 6, 6)
        g.fillRect(-7, 3, 14, 4)
      }

      case MIRV => {
        g.fillOval(-4, -4, 4, 4)
        g.fillOval(0, 0, 4, 4)
        g.fillOval(-4, 0, 4, 4)
        g.fillOval(0, -4, 4, 4)
      }

      case MACHINE_GUN => {
        g.fillRect(-2, -4, 4, 8)
      }

      case DEATHS_HEAD =>  {
        g.fillOval(-8, -8, 8, 8)
        g.fillOval(0, 0, 8, 8)
        g.fillOval(-8, 0, 8, 8)
        g.fillOval(0, -8, 8, 8)
      }
    }
  }
}
