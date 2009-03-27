package client

import shared._
import org.newdawn.slick._
import scala.collection.mutable.Stack
import RichGraphics._
import GL._

object Menu {
  val defaultPositionX = Config("menu.defaultPositionX").toInt
  val defaultPositionY = Config("menu.defaultPositionY").toInt

  val clickableItemWidth = Config("menu.clickableItemWidth").toInt
  val clickableItemHeight = Config("menu.clickableItemHeight").toInt
  val clickableItemOffsetX = Config("menu.clickableItemOffsetX").toInt
  val clickableItemOffsetY = Config("menu.clickableItemOffsetY").toInt
}

class Menu(initTree: List[(Object, MenuItem)], offsetX: Int, offsetY: Int) {
  val SELECTED_COLOR = new Color(1.0f, 1.0f, 1.0f, 1.0f)
  val UNSELECTED_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.5f)
  
  var showing = true
  var editing = false
  
  val path = new Stack[SubmenuWithPositions]
  var selection = 0

  var tree = buildTree(initTree)

  var scrollAmount = 0
  
  val fontSize = Config("gui.fontSize").toInt

  def this(initTree: List[(Object, MenuItem)]) = this(initTree, Menu.defaultPositionX, Menu.defaultPositionY)
  
  def buildTree(tree: List[(Object, MenuItem)]): List[(Int, Int, Object, MenuItem)] = {
    var (x, y) = (offsetX, offsetY)
    
    tree.map { item =>
      val (key, command) = item
      
      val tuple = (x, y, key, command match {
        case Submenu(subTree) => SubmenuWithPositions(buildTree(subTree))
        case _ => command
      })

      y += 20
      
      tuple
    }
  }
  
  def render(g: Graphics) {
    if (!showing) return

    //Scroll the menu if the selected item is off the screen.
    val (_, selY, _, _) = subTree(selection)
    
    if (selY + scrollAmount + fontSize*2 > Main.windowHeight) {
      scrollAmount -= fontSize
    }
    else if (selY+scrollAmount < 0) {
      scrollAmount += fontSize
    }

    translate(0, scrollAmount) {
      renderMenu(g)
    }
  }
    
  def renderMenu(g: Graphics) {
    for (i <- 0 until subTree.length) {
      val (x, y, key, command) = subTree(i)
      val current = (i == selection)
      
      val baseColor = if (current) SELECTED_COLOR else UNSELECTED_COLOR
      val color = if (command.enabled) baseColor else new Color(baseColor.r, baseColor.g, baseColor.b, baseColor.a * 0.5f)

      translate(x, y) {
        g.setColor(color)

        g.drawString(key.toString, 0, 0, true)
        command.render(g, this, current)
      }
    }
  }
  
  def keyPressed(key : Int, char : Char) {
    if (editing) {
      currentItem.keyPressed(key, char, this)
    } else {
      key match {
        case Input.KEY_UP => {
          var newSelection = selection

          do {
            newSelection = (newSelection-1) % subTree.length
            //Java modulo on a negative number returns a negative number, stupidly.
            if (newSelection < 0) {
              newSelection = subTree.length - 1
            }
          } while (!itemAtIndex(newSelection).enabled && newSelection != selection)

          selection = newSelection
        }

        case Input.KEY_DOWN => {
          var newSelection = selection

          do {
            newSelection = (newSelection+1) % subTree.length
          } while (!itemAtIndex(newSelection).enabled && newSelection != selection)

          selection = newSelection
        }

        case Input.KEY_RETURN => currentItem.perform(this)

        case Input.KEY_ESCAPE => {
          if (path.isEmpty) {
            cancel()
          }
          else {
            val subMenu = path.pop()
            selection = subTree.indexOf(subTree.find((item) => {item._4 == subMenu}).get)
          }
        }

        case _ => 
      }
    }
  }
  
  def mouseMoved(oldx: Int, oldy: Int, newx: Int, newy: Int) {
    itemAt(newx, newy) match {
      case Some((i, _)) => selection = i
      case None =>
    }
  }
  
  def mouseClicked(button: Int, x: Int, y: Int, clickCount: Int) {
    itemAt(x, y) match {
      case Some((i, item)) => {
        selection = i
        currentItem.perform(this)
      }

      case None =>
    }
  }
  
  def itemAt(x: Int, y: Int): Option[(Int, MenuItem)] = {
    for (i <- 0 until subTree.length) {
      val (itemX, itemY, _, item) = subTree(i)
      
      if (x > itemX + Menu.clickableItemOffsetX && x < itemX + Menu.clickableItemOffsetX + Menu.clickableItemWidth &&
          y > itemY + Menu.clickableItemOffsetY && y < itemY + Menu.clickableItemOffsetY + Menu.clickableItemHeight) {
        return Some(i, item)
      }
    }
    
    None
  }

  def itemAtIndex(i: Int) = subTree(i)._4
  
  def show() {
    path.clear()
    selection = 0
    
    showing = true
  }
  
  def hide() {
    showing = false
  }

  def cancel() {
    hide()
  }
  
  def subTree = {
    if (path.isEmpty) {
      tree
    }
    else {
      path.top.tree
    }
  }
  
  def currentItem = itemAtIndex(selection)
}

