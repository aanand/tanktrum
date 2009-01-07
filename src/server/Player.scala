package server

import shared._

import java.net._
import sbinary.Instances._
import sbinary.Operations

class Player(var tank: Tank, playerName: String, playerId: Byte) extends shared.Player {
  name = playerName
  id = playerId

  val TIMEOUT = Config("player.timeout").toInt
  
  var lastPing = System.currentTimeMillis
  
  def gun = tank.gun

  def resetTimeout() = {
    lastPing = System.currentTimeMillis
  }

  def timedOut() = {
    (System.currentTimeMillis - lastPing) > TIMEOUT
  }

  def buy(item: Item) = {
    if (money >= item.cost) { 
      money -= item.cost
      if (item.projectileType != null) {
        gun.ammo(item.projectileType) = gun.ammo(item.projectileType) + item.units
      }
      if (item == JumpjetItem) {
        tank.purchasedJumpFuel += item.units
        tank.jumpFuel = tank.purchasedJumpFuel
        if (tank.purchasedJumpFuel > tank.maxJumpFuel) {
          tank.purchasedJumpFuel = tank.maxJumpFuel
        }
      }
      if (item == Corbomite) {
        tank.corbomite += item.units
        if (tank.corbomite > tank.maxCorbomite) {
          tank.corbomite = tank.maxCorbomite
        }
      }
    }
  }

  def awardHit(tank: Tank, damage: Int) {
    var pointsAwarded = 0
    if (tank == this.tank) {
      pointsAwarded = -damage
    }
    else {
      pointsAwarded = damage
      
      if (tank.server.leader.tank == tank) {
        pointsAwarded = (1.5 * pointsAwarded).toInt
        println("Extra points for hitting the leader.")
      }
    }

    //Don't allow negative score or money.
    if (score + pointsAwarded > 0) {
      score += pointsAwarded
    }
    if (money + pointsAwarded > 0) {
      money  += pointsAwarded
    }
  }

  def serialise = {
    Operations.toByteArray((
      id,
      me,
      ready,
      score,
      money,
      name
    ))
  }


}
