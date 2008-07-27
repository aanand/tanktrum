import org.newdawn.slick
import net.phys2d
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

abstract class Session(container: slick.GameContainer) extends phys2d.raw.CollisionListener {
  val BROADCAST_INTERVAL = 0.015
  val WIDTH = 800
  val HEIGHT = 600

  var timeToUpdate = BROADCAST_INTERVAL

  val world = new phys2d.raw.World(new phys2d.math.Vector2f(0.0f, 100.0f), 10)
  world.enableRestingBodyDetection(0.1f, 0.1f, 0.1f)
  world.addListener(this)

  var ground: Ground = _

  var active = false
  var tanks = List[Tank]()
  val bodies = new HashMap[phys2d.raw.Body, Collider]
  val projectiles = new HashSet[Projectile]
  val explosions = new HashSet[Explosion]
  val frags = new HashSet[Frag]

  def enter() {
    ground = new Ground(this, WIDTH, HEIGHT)
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
      p.render(g)
    }
    for (e <- explosions) {
      e.render(g)
    }
    for (f <- frags) {
      f.render(g)
    }
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
    
    timeToUpdate = timeToUpdate - delta/1000.0

    if (timeToUpdate < 0) {
      broadcastUpdate()
      timeToUpdate = BROADCAST_INTERVAL
    }
  }
  
  def broadcastUpdate()
  
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
  
  def addProjectile(tank : Tank, x : Double, y : Double, angle : Double, speed : Double) = {
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
    
    println("added projectile at " + p.x + ", " + p.y + ", " + angle + ", " + speed)
    p
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

  def isActive = active
}
