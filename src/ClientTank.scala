import sbinary.Instances._
import sbinary.Operations

import org.newdawn.slick.geom._
import org.newdawn.slick.particles._
import org.newdawn.slick._

import org.jbox2d.common._

import SwitchableParticleEmitter._
  
object ClientTank {
  implicit def tankToClientTank(tank: Tank) = tank.asInstanceOf[ClientTank]
}

class ClientTank(client: Client) extends Tank(client, 0) {
  val drawShapePoints = shapePoints.foldLeft[List[Float]](List())((list, v) => list ++ List(v.getX(), v.getY())).toArray
  val tankShape = new Polygon(drawShapePoints)
  def wheelColor = color
  
  var jetEmitter: ConfigurableEmitter = _
  var vapourEmitter: ConfigurableEmitter = _
  def particleEmitters = List(jetEmitter, vapourEmitter)
  var emitting = false


  override def create(x: Float) {
    jetEmitter = ParticleIO.loadEmitter("media/particles/jet.xml")
    vapourEmitter = ParticleIO.loadEmitter("media/particles/vapour.xml")
    
    for (e <- particleEmitters) {
      client.particleSystem.addEmitter(e)
      e.setEmitting(false)
    }
    super.create(x)
  }

  override def update(delta: Int) {
    super.update(delta)
    if (jumping && isAlive) {
      startEmitting
      for (e <- particleEmitters) {
        e.setPosition(x, y)
        e.setRotation(body.getAngle.toDegrees)
      }
    }
    else {
      stopEmitting
    }
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

  def render(g: Graphics) {
    if (isDead) {
      return
    }
    
    g.setColor(color)
    
    g.translate(x, y)
    g.rotate(0, 0, angle)
    
    //Tank body
    g.fill(tankShape)
    
    gun.render(g)
    
    drawWheel(g, -WHEEL_OFFSET_X)
    drawWheel(g, WHEEL_OFFSET_X)
    drawBase(g)

  }

  def drawBase(g: Graphics) {
    g.translate(x, y)
    g.rotate(0, 0, body.getAngle.toDegrees)
    g.translate(BASE_OFFSET_X, BASE_OFFSET_Y)
    g.fillRect(-BASE_WIDTH/2, -BASE_HEIGHT/2, BASE_WIDTH, BASE_HEIGHT)
    g.resetTransform
  }

  def drawWheel(g : Graphics, offsetX : Float) {
    g.translate(x, y)
    g.rotate(0, 0, body.getAngle.toDegrees)
    g.translate(offsetX, WHEEL_OFFSET_Y)
    
    g.setColor(wheelColor)
    g.fillOval(-WHEEL_RADIUS, -WHEEL_RADIUS, WHEEL_RADIUS*2, WHEEL_RADIUS*2)
    
    g.resetTransform
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
      Short, Boolean,       //health, jumping
      Byte, Byte,           //gun angle change, gun power change
      Byte, Short, Short,   //selected weapon, selected ammo, jump fuel
      Byte)](data)          //id
    
    val (newX, newY, newAngle, 
        newGunAngle, newGunPower, newGunTimer, 
        newHealth, newJumping,
        newGunAngleChange, newGunPowerChange, 
        newSelectedWeapon, newSelectedAmmo, newFuel,
        newID) = values

    body.setXForm(new Vec2(newX, newY), newAngle.toFloat.toRadians)
    gun.timer = newGunTimer
    health = newHealth
    jumping = newJumping
    gun.selectedWeapon = ProjectileTypes.apply(newSelectedWeapon)
    gun.ammo(gun.selectedWeapon) = newSelectedAmmo
    jumpFuel = newFuel
    
    id = newID
    
    if (null != client.me && id != client.me.id) {
      println("Updating gun things.")
      gun.angle = newGunAngle
      gun.power = newGunPower
      gun.angleChange = newGunAngleChange
      gun.powerChange = newGunPowerChange
    }
  }
}
