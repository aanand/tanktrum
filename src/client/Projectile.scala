package client

import shared._
import shared.ProjectileTypes._

import org.newdawn.slick._

import sbinary.Instances._
import sbinary.Operations

import GL._

object Projectile {
  val maxIconWidth = Config("projectile.maxIconWidth").toInt
  val maxIconHeight = Config("projectile.maxIconHeight").toInt

  val classNames = Map(
    PROJECTILE -> nameForClass(classOf[Projectile]),
    NUKE -> nameForClass(classOf[Nuke]),
    ROLLER -> nameForClass(classOf[Roller]),
    MIRV -> nameForClass(classOf[Mirv]),
    MIRV_CLUSTER -> nameForClass(classOf[MirvCluster]),
    CORBOMITE -> nameForClass(classOf[Corbomite]),
    MACHINE_GUN -> nameForClass(classOf[MachineGun]),
    DEATHS_HEAD -> nameForClass(classOf[DeathsHead]),
    DEATHS_HEAD_CLUSTER -> nameForClass(classOf[DeathsHeadCluster]),
    MISSILE -> nameForClass(classOf[Missile])
  )

  val sprites = new scala.collection.mutable.HashMap[String, Image]
  val icons   = new scala.collection.mutable.HashMap[String, Image]

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

  def generateSprites {
    for (name <- classNames.values) {
      val imageIcon = Resource.getImageIcon(imagePathForName(name))
      val awtImage = imageIcon.getImage

      val targetWidth = (imageSizeForName(name) * Main.gameWindowWidthRatio).toInt
      val targetHeight = imageIcon.getIconHeight * targetWidth / imageIcon.getIconWidth

      val sprite = generateSprite(awtImage, targetWidth, targetHeight)

      sprites.put(name, sprite)

      if (targetWidth < maxIconWidth && targetHeight < maxIconHeight) {
        icons.put(name, sprite)
      } else {
        val iconScale = Math.min(maxIconWidth.toFloat / imageIcon.getIconWidth, maxIconHeight.toFloat / imageIcon.getIconHeight)

        val targetIconWidth  = (imageIcon.getIconWidth * iconScale).toInt
        val targetIconHeight = (imageIcon.getIconHeight * iconScale).toInt

        icons.put(name, generateSprite(awtImage, targetIconWidth, targetIconHeight))
      }
    }
  }

  def generateSprite(awtImage: java.awt.Image, targetWidth: Int, targetHeight: Int) = {
    val bufferedAwtImage = new java.awt.image.BufferedImage(targetWidth, targetHeight, java.awt.image.BufferedImage.TYPE_INT_ARGB)
    val graphics = bufferedAwtImage.createGraphics

    graphics.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION, java.awt.RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    graphics.drawImage(awtImage, 0, 0, targetWidth, targetHeight, null)

    val texture = util.BufferedImageUtil.getTexture("", bufferedAwtImage)
    val slickImage = new Image(texture.getImageWidth, texture.getImageHeight)

    slickImage.setTexture(texture)
    slickImage
  }

  def nameForClass(klass: Class[_]) = klass.getName.split("\\.").last

  def imageForName(name: String)      = sprites(name)
  def imageScaleForName(name: String) = imageSizeForName(name).toFloat / imageForName(name).getWidth

  def imagePathForName(name: String)  = Config("projectile." + name + ".imagePath")
  def imageSizeForName(name: String)  = Config("projectile." + name + ".radius").toFloat * 2

  def render(g: Graphics, value: Value, color: Color) {
    val name = classNames(value)
    val image = icons(name)

    image.draw((-image.getWidth/2f).toInt, (-image.getHeight/2f).toInt, color)
  }
}

class Projectile(client: Client) extends GameObject {
  var id: Int = -1
  
  val trailLifetime = Config("projectile.trail.lifetime").toInt
  var trail: List[(Float, Float, Int)] = Nil
  var stationaryTime = 0
  var dead = false

  def trailDead = stationaryTime > trailLifetime
    
  val name       = Projectile.nameForClass(getClass)
  val image      = Projectile.imageForName(name)
  val imageScale = Projectile.imageScaleForName(name)

  /*Two updates containing a projectile are required before we can do
    interpolation on its position, if we draw it before then it will look like
    it changes velocity oddly.*/
  var readyToInterpolate = false

  var lastX = -1f
  var lastY = -1f
  var lastAngle = -1f

  var interpX = 0f
  var interpY = 0f
  var interpAngle = 0f
  
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
    if (interpX < 0.01f && interpY < 0.01f) {
      return false
    }
    
    return true
  }
  
  def render(g : Graphics, color: Color) {
    if (readyToInterpolate) {
      interpolatePosition
      renderTrail(g)
      if (!dead) {
        renderBody(g, color)
      }
    }
  }

  def interpolatePosition {
    val time = System.currentTimeMillis
    val interpTime = time - INTERPOLATION_TIME

    if (x == lastX && y == lastY) {
      interpX = x
      interpY = y
      interpAngle = angle
    }
    else if (interpTime > currentUpdate) {
      interpX = x
      interpY = y
      interpAngle = angle
    }
    else if (interpTime < previousUpdate) {
      interpX = lastX
      interpY = lastY
      interpAngle = lastAngle
    }
    else if (lastX != -1f && lastY != -1f) {
      val interpFactor = (currentUpdate - interpTime) / (currentUpdate - previousUpdate).toFloat
      interpX = lastX * interpFactor + x * (1-interpFactor)
      interpY = lastY * interpFactor + y * (1-interpFactor)
      interpAngle = lastAngle * interpFactor + angle * (1-interpFactor)
    }
    else {
      interpX = x
      interpY = y
      interpAngle = angle
    }
  }

  def renderBody(g: Graphics, color: Color) {
    import GL._

    translate(interpX, interpY) {
      rotate(0, 0, interpAngle.toDegrees) {
        scale(imageScale, imageScale) {
          image.draw(-image.getWidth/2f, -image.getHeight/2f, color)
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
    lastAngle = angle
    
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
