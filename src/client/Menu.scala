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

class Menu(initTree: List[(String, MenuItem)], offsetX: Int, offsetY: Int) {
  val SELECTED_COLOR = new Color(1.0f, 1.0f, 1.0f, 1.0f)
  val UNSELECTED_COLOR = new Color(1.0f, 1.0f, 1.0f, 0.5f)
  
  var showing = true
  var editing = false
  
  val path = new Stack[SubmenuWithPositions]
  var selection = 0

  var tree = buildTree(initTree)

  def this(initTree: List[(String, MenuItem)]) = this(initTree, Menu.defaultPositionX, Menu.defaultPositionY)
  
  def buildTree(tree: List[(String, MenuItem)]): List[(Int, Int, String, MenuItem)] = {
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
    
    for (i <- 0 until subTree.length) {
      val (x, y, key, command) = subTree(i)
      val current = (i == selection)
      
      val color = if (current) SELECTED_COLOR else UNSELECTED_COLOR

      translate(x, y) {
        g.setColor(color)

        g.drawString(key, 0, 0, true)
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
          selection = (selection-1) % subTree.length
          //Java modulo on a negative number returns a negative number, stupidly.
          if (selection < 0) {
            selection = subTree.length - 1
          }
        }

        case Input.KEY_DOWN => selection = (selection+1) % subTree.length 

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
  
  def currentItem = subTree(selection)._4
}

abstract class MenuItem {
  def perform(menu : Menu)
  def keyPressed(key : Int, char : Char, menu : Menu) = {}
  def render(graphics : Graphics, menu : Menu, current: Boolean) = {}
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

case class MenuCommand(callback : Unit => Unit) extends MenuItem {
  override def perform(menu : Menu) = {
    callback(menu)
    menu.hide()
  }
}

case class MenuCommandWithLabel(override val callback : Unit => Unit, label: String) extends MenuCommand(callback) {
  val offset = 100
  
  override def render(g: Graphics, menu: Menu, current: Boolean) {
    g.drawString(label, offset, 0, true)
  }
}

case class Submenu(tree : List[(String, MenuItem)]) extends MenuItem {
  // This is probably bad.
  override def perform(menu : Menu) {
    throw new RuntimeException("Submenu#perform() should never be called")
  }
}

case class SubmenuWithPositions(tree: List[(Int, Int, String, MenuItem)]) extends MenuItem {
  override def perform(menu : Menu) {
    menu.path.push(this)
    menu.selection = 0
  }
}
