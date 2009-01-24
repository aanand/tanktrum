package shared
import java.io._
import javax.swing.ImageIcon

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

  def getImageIcon(path: String) = {
    if (new File(path).exists) {
      new ImageIcon(path)
    } else {
      new ImageIcon(getClass.getClassLoader.getResource(path))
    }
  }
}
