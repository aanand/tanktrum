import org.newdawn.slick
import net.phys2d
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

abstract class Session(container: slick.GameContainer) extends phys2d.raw.CollisionListener {
  val WIDTH = 800
  val HEIGHT = 600

  val world = new phys2d.raw.World(new phys2d.math.Vector2f(0.0f, 100.0f), 10)
  world.enableRestingBodyDetection(0.001f, 0.001f, 0.001f)
  world.addListener(this)

  var ground: Ground = _

  var active = false
  var tanks = List[Tank]()
  val bodies = new HashMap[phys2d.raw.Body, Collider]
  var projectiles = List[Projectile]()
  val explosions = new HashSet[Explosion]
  val frags = new HashSet[Frag]

  def enter() {
    ground = new Ground(this, WIDTH, HEIGHT)
    active = true
  }
  
  def leave() {
    active = false
  }
  
  def update(delta: Int) {
    world.step(delta/1000f)
    
    ground.update(delta)
    for (tank <- tanks) {
      tank.update(delta)
    }
    for (p <- projectiles) {
      p.update(delta)
    }
    for (e <- explosions) {
      e.update(delta)
    }
    
  }
  
  def byteToArray(c: Byte) = {
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

  def addExplosion(x: Float, y: Float, radius: Float) {
    explosions += new Explosion(x, y, radius, this)
  }

  def removeExplosion(e: Explosion) {
    explosions -= e
  }

  def addFrag(f: Frag) {
    addBody(f, f.body)
    frags += f
  }

  def removeFrag(f: Frag) {
    removeBody(f.body)
    frags -= f
  }
  
  def addProjectile(tank : Tank, x : Double, y : Double, angle : Double, speed : Double, projectileType : ProjectileTypes.Value) = {
    val radians = Math.toRadians(angle-90)
    val velocity = new phys2d.math.Vector2f((speed * Math.cos(radians)).toFloat, (speed * Math.sin(radians)).toFloat)
    var p: Projectile = null

    projectileType match {
      case ProjectileTypes.PROJECTILE => { p = new Projectile(this, tank) }
      case ProjectileTypes.NUKE => { p = new Nuke(this, tank) }
      case ProjectileTypes.ROLLER => { p = new Roller(this, tank) }
    }

    p.body.setPosition(x.toFloat, y.toFloat)
    p.body.adjustVelocity(velocity)
    projectiles += p
    
    println("added projectile at " + p.x + ", " + p.y + ", " + angle + ", " + speed)
    p
  }

  def addProjectile(projectile: Projectile) {
    projectiles += projectile
  }
  
  def removeProjectile(p : Projectile) {
    removeBody(p.body)
    
    projectiles -= p
  }
  
  override def collisionOccured(event : phys2d.raw.CollisionEvent) {
    val a = event.getBodyA()
    val b = event.getBodyB()
    
    bodies(a).collide(bodies(b), event)
    bodies(b).collide(bodies(a), event)
  }

  def isActive = active
}
