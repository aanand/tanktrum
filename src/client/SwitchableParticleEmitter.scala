package client 
import org.newdawn.slick.particles._

object SwitchableParticleEmitter {
  val emitters = new scala.collection.mutable.HashMap[ConfigurableEmitter, SwitchableParticleEmitter]
  
  implicit def wrapEmitter(e: ConfigurableEmitter) = {
    if (!emitters.isDefinedAt(e)) {
      emitters.put(e, new SwitchableParticleEmitter(e))
    }
    
    emitters(e)
  }
}

class SwitchableParticleEmitter(e: ConfigurableEmitter) {
  val originalSpawnCountMin = e.spawnCount.getMin
  val originalSpawnCountMax = e.spawnCount.getMax

  val angularOffset = e.angularOffset.asInstanceOf[ConfigurableEmitter$SimpleValue]
  val originalAngularOffset = angularOffset.getValue(0)

  def setEmitting(emitting: Boolean) {
    if (emitting) {
      e.spawnCount.setMin(originalSpawnCountMin)
      e.spawnCount.setMax(originalSpawnCountMax)
    } else {
      e.spawnCount.setMin(0f)
      e.spawnCount.setMax(0f)
    }
  }
  
  def setRotation(rotation: Float) {
    angularOffset.setValue(originalAngularOffset + rotation)
  }
}
