import org.newdawn.slick._
import scala.collection.mutable.Stack

class Menu(tree : List[(String, MenuItem)]) {
  val SELECTED_COLOR = new Color(1.0f, 1.0f, 1.0f, 1.0f)
  val UNSELECTED_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.5f)
  
  var showing = true
  var editing = false
  
  val path = new Stack[Submenu]
  var selection = 0

  def render(container : GameContainer, g : Graphics) {
    if (!showing) return;
    
    val font = container.getDefaultFont()
    
    g.translate(20, 20)
    
    for (i <- 0 until subTree.length) {
      val (key, command) = subTree(i)
      val current = (i == selection)
      
      val color = if (current) SELECTED_COLOR else UNSELECTED_COLOR

      font.drawString(0, 0, key, color)
      command.render(container, g, this, current)
      
      g.translate(0, 20)
    }
    
    g.resetTransform()
  }
  
  def keyPressed(key : Int, char : Char) {
    if (editing) {
      currentItem.keyPressed(key, char, this)
    } else {
      key match {
        case Input.KEY_UP   => { selection = (selection-1) % subTree.length }
        case Input.KEY_DOWN => { selection = (selection+1) % subTree.length }
        case Input.KEY_RETURN => {
          currentItem.perform(this)
        }
        case Input.KEY_ESCAPE => {
          if (path.isEmpty) {
            hide()
          }
          else {
            val subMenu = path.pop()
            selection = subTree.indexOf(subTree.find((item) => {item._2 == subMenu}).get)
          }
        }
        case _ => {}
      }
    }
  }
  
  def show() {
    path.clear()
    selection = 0
    
    showing = true
  }
  
  def hide() {
    showing = false
  }
  
  def subTree = {
    if (path.isEmpty) {
      tree
    }
    else {
      path.top.tree
    }
  }
  
  def currentItem = {
    val (key, item) = subTree(selection)
    item
  }
}

abstract class MenuItem {
  def perform(menu : Menu)
  def keyPressed(key : Int, char : Char, menu : Menu) = {}
  def render(container : GameContainer, graphics : Graphics, menu : Menu, current: Boolean) = {}
}

case class MenuEditable(initValue : String) extends MenuItem {
  var value = initValue
  
  override def perform(menu : Menu) = menu.editing = true
  
  override def keyPressed(key : Int, char : Char, menu : Menu) {
    key match {
      case Input.KEY_RETURN => { menu.editing = false }
      case Input.KEY_ESCAPE => { menu.editing = false }
      case Input.KEY_BACK => {
        if (value.length > 0) {
          value = value.substring(0, value.length-1)
        }
      }
      case _ => {
        value = value + char
      }
    }
  }
  
  override def render(container : GameContainer, g : Graphics, menu : Menu, current: Boolean) {
    var str = value
    
    if (current && menu.editing) {
      str = str + '_'
    }
    
    container.getDefaultFont.drawString(50, 0, str)
  }
}

case class MenuCommand(callback : Unit => Unit) extends MenuItem {
  override def perform(menu : Menu) = {
    callback(menu)
    menu.hide()
  }
}

case class Submenu(tree : List[(String, MenuItem)]) extends MenuItem {
  override def perform(menu : Menu) {
    menu.path.push(this)
    menu.selection = 0
  }
}
