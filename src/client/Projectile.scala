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
    MISSILE -> nameForClass(classOf[Missile]),
    GIB -> nameForClass(classOf[Gib])
  )

  val sprites = new scala.collection.mutable.HashMap[String, Image]
  val icons   = new scala.collection.mutable.HashMap[String, Image]

  val gibColor = new Color(0.5f, 0.5f, 0.5f)

  def create(client: Client, projectileType: ProjectileTypes.Value, playerID: Byte): Projectile = {
    projectileType match {
      case PROJECTILE          => new Projectile(client, playerID)
      case NUKE                => new Nuke(client, playerID)
      case ROLLER              => new Roller(client, playerID)
      case MIRV                => new Mirv(client, playerID)
      case MIRV_CLUSTER        => new MirvCluster(client, playerID)
      case CORBOMITE           => new Corbomite(client, playerID)
      case MACHINE_GUN         => new MachineGun(client, playerID)
      case DEATHS_HEAD         => new DeathsHead(client, playerID)
      case DEATHS_HEAD_CLUSTER => new DeathsHeadCluster(client, playerID)
      case MISSILE             => new Missile(client, playerID)
      case GIB                 => new Gib(client, playerID)
    }
  }

  def newFromTuple(client: Client, tuple: (Int, Float, Float, Float, Byte, Byte)) = {
    val (id, _, _, _, projectileType, playerID) = tuple
    
    val p = create(client, ProjectileTypes(projectileType), playerID)
    p.id = id
    p.updateFromTuple(tuple)
    
    p
  }

  def deserialise(data: Array[byte]) = Operations.fromByteArray[(Int, Float, Float, Float, Byte, Byte)](data)

  def generateSprites {
    for (name <- classNames.values) {
      val imageIcon = Resource.getImageIcon(imagePathForName(name))
      val awtImage = imageIcon.getImage

      val targetWidth = (imageSizeForName(name) * Main.GAME_SCALE).toInt
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

class Projectile(client: Client, val playerID: Byte) extends GameObject {
  var id: Int = -1
  
  val trailLifetime = Config("projectile.trail.lifetime").toInt
  var trail: List[(Float, Float, Int)] = Nil
  var stationaryTime = 0
  var dead = false

  def trailDead = stationaryTime > trailLifetime
    
  val name       = Projectile.nameForClass(getClass)
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

    if (this.isInstanceOf[Gib]) {
      val image = Tank.image(playerID)
      val transX = (id % 2) * image.getWidth/2f;
      val transY = (id % 2) * image.getHeight/2f;
      translate(interpX, interpY) {
        rotate(0, 0, interpAngle.toDegrees) {
          scale(imageScale, imageScale) {
            image.draw(-image.getWidth/4f, -image.getHeight/4f, image.getWidth/4, image.getHeight/4, //Draw area
                       transX, transY, image.getWidth/2 + transX, image.getHeight/2 + transY, //Source from texture
                       Projectile.gibColor)
          }
        }
      }
    } else {
      val image      = Projectile.imageForName(name)
      translate(interpX, interpY) {
        rotate(0, 0, interpAngle.toDegrees) {
          scale(imageScale, imageScale) {
            texture (image.getTexture.getTextureID) {
              image.draw(-image.getWidth/2f, -image.getHeight/2f, color)
            }
          }
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
        color(1f, 1f, 1f, 0.5f - (t.toFloat / trailLifetime)*0.5f)
        line(x, y, prevX, prevY)
      }
    
      prevX = x
      prevY = y
      t += delta
    }
    g.setAntiAlias(false)
  }
  
  def updateFromTuple(tuple: (Int, Float, Float, Float, Byte, Byte)) {
    val (id, newX, newY, newAngle, projectileType, _) = tuple
    
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

  def playSound(event: String) {
    val sound = getSound(event)

    if (!sound.equals("")) {
      SoundPlayer.play(sound)
    }
  }

  def getSound(event: String): String = {
    val className = Projectile.nameForClass(this.getClass)

    val specificSoundKey = "projectile." + className + ".sound." + event
    val genericSoundKey  = "projectile.Projectile.sound." + event 

    if (Config.getProperty(specificSoundKey) != null) {
      Config(specificSoundKey)
    } else {
      Config(genericSoundKey)
    }
  }
}

class Nuke(client: Client, playerID: Byte) extends Projectile(client, playerID)

class Mirv(client: Client, playerID: Byte) extends Projectile(client, playerID)
class MirvCluster(client: Client, playerID: Byte) extends Projectile(client, playerID)

class DeathsHead(client: Client, playerID: Byte) extends Mirv(client, playerID)
class DeathsHeadCluster(client: Client, playerID: Byte) extends Projectile(client, playerID)

class MachineGun(client: Client, playerID: Byte) extends Projectile(client, playerID)
class Roller(client: Client, playerID: Byte) extends Projectile(client, playerID)
class Missile(client: Client, playerID: Byte) extends MachineGun(client, playerID)

class Corbomite(client: Client, playerID: Byte) extends Projectile(client, playerID)

class Gib(client: Client, playerID: Byte) extends Projectile(client, playerID)
