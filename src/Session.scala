import org.newdawn.slick

import org.jbox2d.dynamics._
import org.jbox2d.dynamics.contacts._
import org.jbox2d.common._
import org.jbox2d.collision._

import scala.collection.mutable.HashMap
import scala.collection.mutable.HashSet
import java.util.Date

abstract class Session(container: slick.GameContainer) extends ContactListener {
  val TANK_BROADCAST_INTERVAL       = Config("server.tankBroadcastInterval").toInt
  val PROJECTILE_BROADCAST_INTERVAL = Config("server.projectileBroadcastInterval").toInt
  val PLAYER_BROADCAST_INTERVAL     = Config("server.playerBroadcastInterval").toInt
  val READY_ROOM_BROADCAST_INTERVAL = Config("server.readyRoomBroadcastInterval").toInt
  val MAX_PLAYERS                   = Config("server.maxPlayers").toInt
  
  var world = createWorld

  var ground: Ground = _

  var active = false
  var bodies = new HashMap[Body, GameObject]
  var projectiles = new HashMap[Int, Projectile]
  var explosions = new HashSet[Explosion]

  var nextProjectileId = 0

  def tanks: Iterator[Tank]

  var supposedRunTime = 0
  var numTankUpdates = 0
  var startTime: Long = 0

  def enter() {
    ground = new Ground(this, Main.GAME_WIDTH.toInt, Main.GAME_HEIGHT.toInt)
    active = true
  }
  
  def leave() {
    active = false
  }
  
  def update(delta: Int) {
    supposedRunTime += delta
    
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
    val gravity = new Vec2(0.0f, Config("physics.gravity").toFloat)
    val bounds = new AABB(new Vec2(-Main.GAME_WIDTH, -Main.GAME_HEIGHT),
                          new Vec2(2*Main.GAME_WIDTH, 2*Main.GAME_HEIGHT))

    val newWorld = new World(bounds, gravity, false)

    newWorld.setContactListener(this)
    newWorld
  }

  def createBody(obj: GameObject, bodyDef: BodyDef) = {
    val body = world.createBody(bodyDef)
    if (null != body) {
      bodies.put(body, obj)
      body.setUserData(obj)
    }
    body
  }

  def removeBody(body: Body) {
    world.destroyBody(body)
    bodies -= body
  }

  def addExplosion(x: Float, y: Float, radius: Float, projectile: Projectile, damageFactor: Float) {
    explosions += new Explosion(x, y, radius, this, projectile, damageFactor)
  }

  def removeExplosion(e: Explosion) {
    removeBody(e.body)
    explosions -= e
  }

  def addProjectile(tank: Tank, x: Float, y: Float, angle: Float, speed: Float, projectileType : ProjectileTypes.Value): Projectile = {
    val radians = Math.toRadians(angle-90)
    
    val velocity = new Vec2((speed * Math.cos(radians)).toFloat, (speed * Math.sin(radians)).toFloat)
    velocity.addLocal(tank.velocity)

    val position = new Vec2(x.toFloat, y.toFloat)
    
    var p: Projectile = ProjectileTypes.newProjectile(this, tank, projectileType)

    p.body.setXForm(position, 0f)
    p.body.setLinearVelocity(tank.velocity.add(velocity))

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
  
  /**
   * Contact listener callbacks:
   */
  override def add(contact: ContactPoint) {
    val a = contact.shape1.getBody()
    val b = contact.shape2.getBody()
    
    bodies(a).collide(bodies(b), contact)
    bodies(b).collide(bodies(a), contact)
  }

  override def persist(contact: ContactPoint) {
    val a = contact.shape1.getBody()
    val b = contact.shape2.getBody()
    
    bodies(a).persist(bodies(b), contact)
    bodies(b).persist(bodies(a), contact)
  }
  
  override def remove(contact: ContactPoint) {
  }

  override def result(result: ContactResult) {
  }

  def isActive = active
  
  def endRound() {
    val runTime = (new Date().getTime - startTime).toFloat
    
    val prefix = if (this.isInstanceOf[Server]) "Server: " else "Client: "
    
    println(prefix + "runTime = " + runTime/1000)
    println(prefix + "numTankUpdates = " + numTankUpdates)
    
    if (numTankUpdates > 0) {
      val targetTankUpdateRate = 1000f / TANK_BROADCAST_INTERVAL
      val actualTankUpdateRate = numTankUpdates.toFloat/runTime * 1000
      val error = (actualTankUpdateRate - targetTankUpdateRate) / targetTankUpdateRate * 100
      
      println(prefix + "avg tank update interval = " + runTime/numTankUpdates)
      println(prefix + "target tank update = " + targetTankUpdateRate + " updates/sec")
      println(prefix + "actual tank update rate = " + actualTankUpdateRate + " updates/sec")
      println(prefix + "update rate error = " + error + "%")
    }
    
    val error = (supposedRunTime - runTime).toFloat / runTime * 100
    
    println(prefix + "supposedRunTime = " + supposedRunTime.toFloat/1000)
    println(prefix + "delta error = " + error + "%")
  }
}
