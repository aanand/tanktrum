import org.newdawn.slick
import net.phys2d
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

abstract class Session(container: slick.GameContainer) extends phys2d.raw.CollisionListener {
  val world = new phys2d.raw.World(new phys2d.math.Vector2f(0.0f, 100.0f), 10)
  world.addListener(this)

  var ground: Ground = _
  var me : Player = _

  var active = false
  var tanks = List[Tank]()
  val bodies = new HashMap[phys2d.raw.Body, Collider]
  val projectiles = new HashSet[Projectile]

  def enter() {
    ground = new Ground(this, container.getWidth(), container.getHeight())
    active = true
  }
  
  def leave() {
    active = false
  }
  
  def render(g: slick.Graphics) {
    if (ground.initialised) {
      ground.render(g)
    }
    for (tank <- tanks) {
      tank.render(g)
    }
    for (p <- projectiles) {
      p.render(container, g)
    }
  }
  
  def update(delta: Int) {
    world.step(delta/1000f)
    
    for (tank <- tanks) {
      tank.update(delta)
    }
    for (p <- projectiles) {
      p.update(container, delta)
    }
  }
  
  def keyPressed(key : Int, char : Char) {
    if (me == null) return
    
    char match {
      case 'a' => { me.tank.thrust = -1 }
      case 'd' => { me.tank.thrust = 1 }
      case _ => {
        key match {
          case slick.Input.KEY_LEFT  => { me.tank.gunAngleChange = -1 }
          case slick.Input.KEY_RIGHT => { me.tank.gunAngleChange = 1 }
          case slick.Input.KEY_UP    => { me.tank.gunPowerChange = 1 }
          case slick.Input.KEY_DOWN  => { me.tank.gunPowerChange = -1 }
          case slick.Input.KEY_SPACE => { me.tank.fire() }
          case _ => {}
        }
      }
    }
  }
  
  def keyReleased(key : Int, char : Char) {
    if (me == null) return
    
    char match {
      case 'a' => { me.tank.thrust = 0 }
      case 'd' => { me.tank.thrust = 0 }
      case _ => {
        key match {
          case slick.Input.KEY_LEFT  => { me.tank.gunAngleChange = 0 }
          case slick.Input.KEY_RIGHT => { me.tank.gunAngleChange = 0 }
          case slick.Input.KEY_UP    => { me.tank.gunPowerChange = 0 }
          case slick.Input.KEY_DOWN  => { me.tank.gunPowerChange = 0 }
          case _ => {}
        }
      }
    }
  }

  def charToByteArray(c: Char) = {
    val a = new Array[byte](1)
    a(0) = c.toByte
    a
  }

  def addBody(obj: Collider, body: phys2d.raw.Body) {
    world.add(body)
    bodies.put(body, obj)
  }
  
  def removeBody(body : phys2d.raw.Body) {
    world.remove(body)
    bodies -= body
  }
  
  def addProjectile(tank : Tank, x : Double, y : Double, angle : Double, speed : Double, broadcast : Boolean) {
    val radians = Math.toRadians(angle-90)
    val velocity = new phys2d.math.Vector2f((speed * Math.cos(radians)).toFloat, (speed * Math.sin(radians)).toFloat)
    
    val radius = 3f
    
    val shape = new phys2d.raw.shapes.Circle(radius)

    val body = new phys2d.raw.Body(shape, 1.0f)
    body.setPosition(x.toFloat, y.toFloat)
    body.adjustVelocity(velocity)
    
    val p = new Projectile(this, tank, body, radius)

    addBody(p, body)
    
    projectiles += p
    
    if (broadcast) {
      // self.broadcast({:type => :projectile, :player_id => tank.player_id, :x => x, :y => y, :angle => angle, :speed => speed})
    }
    
    println("added projectile at " + p.x + ", " + p.y + ", " + angle + ", " + speed)
  }
  
  def removeProjectile(p : Projectile) {
    removeBody(p.body)
    
    projectiles -= p
    
    println("removed projectile at " + p.x + ", " + p.y)
  }
  
  override def collisionOccured(event : phys2d.raw.CollisionEvent) {
    val a = event.getBodyA()
    val b = event.getBodyB()
    
    bodies(a).collide(bodies(b), event)
    bodies(b).collide(bodies(a), event)
  }

  def getGround = ground

  def isActive = active
}
