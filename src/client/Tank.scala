package client

import shared._

import sbinary.Instances._
import sbinary.Operations

import org.newdawn.slick.geom._
import org.newdawn.slick.particles._
import org.newdawn.slick._

import org.jbox2d.common._

import SwitchableParticleEmitter._

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
  
  lazy val shapePoints = List[Vector2f] (
                      new Vector2f(-(WIDTH/2-TAPER), -HEIGHT),
                      new Vector2f(WIDTH/2-TAPER, -HEIGHT),
                      new Vector2f(WIDTH/2, -BEVEL),
                      new Vector2f(-WIDTH/2, -BEVEL)
                    ).toArray
 
  val drawShapePoints = shapePoints.foldLeft[List[Float]](List())((list, v) => list ++ List(v.getX(), v.getY())).toArray
  val tankShape = new Polygon(drawShapePoints)
  def wheelColor = color
  
  var jetEmitter: ConfigurableEmitter = _
  var vapourEmitter: ConfigurableEmitter = _
  def particleEmitters = List(jetEmitter, vapourEmitter)
  var emitting = false

  var wasAlive = false
  
  val gun = new Gun(client)

  def color = Colors(id)
  
  var health = 100f
  def isAlive = health > 0
  def isDead = !isAlive
  
  var jumping = false
  
  var maxJumpFuel = Config("tank.jumpjet.maxFuel").toInt
  var jumpFuel = 0f
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

    if (jumping && isAlive) {
      startEmitting
      for (e <- particleEmitters) {
        e.setPosition(x, y)
        e.setRotation(angle.toDegrees)
      }
    }
    else {
      stopEmitting
    }
    
    if (wasAlive && !isAlive) {
      SoundPlayer ! PlaySound("explosion.ogg")
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
    
    g.setColor(color)
    
    translate(x, y) {
      rotate(0, 0, angle.toDegrees) {
    
        //Tank body
        g.fill(tankShape)

        gun.render(g)

        drawWheel(g, -WHEEL_OFFSET_X)
        drawWheel(g, WHEEL_OFFSET_X)
        drawBase(g)

        g.setAntiAlias(true)
        g.setColor(new Color(0f, 0f, 0f, 0.5f))
        g.setLineWidth(1.3f)
        GL.line(-(WIDTH/2-TAPER), -HEIGHT, WIDTH/2-TAPER, -HEIGHT)
        GL.line(-WIDTH/2, 0f, WIDTH/2, 0f)
        GL.line(-(WIDTH/2-TAPER), -HEIGHT, -WIDTH/2, 0f)
        GL.line(WIDTH/2-TAPER, -HEIGHT, WIDTH/2, 0f)
        g.setAntiAlias(false)
      }
    }
    
    g.resetTransform
    g.scale(Main.GAME_WINDOW_RATIO, Main.GAME_WINDOW_RATIO)
    
  }

  def drawBase(g: Graphics) {
    translate(BASE_OFFSET_X, BASE_OFFSET_Y) {
      g.fillRect(-BASE_WIDTH/2, -BASE_HEIGHT/2, BASE_WIDTH, BASE_HEIGHT)
    }
  }

  def drawWheel(g : Graphics, offsetX : Float) {
    translate(offsetX, WHEEL_OFFSET_Y) {
      g.setColor(wheelColor)
      g.fillOval(-WHEEL_RADIUS, -WHEEL_RADIUS, WHEEL_RADIUS*2, WHEEL_RADIUS*2)
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
    println("Set angle to: " + angle)
    gun.timer = newGunTimer
    health = newHealth
    jumping = newJumping
    gun.selectedWeapon = ProjectileTypes.apply(newSelectedWeapon)
    gun.ammo(gun.selectedWeapon) = newSelectedAmmo
    jumpFuel = newFuel
    
    id = newID
    
    if (null != client.me && id != client.me.id) {
      gun.angle = newGunAngle
      gun.power = newGunPower
      gun.angleChange = newGunAngleChange
      gun.powerChange = newGunPowerChange
    }
  }
}
