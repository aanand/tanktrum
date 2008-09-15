import java.util.Properties
import java.io.FileInputStream

object Config extends Properties {
  load(new FileInputStream("config.properties"))
  
  def apply(s: String) = {
    val value = getProperty(s)
    
    if (null == value) {
      throw new RuntimeException("Property " + s + " is not set")
    } else {
      value
    }
  }
}