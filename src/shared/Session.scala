package shared
import org.newdawn.slick

import scala.collection.mutable.HashMap

abstract class Session(container: slick.GameContainer) {

  var active = false
  var projectiles = new HashMap[Int, Projectile]

  var nextProjectileId = 0

  var supposedRunTime = 0
  var numTankUpdates = 0
  var startTime: Long = 0

  def enter() {
    active = true
  }
  
  def leave() {
    active = false
  }
  
  def update(delta: Int) {
    supposedRunTime += delta
    
    for (p <- projectiles.values) {
      p.update(delta)
    }
  }
  
  
  def byteToArray(c: Byte) = {
    val a = new Array[byte](1)
    a(0) = c.toByte
    a
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
  
  def isActive = active
}
