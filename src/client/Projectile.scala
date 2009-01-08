package client

import shared._
import shared.ProjectileTypes._

import org.newdawn.slick._

import sbinary.Instances._
import sbinary.Operations

import GL._

object Projectile {
  def create(projectileType: ProjectileTypes.Value): Projectile = {
    projectileType match {
      case PROJECTILE          => new Projectile
      case NUKE                => new Nuke
      case ROLLER              => new Roller
      case MIRV                => new Mirv
      case MIRV_CLUSTER        => new MirvCluster
      case CORBOMITE           => new Corbomite
      case MACHINE_GUN         => new MachineGun
      case DEATHS_HEAD         => new DeathsHead
      case DEATHS_HEAD_CLUSTER => new DeathsHeadCluster
      case MISSILE             => new Missile
    }
  }

  def newFromTuple(client: Client, tuple: (Int, Float, Float, Float, Byte)) = {
    val (id, _, _, _, projectileType) = tuple
    
    val p = create(ProjectileTypes(projectileType))
    p.id = id
    p.updateFromTuple(tuple)
    
    p
  }

  def deserialise(data: Array[byte]) = Operations.fromByteArray[(Int, Float, Float, Float, Byte)](data)

  def render(g: Graphics, value: Value) {
    g.setColor(new Color(1f, 1f, 1f))
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

class Projectile extends GameObject {
  var id: Int = -1
  
  var image: Image = _
  
  initImage
  
  val trailLifetime = Config("projectile.trail.lifetime").toInt
  var trail: List[(Float, Float, Int)] = Nil
  var stationaryTime = 0
  var dead = false

  def trailDead = stationaryTime > trailLifetime
    
  def name = getClass.getName.split("\\.").last
  def imagePath = Config("projectile." + name + ".imagePath")
  def imageWidth = Config("projectile." + name + ".imageWidth").toInt
  
  def update(delta : Int) {
    updateTrail(delta)
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
    image = new Image(imagePath)
  }
  
  def imageScale = (imageWidth.toFloat / image.getWidth) / Main.GAME_WINDOW_RATIO
 

  def render(g : Graphics) {
    renderTrail(g)
    if (!dead) {
      renderBody(g)
    }
  }

  def renderBody(g: Graphics) {
    import GL._

    translate(x, y) {
      rotate(0, 0, angle.toDegrees) {
        scale(imageScale, imageScale) {
          image.draw(-image.getWidth/2f, -image.getHeight/2f)
        }
      }
    }
  }
  
  def renderTrail(g: Graphics) {
    var prevX: Float = 0
    var prevY: Float = 0
    var t = stationaryTime
    
    for ((x, y, delta) <- trail) {
      if (t > trailLifetime) {
        return
      }
      
      if (prevX > 0 && Math.abs(x-prevX) < Main.GAME_WIDTH/2) {
        g.setColor(new Color(1f, 1f, 1f, 0.5f - (t.toFloat / trailLifetime)*0.5f))
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
  
  def updateFromTuple(tuple: (Int, Float, Float, Float, Byte)) {
    val (id, x, y, rot, projectileType) = tuple
    
    this.x = x
    this.y = y
    this.angle = rot
  }

}

class Nuke extends Projectile

class Mirv extends Projectile
class MirvCluster extends Projectile

class DeathsHead extends Mirv
class DeathsHeadCluster extends Projectile

class MachineGun extends Projectile
class Roller extends Projectile
class Missile extends MachineGun

class Corbomite extends Projectile
