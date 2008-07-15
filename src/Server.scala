import org.newdawn.slick

class Server extends Session {
  override def enter(container : slick.GameContainer) {
    super.enter(container)
    
    ground = Some(new Ground(this, container.getWidth(), container.getHeight()))
    ground.get.buildPoints()
  }
}