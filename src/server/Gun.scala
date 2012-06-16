package server

import shared._

class Gun(server: Server, tank: Tank) extends shared.Gun(server) {
  def x = (tank.x - OFFSET_X * Math.cos(tank.angle.toRadians) - OFFSET_Y * Math.sin(tank.angle.toRadians)).toFloat
  def y = (tank.y - OFFSET_X * Math.sin(tank.angle.toRadians) + OFFSET_Y * Math.cos(tank.angle.toRadians)).toFloat
  
  for (projectileType <- ProjectileTypes) {
    ammo(projectileType) = 0
  }
  ammo(ProjectileTypes.PROJECTILE) = 999
  
  def cycleWeapon() {
    var id = (selectedWeapon.id + 1) % ProjectileTypes.maxId
    
    if (!ammo.values.exists((ammoType) => {ammoType > 0})) {
      println("No ammo left.")
      return
    }

    while(ammo(ProjectileTypes.apply(id)) <= 0) {
      id = (id + 1) % ProjectileTypes.maxId
    }

    selectedWeapon = ProjectileTypes.apply(id)
    println(selectedWeapon)
  }

  override def update(delta: Int) {
    if (firing) fire
    super.update(delta)
    if (!ready) {
      timer -= delta / 1000.0f
    }
  }
  
  def fire {
    if (tank.isDead) return
    
    if (ready && ammo(selectedWeapon) > 0) {
      ammo(selectedWeapon) = ammo(selectedWeapon) - 1
      val proj = server.addProjectile(tank, x, y, tank.angle+angle, power, selectedWeapon)
      timer = proj.reloadTime
    }
    
    if (ammo(selectedWeapon) == 0) {
      cycleWeapon
    }
  }
}
