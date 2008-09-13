import org.newdawn.slick.geom._
import org.newdawn.slick._
import scala.collection.mutable.HashMap

class Gun(session: Session, tank: Tank) {
  val ANGLE_SPEED = 20 // degrees/second
  val POWER_SPEED = 50 // pixels/second/second

  val ANGLE_RANGE = new Range(-90, 90, 1)
  val POWER_RANGE = new Range(50, 300, 1)

  val POWER_SCALE = 200.0f

  val OFFSET_X = 0
  val OFFSET_Y = -(1.5f*tank.HEIGHT)
  
  val READY_COLOR   = new Color(0.0f, 1.0f, 0.0f, 0.5f)
  val LOADING_COLOR = new Color(1.0f, 0.0f, 0.0f, 0.5f)
  
  val arrowShape = new Polygon(List[Float](-5, 0, -5, -50, -10, -50, 0, -60, 10, -50, 5, -50, 5, 0).toArray)
  
  var selectedWeapon = ProjectileTypes.PROJECTILE
  
  var firing = false
  
  var ammo = new HashMap[ProjectileTypes.Value, Int]()
  for (projectileType <- ProjectileTypes) {
    ammo(projectileType) = 0
  }
  ammo(ProjectileTypes.PROJECTILE) = 999
  
  var angleChange = 0
  var powerChange = 0

  var angle = 0f
  var power = 200f
  var timer = 0f

  def ready = timer <= 0
  
  def x = tank.x - OFFSET_X * Math.cos(tank.angle.toRadians) - OFFSET_Y * Math.sin(tank.angle.toRadians)
  def y = tank.y - OFFSET_X * Math.sin(tank.angle.toRadians) + OFFSET_Y * Math.cos(tank.angle.toRadians)

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

  def update(delta: Int): Unit = {
    if (firing) fire
    
    if (angleChange != 0) {
      val newAngle = angle + angleChange * ANGLE_SPEED * delta / 1000.0f
      
      angle = Math.max(ANGLE_RANGE.start, Math.min(ANGLE_RANGE.end, newAngle))
    }
    
    if (powerChange != 0) {
      val newPower = power + powerChange * POWER_SPEED * delta / 1000.0f
      
      power = Math.max(POWER_RANGE.start, Math.min(POWER_RANGE.end, newPower))
    }
    
    if (!ready) {
      timer -= delta / 1000.0f
    }
  }

  def fire {
    if (tank.isDead) return
    
    if (ready && ammo(selectedWeapon) > 0) {
      ammo(selectedWeapon) = ammo(selectedWeapon) - 1
      val proj = session.addProjectile(tank, x, y, tank.angle+angle, power, selectedWeapon)
      timer = proj.reloadTime
    }
    
    if (ammo(selectedWeapon) == 0) {
      cycleWeapon
    }
  }

  def render(g: Graphics) {
    g.translate(OFFSET_X, OFFSET_Y)
    g.rotate(0, 0, angle)
    g.scale(1, power/POWER_SCALE)
    g.setColor(if (ready) READY_COLOR else LOADING_COLOR)
    g.fill(arrowShape)
    
    g.resetTransform
  }
}
