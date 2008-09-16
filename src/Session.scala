import org.newdawn.slick
import net.phys2d
import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet

abstract class Session(container: slick.GameContainer) extends phys2d.raw.CollisionListener {
  val WIDTH = 800
  val HEIGHT = 600

  var world = createWorld

  var ground: Ground = _

  var active = false
  var bodies = new HashMap[phys2d.raw.Body, Collider]
  var projectiles = new HashMap[Int, Projectile]
  var explosions = new HashSet[Explosion]
  val frags = new HashSet[Frag]

  var nextProjectileId = 0

  def tanks: Iterator[Tank]

  def enter() {
    ground = new Ground(this, WIDTH, HEIGHT)
    active = true
  }
  
  def leave() {
    active = false
  }
  
  def update(delta: Int) {
    ground.update(delta)
    for (tank <- tanks) {
      if (null != tank) { tank.update(delta) }
    }
    for (p <- projectiles.values) {
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

  def createWorld = {
    val newWorld = new phys2d.raw.World(
      new phys2d.math.Vector2f(0.0f, Config("physics.gravity").toFloat),

      Config("physics.iterations").toInt,

      new phys2d.raw.strategies.QuadSpaceStrategy(
        Config("physics.quadSpace.maxInSpace").toInt,
        Config("physics.quadSpace.maxLevels").toInt))

    newWorld.enableRestingBodyDetection(0.01f, 0.01f, 0.01f)
    newWorld.addListener(this)
    newWorld
  }

  def addBody(obj: Collider, body: phys2d.raw.Body) {
    world.add(body)
    bodies.put(body, obj)
  }
  
  def removeBody(body : phys2d.raw.Body) {
    world.remove(body)
    bodies -= body
  }

  def addExplosion(x: Float, y: Float, radius: Float, projectile: Projectile) {
    explosions += new Explosion(x, y, radius, this, projectile)
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
  
  def addProjectile(tank : Tank, x : Double, y : Double, angle : Double, speed : Double, projectileType : ProjectileTypes.Value): Projectile = {
    val radians = Math.toRadians(angle-90)
    val velocity = new phys2d.math.Vector2f((speed * Math.cos(radians)).toFloat, (speed * Math.sin(radians)).toFloat)
    var p: Projectile = ProjectileTypes.newProjectile(this, tank, projectileType)

    p.body.setPosition(x.toFloat, y.toFloat)
    p.body.adjustVelocity(new phys2d.math.Vector2f(tank.velocity))
    p.body.adjustVelocity(velocity)

    addProjectile(p)
  }

  def addProjectile(projectile: Projectile): Projectile = {
    projectile.id = nextProjectileId
    projectiles.put(nextProjectileId, projectile)
    nextProjectileId += 1

    projectile
  }
  
  def removeProjectile(p : Projectile) {
    p.onRemove
    projectiles -= p.id
  }
  
  override def collisionOccured(event : phys2d.raw.CollisionEvent) {
    val a = event.getBodyA()
    val b = event.getBodyB()
    
    bodies(a).collide(bodies(b), event)
    bodies(b).collide(bodies(a), event)
  }

  def isActive = active
}
