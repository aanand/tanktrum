import org.newdawn.slick
import org.newdawn.slick._
import java.nio.channels._
import java.nio._
import java.net._
import java.util.Date

import sbinary.Operations
import sbinary.Instances._

class Client (hostname: String, port: Int, name: String, container: GameContainer) extends Session(container) {
  val PING_PERIOD = 1000
  val SERVER_TIMEOUT = 10000

  var channel: DatagramChannel = _
  val data = ByteBuffer.allocate(10000)
  var lastPing = new Date()
  var lastPong = new Date()

  val skyTopColor    = new Color(0f, 0f, 0.25f)
  val skyBottomColor = new Color(0.3f, 0.125f, 0.125f)

  override def enter() = {
    super.enter()
    channel = DatagramChannel.open()
    channel.connect(new InetSocketAddress(hostname, port))
    channel.configureBlocking(false)

    sendHello
  }

  override def update(delta: Int) {
    super.update(delta)
    ping
    checkTimeout

    data.rewind
    if (channel.receive(data) != null) {
      data.rewind
      val command = data.get.toChar
      processCommand(command)
    }
  }
  
  def render(g: Graphics) {
    if (ground.initialised) {
      renderSky(g)
      ground.render(g)
    }
    for (tank <- tanks) {
      tank.render(g)
    }
    for (p <- projectiles) {
      p.render(g)
    }
    for (e <- explosions) {
      e.render(g)
    }
    for (f <- frags) {
      f.render(g)
    }
    for (i <- 0 until tanks.length) {
      renderHUD(g, i, tanks(i))
    }
  }
  
  def renderSky(g : Graphics) {
    import slick.opengl.renderer.SGL
    
    new GL {
      def draw(gl : SGL) {
        import gl._
        
        shape(SGL.GL_QUADS) {
          glColor4f(skyTopColor.r, skyTopColor.g, skyTopColor.b, 1f)
          glVertex2f(0, 0)
          glVertex2f(container.getWidth, 0)
        
          glColor4f(skyBottomColor.r, skyBottomColor.g, skyBottomColor.b, 1f)
          glVertex2f(container.getWidth, container.getHeight)
          glVertex2f(0, container.getHeight)
        }
      }
    }
  }
 
  def renderHUD(g : Graphics, index : Int, tank : Tank) {
    g.translate(10 + index*110, 10)
    g.setColor(tank.color)
    g.fillRect(0, 0, tank.health, 10)
    
    g.translate(10, 30)
    
    tank.selectedWeapon match {
      case ProjectileTypes.PROJECTILE => {
        g.fillOval(-3, -3, 6, 6)
      }
      case ProjectileTypes.NUKE => {
        g.fillOval(-6, -6, 12, 12)
      }
      case ProjectileTypes.ROLLER => {
        g.setColor(new Color(0f, 0f, 1f))
        g.fillOval(-6, -6, 12, 12)
      }
    }

    g.resetTransform
  }
  
  def processCommand(command: Char) {
    command match {
      case Commands.GROUND => {loadGround}
      case Commands.PING   => {resetTimeout}
      case Commands.TANKS => {processUpdate}
      case Commands.PROJECTILE => {loadProjectile}
      case Commands.PROJECTILES => {loadProjectiles}
    }
  }
  
  def keyPressed(key : Int, char : Char) {
    char match {
      case 'a' => { sendCommand(Commands.MOVE_LEFT) }
      case 'd' => { sendCommand(Commands.MOVE_RIGHT) }
      case _ => {
        key match {
          case Input.KEY_LEFT  => { sendCommand(Commands.AIM_ANTICLOCKWISE) }
          case Input.KEY_RIGHT => { sendCommand(Commands.AIM_CLOCKWISE) }
          case Input.KEY_UP    => { sendCommand(Commands.POWER_UP) }
          case Input.KEY_DOWN  => { sendCommand(Commands.POWER_DOWN) }
          case Input.KEY_SPACE => { sendCommand(Commands.FIRE) }
          case Input.KEY_TAB   => { sendCommand(Commands.CYCLE_WEAPON) }
          case _ => {}
        }
      }
    }
  }
  
  def keyReleased(key : Int, char : Char) {
    char match {
      case 'a' => { sendCommand(Commands.STOP_MOVE_LEFT) }
      case 'd' => { sendCommand(Commands.STOP_MOVE_RIGHT) }
      case _ => {
        key match {
          case Input.KEY_LEFT  => { sendCommand(Commands.STOP_AIM_ANTICLOCKWISE) }
          case Input.KEY_RIGHT => { sendCommand(Commands.STOP_AIM_CLOCKWISE) }
          case Input.KEY_UP    => { sendCommand(Commands.STOP_POWER_UP) }
          case Input.KEY_DOWN  => { sendCommand(Commands.STOP_POWER_DOWN) }
          case _ => {}
        }
      }
    }
  }
  
  def ping = {
    if (new Date().getTime - lastPing.getTime > PING_PERIOD) {
      sendPing
      lastPing = new Date()
    }
  }

  def checkTimeout = {
    if (new Date().getTime - lastPong.getTime > SERVER_TIMEOUT) {
      println("Connection timed out.")
      super.leave()
    }
  }

  def resetTimeout = {
    lastPong = new Date
  }

  def loadGround = {
    val groundArray = new Array[byte](data.remaining)
    if (ground != null && null != ground.body) {
      removeBody(ground.body)
    }
    data.get(groundArray)
    ground.loadFrom(groundArray)
  }

  def loadProjectile = {
    val projArray = new Array[byte](data.remaining)
    data.get(projArray)
    addProjectile(ProjectileLoader.loadProjectile(projArray, this))
  }

  def loadProjectiles = {
    val projArray = new Array[byte](data.remaining)
    data.get(projArray)

    val projDataList = Operations.fromByteArray[List[Array[byte]]](projArray)

    for (projectile <- projectiles) {
      removeProjectile(projectile)
    }

    projectiles = projDataList.map(projectileData => {
      ProjectileLoader.loadProjectile(projectileData, this)
    })
  }
  
  def processUpdate = {
    val byteArray = new Array[byte](data.remaining)
    data.get(byteArray)
    
    val tankDataList = Operations.fromByteArray[List[Array[byte]]](byteArray)
  
    if (tanks.length == tankDataList.length) {
      for (i <- 0 until tanks.length) {
        tanks(i).loadFrom(tankDataList(i))
      }
    }
    else {
      for (tank <- tanks) {
        removeBody(tank.body)
      }
      tanks = tankDataList.map(tankData => {
        val t = new Tank(this)
        t.create(0, null)
        t.loadFrom(tankData)
        t
      })
    }
  }

  def sendCommand(command: Byte) {
    send(byteToArray(command))
  }

  def sendHello = {
    send(byteToArray(Commands.HELLO) ++ name.getBytes)
  }

  def sendPing = {
    send(byteToArray(Commands.PING))
  }

  def send(data: Array[byte]) {
    channel.write(ByteBuffer.wrap(data))
  }
}
