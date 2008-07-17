import java.util.Random

class PerlinNoise(size: Int, layers: Int, pers: float) {
  val rand = new Random
  var noise = new Array[float](size+1)
  
  def generate = {
    var persistence = pers

    var step = size
    var weight = 0.0f
    var layer = 0

    while (step >= 1 && layer < layers) {
      layer += 1
      val layerSize = Math.ceil(size.toFloat/step).toInt + 1
      val layerNoise = new Range(0, layerSize, 1).map((i) => rand.nextFloat * 2 - 1).toArray
      
      for (i <- new Range(0, layerSize-1, 1)) {
        noise(i*step) += layerNoise(i) * persistence

        for (j <- new Range(1, step, 1)) {
          if (i*step+j < size) {
            noise(i*step+j) += interpolate(layerNoise(i), layerNoise(i+1), j.toFloat/step) * persistence
          }
        }
      }
      step /= 2
      weight += persistence
      persistence *= persistence
    }
    noise = noise.map((i) => i/weight).toArray
    noise
  }

  def interpolate(a: float, b: float, x: float) = {
    a*(1-x) + b*x
  }
}
