/**
 * Class to keep track of things people can buy.
 */
package shared
import scala.collection.mutable.HashMap

abstract class Item {
  def name: String
  def cost: Int
  def units: Int
  val projectileType: ProjectileTypes.Value = null
  var itemType: Items.Value = null
}

object Items extends Enumeration {
  val items = new HashMap[Value, Item]
  
  Nuke.itemType = Value
  items.put(Nuke.itemType, Nuke)
  
  Mirv.itemType = Value
  items.put(Mirv.itemType, Mirv)
  
  Roller.itemType = Value
  items.put(Roller.itemType, Roller)

  MachineGun.itemType = Value
  items.put(MachineGun.itemType, MachineGun)

  DeathsHead.itemType = Value
  items.put(DeathsHead.itemType, DeathsHead)
  
  JumpjetItem.itemType = Value
  items.put(JumpjetItem.itemType, JumpjetItem)
  
  Corbomite.itemType = Value
  items.put(Corbomite.itemType, Corbomite)
  
  Missile.itemType = Value
  items.put(Missile.itemType, Missile)
}
