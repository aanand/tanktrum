package client

import shared.{Config, Main, ProjectileTypes}
import org.newdawn.slick.geom._
import org.newdawn.slick._

object Gun {
  val lowerImages = new scala.collection.mutable.HashMap[Int, Image]
  val upperImages = new scala.collection.mutable.HashMap[Int, Image]

  def lowerImage(playerID: Int) = {
    val id = Colors.cycle(playerID) + 1
    lowerImages.getOrElseUpdate(id, new Image("media/tanks/" + id + "-gun-lower.png"))
  }

  def upperImage(playerID: Int) = {
    val id = Colors.cycle(playerID) + 1
    upperImages.getOrElseUpdate(id, new Image("media/tanks/" + id + "-gun-upper.png"))
  }
}

class Gun(client: Client, playerID: Short) extends shared.Gun(client) {
  val lowerHeight = Config("gun.lower.height").toFloat
  val upperHeight = Config("gun.upper.height").toFloat
  
  val lowerImage = Gun.lowerImage(playerID)
  val upperImage = Gun.upperImage(playerID)

  val lowerScale = lowerHeight / lowerImage.getHeight
  val upperScale = upperHeight / upperImage.getHeight

  val lowerWidth = lowerImage.getWidth * lowerScale
  val upperWidth = upperImage.getWidth * upperScale

  val READY_COLOR   = new Color(0.0f, 1.0f, 0.0f, 0.8f)
  val LOADING_COLOR = new Color(1.0f, 0.0f, 0.0f, 0.8f)

  def render(g: Graphics) {
    import GL._
    
    val powerFraction  = (power-POWER_RANGE.start) / (POWER_RANGE.end - POWER_RANGE.start)
    val lowerExtension = Math.min(1, 2*powerFraction)
    val upperExtension = Math.max(0, 2*powerFraction - 1)

    val lowerOffsetY = lowerHeight * (lowerExtension-1)
    val upperOffsetY = upperHeight * upperExtension

    translate(OFFSET_X, OFFSET_Y) {
      rotate(0, 0, angle) {
        scale(1, -1) {
          texture(upperImage.getTexture.getTextureID) {
            upperImage.draw(-upperWidth/2f, lowerOffsetY + upperOffsetY, upperWidth, upperHeight)
          }

          texture(lowerImage.getTexture.getTextureID) {
            lowerImage.draw(-lowerWidth/2f, lowerOffsetY, lowerWidth, lowerHeight)
          }
        }
      }
    }
  }

  def setTimer(newVal: float) = {
    if (newVal <= 0 && timer > 0) {
      if (selectedWeapon != ProjectileTypes.MACHINE_GUN) {
        SoundPlayer ! PlaySound("reload.wav")
      }
    }
    timer = newVal
  }
}
