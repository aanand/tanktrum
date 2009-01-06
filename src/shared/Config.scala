package shared;
import java.util.Properties
import java.io.FileInputStream

object Config extends Properties {
  val cl = getClass().getClassLoader()
  try {
    load(cl.getResourceAsStream("config.properties"))
  }
  catch {
    case e: Exception => {
      println("Reading properties from file.")
      load(new FileInputStream("config.properties"))
    }
  }
  
  def apply(s: String) = {
    val value = getProperty(s)
    
    if (null == value) {
      throw new RuntimeException("Property " + s + " is not set")
    } else {
      value
    }
  }
}
