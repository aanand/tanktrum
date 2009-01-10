package shared
import java.util.Properties
import java.io.FileInputStream

object Config extends Properties {
  val cl = getClass().getClassLoader()
  load(Resource.get("config.properties"))
  
  def apply(s: String) = {
    val value = getProperty(s)
    
    if (null == value) {
      throw new RuntimeException("Property " + s + " is not set")
    } else {
      value
    }
  }
}
