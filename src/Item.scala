/**
 * Class to keep track of things people can buy.
 */
import scala.collection.mutable.HashMap

abstract class Item {
  def name: String
  def cost: Int
  def units: Int
  val projectileType: ProjectileTypes.Value = null
  var itemType: Items.Value = null;
}

//Can't seem to get a handle on Item.Value from here.
object Items extends Enumeration {
  val items = new HashMap[Value, Item]
  
  NukeItem.itemType = Value
  items.put(NukeItem.itemType, NukeItem)
  
  MIRVItem.itemType = Value
  items.put(MIRVItem.itemType, MIRVItem)
  
  RollerItem.itemType = Value
  items.put(RollerItem.itemType, RollerItem)

  MachineGunItem.itemType = Value
  items.put(MachineGunItem.itemType, MachineGunItem)
}
