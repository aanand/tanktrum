package shared
import java.io._

object Resource {
  def get(path: String) = {
    if (new File(path).exists) {
      new FileInputStream(path)
    }
    else {
      getClass.getResourceAsStream(path)
    }
  }
}
