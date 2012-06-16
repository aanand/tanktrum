package shared
import scala.collection.mutable.HashMap

class Gun(session: Session) {
  val ANGLE_SPEED = Config("gun.angleSpeed").toFloat
  val POWER_SPEED = Config("gun.powerSpeed").toFloat

  val ANGLE_RANGE = new Range(Config("gun.angleMin").toInt, Config("gun.angleMax").toInt, 1)
  val POWER_RANGE = new Range(Config("gun.powerMin").toInt, Config("gun.powerMax").toInt, 1)

  val OFFSET_X = 0f
  val OFFSET_Y = Config("gun.offsetY").toFloat * Config("tank.height").toFloat
  
  var selectedWeapon = ProjectileTypes.PROJECTILE
  
  var firing = false
  
  var ammo = new HashMap[ProjectileTypes.Value, Int]()
  var angleChange = 0
  var powerChange = 0

  var angle = 0f
  var power = POWER_RANGE.end/2f
  var timer = 0f

  def ready = timer <= 0
  
  def update(delta: Int) {
    
    if (angleChange != 0) {
      val newAngle = angle + angleChange * ANGLE_SPEED * delta / 1000.0f
      
      angle = Math.max(ANGLE_RANGE.start, Math.min(ANGLE_RANGE.end, newAngle))
    }
    
    if (powerChange != 0) {
      val newPower = power + powerChange * POWER_SPEED * delta / 1000.0f
      
      power = Math.max(POWER_RANGE.start, Math.min(POWER_RANGE.end, newPower))
    }
  }

  def reset {
    angle = 0f
    power = POWER_RANGE.end/2f
  }
}