abstract class MenuItem {
  def perform(menu : Menu)
  def keyPressed(key : Int, char : Char, menu : Menu) = {}
  def render(graphics : Graphics, menu : Menu, current: Boolean) = {}
  def enabled = true
}

case class MenuEditable(initValue : String, maxLength: Int) extends MenuItem {
  var value = initValue
  val offset = 100
  
  override def perform(menu : Menu) = menu.editing = true
  
  override def keyPressed(key : Int, char : Char, menu : Menu) {
    key match {
      case Input.KEY_RETURN => menu.editing = false 
      case Input.KEY_ESCAPE => menu.editing = false 

      case Input.KEY_BACK => {
        if (value.length > 0) {
          value = value.substring(0, value.length-1)
        }
      }

      case _ => {
        if (value.length < maxLength && char >= 32 && char <= 126) {
          value = value + char
        }
      }
    }
  }
  
  override def render(g: Graphics, menu: Menu, current: Boolean) {
    var str = value
    
    if (current && menu.editing) {
      str = str + '_'
    }
    
    g.drawString(str, offset, 0, true)
  }
}

case class MenuToggle(var value: boolean) extends MenuItem {
  val offset = 100
  override def perform(menu: Menu) = {
    value = !value
  }

  override def render(g: Graphics, menu: Menu, current: Boolean) {
    g.drawString(if (value) "yes" else "no", offset, 0, true)
  }
}

object MenuCommand {
  def apply(callback: Unit => Unit, label: String): MenuCommand = apply(callback, label, null)
  def apply(callback: Unit => Unit): MenuCommand = apply(callback, null, null)
}

case class MenuCommand(callback : Unit => Unit, label: String, enabledFn: Unit => Boolean) extends MenuItem {
  val offset = 100

  override def perform(menu : Menu) = {
    menu.hide()
    callback(menu)
  }

  override def enabled = if (null == enabledFn) true else enabledFn()
  
  override def render(g: Graphics, menu: Menu, current: Boolean) {
    if (null != label) {
      g.drawString(label, offset, 0, true)
    }
  }
}

case class Submenu(tree : List[(Object, MenuItem)]) extends MenuItem {
  // This is probably bad.
  override def perform(menu : Menu) {
    throw new RuntimeException("Submenu#perform() should never be called")
  }
}

case class SubmenuWithPositions(tree: List[(Int, Int, Object, MenuItem)]) extends MenuItem {
  override def perform(menu : Menu) {
    menu.path.push(this)
    menu.selection = 0
  }
}
