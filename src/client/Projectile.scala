package client

import shared._
import shared.ProjectileTypes._

import org.newdawn.slick._

import sbinary.Instances._
import sbinary.Operations

import GL._

object Projectile {
  def create(client: Client, projectileType: ProjectileTypes.Value): Projectile = {
    projectileType match {
      case PROJECTILE          => new Projectile(client)
      case NUKE                => new Nuke(client)
      case ROLLER              => new Roller(client)
      case MIRV                => new Mirv(client)
      case MIRV_CLUSTER        => new MirvCluster(client)
      case CORBOMITE           => new Corbomite(client)
      case MACHINE_GUN         => new MachineGun(client)
      case DEATHS_HEAD         => new DeathsHead(client)
      case DEATHS_HEAD_CLUSTER => new DeathsHeadCluster(client)
      case MISSILE             => new Missile(client)
    }
  }

  def newFromTuple(client: Client, tuple: (Int, Float, Float, Float, Byte)) = {
    val (id, _, _, _, projectileType) = tuple
    
    val p = create(client, ProjectileTypes(projectileType))
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

class Projectile(client: Client) extends GameObject {
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
  def imageSize = Config("projectile." + name + ".imageSize").toInt
  
  /*Two updates containing a projectile are required before we can do
    interpolation on its position, if we draw it before then it will look like
    it changes velocity oddly.*/
  var readyToInterpolate = false

  var lastX = -1f
  var lastY = -1f

  var interpX = 0f
  var interpY = 0f
  
  val INTERPOLATION_TIME = Config("projectile.interpolationTime").toInt
  
  def previousUpdate = client.previousProjectileUpdate
  def currentUpdate = client.currentProjectileUpdate
  
  def update(delta : Int) {
    if (readyToInterpolate) {
      updateTrail(delta)
    }
  }

  def updateTrail(delta: Int) {
    if (shouldUpdateTrail) {
      trail = (interpX, interpY, delta + stationaryTime) :: trail
      stationaryTime = 0
    } else {
      stationaryTime += delta
    }
  }
  
  def shouldUpdateTrail: Boolean = {
    if (dead) {
      return false
    }

    if (!trail.isEmpty) {
      val (prevX, prevY, _) = trail.head

      if (Math.abs(interpX-prevX) < 1 && Math.abs(interpY-prevY) < 1) {
        return false
      }
    }

    //Stops trail drawing from top left of the screen before interpolation settles.
    if (interpX < 0.01f || interpY < 0.01f) {
      return false
    }
    
    return true
  }
  
  def initImage {
    image = new Image(imagePath)
  }
  
  def imageScale = (imageSize.toFloat / image.getWidth) / Main.gameWindowWidthRatio

  def render(g : Graphics) {
    if (readyToInterpolate) {
      interpolatePosition
      renderTrail(g)
      if (!dead) {
        renderBody(g)
      }
    }
  }

  def interpolatePosition {
    val time = System.currentTimeMillis
    val interpTime = time - INTERPOLATION_TIME

    if (x == lastX && y == lastY) {
      interpX = x
      interpY = y
    }
    else if (interpTime > currentUpdate) {
      interpX = x
      interpY = y
    }
    else if (interpTime < previousUpdate) {
      interpX = lastX
      interpY = lastY
    }
    else if (lastX != -1f && lastY != -1f) {
      val interpFactor = (currentUpdate - interpTime) / (currentUpdate - previousUpdate).toFloat
      interpX = lastX * interpFactor + x * (1-interpFactor)
      interpY = lastY * interpFactor + y * (1-interpFactor)
    }
    else {
      interpX = x
      interpY = y
    }
  }

  def renderBody(g: Graphics) {
    import GL._

    translate(interpX, interpY) {
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
    g.setLineWidth(2f)
    g.setAntiAlias(true)
    
    for ((x, y, delta) <- trail) {
      if (t > trailLifetime) {
        return
      }

      if (prevX > 0 && Math.abs(x-prevX) < Main.GAME_WIDTH/2) {
        g.setColor(new Color(1f, 1f, 1f, 0.5f - (t.toFloat / trailLifetime)*0.5f))
        line(x, y, prevX, prevY)
      }
    
      prevX = x
      prevY = y
      t += delta
    }
    g.setAntiAlias(false)
  }
  
  def updateFromTuple(tuple: (Int, Float, Float, Float, Byte)) {
    val (id, newX, newY, newAngle, projectileType) = tuple
    
    lastX = x
    lastY = y
    
    x = newX
    y = newY
    angle = newAngle

    if (lastX != -1f && lastY != -1f) {
      readyToInterpolate = true
    }
  }

}

class Nuke(client: Client) extends Projectile(client)

class Mirv(client: Client) extends Projectile(client)
class MirvCluster(client: Client) extends Projectile(client)

class DeathsHead(client: Client) extends Mirv(client)
class DeathsHeadCluster(client: Client) extends Projectile(client)

class MachineGun(client: Client) extends Projectile(client)
class Roller(client: Client) extends Projectile(client)
class Missile(client: Client) extends MachineGun(client)

class Corbomite(client: Client) extends Projectile(client)
