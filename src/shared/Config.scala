package shared
import java.util.prefs._
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

object Prefs {
  val prefs = Preferences.userRoot.node("boomtrapezoid")
  
  def apply (name: String, defaultProperty: String) = {
    prefs.get(name, Config(defaultProperty))
  }
  
  def apply (name: String) = {
    prefs.get(name, Config(name))
  }

  def save(name: String, value: String) = {
    prefs.put(name, value)
  }
}
