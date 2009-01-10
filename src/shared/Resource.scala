package shared
import java.io._

object Resource {
  def get(path: String) = {
    println("Loading " + path)
    if (new File(path).exists) {
      new FileInputStream(path)
    }
    else {
      getClass.getClassLoader.getResourceAsStream(path)
    }
  }
}
