import net.phys2d

abstract class Collider {
  def collide(other : Collider, event : phys2d.raw.CollisionEvent) {}
}