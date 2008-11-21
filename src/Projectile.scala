import org.newdawn.slick
import sbinary.Instances._
import sbinary.Operations

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

import GL._

object Projectile {
  val antiGravity = Config("physics.projectileGravity").toFloat - Config("physics.gravity").toFloat

  def deserialise(data: Array[byte]) = Operations.fromByteArray[(Int, Float, Float, Float, Float, Float, Float, Byte)](data)
  
  def newFromTuple(session: Session, tuple: (Int, Float, Float, Float, Float, Float, Float, Byte)) = {
    val (id, _, _, _, _, _, _, projectileType) = tuple
    
    val p = ProjectileTypes.newProjectile(session, null, ProjectileTypes(projectileType))
    p.id = id
    p.updateFromTuple(tuple)
    
    p
  }
}

class Projectile(session: Session, val tank: ServerTank) extends GameObject(session) {
  var id: Int = -1

  def color = new slick.Color(1f, 1f, 1f)

  var image: slick.Image = _

  def imagePath = Config("projectile.imagePath")
  def imageWidth = Config("projectile.imageWidth").toInt
  def round = Config("projectile.round").toBoolean

  val explosionRadius = 4f
  val explosionDamageFactor = 1f
  lazy val radius = 0.6f
  val damage = 5
  val reloadTime = 4f

  var collidedWith: GameObject = _
  
  override def shapes: List[ShapeDef] = {
    val sDef = new CircleDef
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
  
  def trailDead = stationaryTime > trailLifetime

  if (session.isInstanceOf[Server]) {
    body.setMassFromShapes
  } else {
    initImage
  }

  //body.addExcludedBody(session.ground.body)

  val projectileType = ProjectileTypes.PROJECTILE

  var destroy = false

  def x = body.getPosition.x
  def y = body.getPosition.y
  
  def update(delta : Int) {
    if (null != collidedWith) {
      explode(collidedWith)
    }
    body.applyForce(new Vec2(0, Projectile.antiGravity * body.getMass), body.getPosition)
    if (session.isInstanceOf[Client]) {
      updateTrail(delta)
    }
    else {
      if (y > Main.GAME_HEIGHT || destroy) {
        session.removeProjectile(this)
      }
      else if (y + radius > session.ground.heightAt(x) || 
               x < 0 || 
               x > Main.GAME_WIDTH) {
        collide(session.ground, null)
      }
      
      if (!round) {
        body.setXForm(body.getPosition, (Math.atan2(body.getLinearVelocity.x, -body.getLinearVelocity.y)).toFloat)
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
  
  def initImage {
    image = new slick.Image(imagePath)
  }
  
  def imageScale = (imageWidth.toFloat / image.getWidth) / Main.GAME_WINDOW_RATIO
  
  def render(g : slick.Graphics) {
    renderTrail(g)
    if (!dead) {
      renderBody(g)
    }
  }

  def renderBody(g: slick.Graphics) {
    import GL._

    translate(x, y) {
      rotate(0, 0, body.getAngle.toDegrees) {
        scale(imageScale, imageScale) {
          image.draw(-image.getWidth/2f, -image.getHeight/2f)
        }
      }
    }
  }
  
  def renderTrail(g: slick.Graphics) {
    var prevX: Float = 0
    var prevY: Float = 0
    var t = stationaryTime
    
    for ((x, y, delta) <- trail) {
      if (t > trailLifetime) {
        return
      }
      
      if (prevX > 0 && Math.abs(x-prevX) < Main.GAME_WIDTH/2) {
        g.setColor(new slick.Color(1f, 1f, 1f, 0.5f - (t.toFloat / trailLifetime)*0.5f))
        g.setLineWidth(2f)
        g.setAntiAlias(true)
        line(x, y, prevX, prevY)
        g.setAntiAlias(false)
      }
      
      prevX = x
      prevY = y
      t += delta
    }
  }
  
  override def collide(obj: GameObject, contact: ContactPoint) {
    if (!obj.isInstanceOf[Projectile] && !obj.isInstanceOf[Explosion]) {
      collidedWith = obj
    }
  }
  
  def explode(obj: GameObject) {
    if (destroy) {
      return
    }
    
    destroy = true

    if (session.isInstanceOf[Server]) {
      session.addExplosion(x, y, explosionRadius, this, explosionDamageFactor)
      session.ground.deform(x, y, explosionRadius)
    }
    
    if (obj.isInstanceOf[ServerTank] && session.isInstanceOf[Server]) {
      val hitTank = obj.asInstanceOf[ServerTank]
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
      MACHINE_GUN, DEATHS_HEAD, DEATHS_HEAD_CLUSTER, MISSILE = Value

  def newProjectile(session: Session, tank: ServerTank, projectileType: Value) : Projectile = {
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
      case MISSILE             => new Missile(session, tank)
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

      case MISSILE => {
        g.fillRect(-3, -6, 6, 12)
        g.fillOval(-3, -9, 6, 6)
      }
    }
  }
}
