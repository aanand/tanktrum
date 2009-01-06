
import sbinary.Instances._
import sbinary.Operations

package shared {
  object ProjectileTypes extends Enumeration {
    val PROJECTILE, NUKE, ROLLER, MIRV, MIRV_CLUSTER, CORBOMITE, 
        MACHINE_GUN, DEATHS_HEAD, DEATHS_HEAD_CLUSTER, MISSILE = Value
    
  }

  trait Projectile {
    var id: Int = -1

    def imagePath = Config("projectile.imagePath")
    def imageWidth = Config("projectile.imageWidth").toInt
    def round = Config("projectile.round").toBoolean

    val explosionRadius = 4f
    val explosionDamageFactor = 1f
    lazy val radius = 0.6f
    val damage = 5
    val reloadTime = 4f

    //body.addExcludedBody(session.ground.body)

    val projectileType = ProjectileTypes.PROJECTILE

    var destroy = false
  }
}

package server {
  import shared._
  import ProjectileTypes._
  object Projectile {
    val antiGravity = Config("physics.projectileGravity").toFloat - Config("physics.gravity").toFloat
    
    def create(server: Server, tank: Tank, projectileType: ProjectileTypes.Value): Projectile = {
      projectileType match {
        case PROJECTILE          => new Projectile(server, tank) 
        case NUKE                => new Nuke(server, tank) 
        case ROLLER              => new Roller(server, tank) 
        case MIRV                => new Mirv(server, tank) 
        case MIRV_CLUSTER        => new MirvCluster(server, tank)
        case CORBOMITE           => new Corbomite(server, tank)
        case MACHINE_GUN         => new MachineGun(server, tank) 
        case DEATHS_HEAD         => new DeathsHead(server, tank)
        case DEATHS_HEAD_CLUSTER => new DeathsHeadCluster(server, tank)
        case MISSILE             => new Missile(server, tank)
      }
    }
  }
}

package client {
  import shared._
  import ProjectileTypes._
  import GL._
  import org.newdawn.slick

  object Projectile {
    def create(projectileType: ProjectileTypes.Value): Projectile = {
      projectileType match {
        case PROJECTILE          => new Projectile
        case NUKE                => new Nuke
        case ROLLER              => new Roller
        case MIRV                => new Mirv
        case MIRV_CLUSTER        => new MirvCluster
        case CORBOMITE           => new Corbomite
        case MACHINE_GUN         => new MachineGun
        case DEATHS_HEAD         => new DeathsHead
        case DEATHS_HEAD_CLUSTER => new DeathsHeadCluster
        case MISSILE             => new Missile
      }
    }

    def newFromTuple(client: Client, tuple: (Int, Float, Float, Float, Float, Float, Float, Byte)) = {
      val (id, _, _, _, _, _, _, projectileType) = tuple
      
      val p = create(ProjectileTypes(projectileType))
      p.id = id
      p.updateFromTuple(tuple)
      
      p
    }
    def deserialise(data: Array[byte]) = Operations.fromByteArray[(Int, Float, Float, Float, Float, Float, Float, Byte)](data)

    def render(g: slick.Graphics, value: Value) {
      g.setColor(new slick.Color(1f, 1f, 1f))
      value match {
        case PROJECTILE => {
          g.fillOval(-3, -3, 6, 6)
        }

        case NUKE => {
          g.fillOval(-6, -6, 12, 12)
        }

        case ROLLER => {
          g.fillOval(-3, -3, 6, 6)
          g.fillRect(-7, 3, 14, 4)
        }

        case MIRV => {
          g.fillOval(-4, -4, 4, 4)
          g.fillOval(0, 0, 4, 4)
          g.fillOval(-4, 0, 4, 4)
          g.fillOval(0, -4, 4, 4)
        }

        case MACHINE_GUN => {
          g.fillRect(-2, -4, 4, 8)
        }

        case DEATHS_HEAD =>  {
          g.fillOval(-8, -8, 8, 8)
          g.fillOval(0, 0, 8, 8)
          g.fillOval(-8, 0, 8, 8)
          g.fillOval(0, -8, 8, 8)
        }

        case MISSILE => {
          g.fillRect(-3, -6, 6, 12)
          g.fillOval(-3, -9, 6, 6)
        }
      }
    }
  }
}
