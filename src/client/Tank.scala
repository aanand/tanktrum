package client

import shared._

import sbinary.Instances._
import sbinary.Operations

import org.newdawn.slick.geom._
import org.newdawn.slick.particles._
import org.newdawn.slick._

import SwitchableParticleEmitter._

object Tank {
  val images = new scala.collection.mutable.HashMap[Int, Image]

  def image(playerID: Int) = {
    val id = Colors.cycle(playerID) + 1
    images.getOrElseUpdate(id, new Image("media/tanks/" + id + ".png"))
  }
}

class Tank(client: Client) extends GameObject {
  var id: Short = _
  
  lazy val WIDTH  = Config("tank.width").toFloat
  lazy val HEIGHT = Config("tank.height").toFloat
  lazy val TAPER  = Config("tank.taper").toFloat
  lazy val BEVEL  = Config("tank.bevel").toFloat
  
  lazy val WHEEL_RADIUS = BEVEL
  lazy val WHEEL_OFFSET_X = WIDTH/2-BEVEL
  lazy val WHEEL_OFFSET_Y = -BEVEL

  lazy val BASE_WIDTH = WIDTH - 2*WHEEL_RADIUS
  lazy val BASE_HEIGHT = BEVEL
  lazy val BASE_OFFSET_X = 0
  lazy val BASE_OFFSET_Y = -BASE_HEIGHT/2

  var player: Player = _
  
  lazy val shapePoints = List[(Float, Float)] ( (-(WIDTH/2-TAPER), -HEIGHT),
                                                (WIDTH/2-TAPER, -HEIGHT),
                                                (WIDTH/2, 0f),
                                                (-WIDTH/2, 0f),
                                                (-(WIDTH/2-TAPER), -HEIGHT)
                                              ).toArray
 
  var jetEmitter: ConfigurableEmitter = _
  var vapourEmitter: ConfigurableEmitter = _
  def particleEmitters = List(jetEmitter, vapourEmitter)
  var emitting = false

  var wasAlive = false
  
  val gun = new Gun(client, id)

  def image = Tank.image(id)
  
  var health = 100f
  def isAlive = health > 0
  def isDead = !isAlive
  
  var jumping = false
  
  var maxJumpFuel = Config("tank.jumpjet.maxFuel").toInt
  var jumpFuel = 0f
  
  val maxJetSoundTimer = Config("tank.jumpjet.soundRepeat").toInt
  var jetSoundTimer = 0

  def fuelPercent = (jumpFuel.toFloat/maxJumpFuel) * 100

  def create(x: Float) {
    jetEmitter = ParticleIO.loadEmitter("media/particles/jet.xml")
    vapourEmitter = ParticleIO.loadEmitter("media/particles/vapour.xml")
    
    for (e <- particleEmitters) {
      client.particleSystem.addEmitter(e)
      e.setEmitting(false)
    }
  }

  def update(delta: Int) {
    gun.update(delta)
    
    if (jetSoundTimer > 0) jetSoundTimer -= delta

    if (jumping && isAlive) {
      startEmitting

      if (jetSoundTimer <= 0) {
        jetSoundTimer = maxJetSoundTimer
        SoundPlayer ! PlaySound(Config("tank.sound.jet"))
      }

      for (e <- particleEmitters) {
        e.setPosition(x, y)
        e.setRotation(angle.toDegrees)
      }
    }
    else {
      stopEmitting
    }
    
    if (wasAlive && !isAlive) {
      val sound = Config("tank.sound.death")
      SoundPlayer ! PlaySound(sound)
    }
    
    wasAlive = isAlive
  }

  def startEmitting {
    if (!emitting) {
      for (e <- particleEmitters) {
        e.setEmitting(true)
      }
      
      emitting = true
    }
  }

  def stopEmitting {
    if (emitting) {
      for (e <- particleEmitters) {
        e.setEmitting(false)
      }
      
      emitting = false
    }
  }

  import GL._

  def render(g: Graphics) {
    if (isDead) {
      return
    }
    
    translate(x, y) {
      rotate(0, 0, angle.toDegrees) {
        gun.render(g)
        
        //Tank body
        texture (image.getTexture.getTextureID) {
          image.draw(-WIDTH/2f, -HEIGHT, WIDTH, HEIGHT)
        }
      }
    }
  }

  def serialise = {
    Operations.toByteArray((
      gun.angle.toShort,
      gun.angleChange.toByte,
      gun.power.toShort,
      gun.powerChange.toByte
    ))
  }
  
  def loadFrom(data: Array[Byte]) = {
    val values = Operations.fromByteArray[(
      Float, Float, Short,  //x, y, angle
      Short, Short, Short,  //gun angle, gun power, gun timer
      Float, Boolean,       //health, jumping
      Byte, Byte,           //gun angle change, gun power change
      Byte, Short, Short,   //selected weapon, selected ammo, jump fuel
      Byte)](data)          //id
    
    val (newX, newY, newAngle, 
        newGunAngle, newGunPower, newGunTimer, 
        newHealth, newJumping,
        newGunAngleChange, newGunPowerChange, 
        newSelectedWeapon, newSelectedAmmo, newFuel,
        newID) = values

    x = newX
    y = newY
    angle = newAngle.toFloat.toRadians
    gun.setTimer(newGunTimer)
    health = newHealth
    jumping = newJumping
    gun.selectedWeapon = ProjectileTypes(newSelectedWeapon)
    gun.ammo(gun.selectedWeapon) = newSelectedAmmo
    jumpFuel = newFuel
    
    id = newID
    gun.playerID = newID
    
    if (null != client.me && id != client.me.id) {
      gun.angle = newGunAngle
      gun.power = newGunPower
      gun.angleChange = newGunAngleChange
      gun.powerChange = newGunPowerChange
    }
  }
}
