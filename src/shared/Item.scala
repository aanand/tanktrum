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
  
  NukeItem.itemType = Value
  items.put(NukeItem.itemType, NukeItem)
  
  MirvItem.itemType = Value
  items.put(MirvItem.itemType, MirvItem)
  
  RollerItem.itemType = Value
  items.put(RollerItem.itemType, RollerItem)

  MachineGunItem.itemType = Value
  items.put(MachineGunItem.itemType, MachineGunItem)

  DeathsHeadItem.itemType = Value
  items.put(DeathsHeadItem.itemType, DeathsHeadItem)
  
  JumpjetItem.itemType = Value
  items.put(JumpjetItem.itemType, JumpjetItem)
  
  CorbomiteItem.itemType = Value
  items.put(CorbomiteItem.itemType, CorbomiteItem)
  
  MissileItem.itemType = Value
  items.put(MissileItem.itemType, MissileItem)
}
