package logicops {

import java.util.Properties
import java.io.FileInputStream

package object utils {
  def findConfig(fileName : String) = {
    val props = new Properties()
    sys.env.get(fileName) match {
      case Some(file) => {
        try {
          props.load(new FileInputStream(file))
        } catch {
          case e : Exception => {
            sys.error(e.getStackTraceString)
            sys.error("Could not load properties file from filepath: " + file)
            sys.exit(1)
          }
        }
      }
      case None => {
        try {
          val resource = getClass.getResourceAsStream(fileName)
          println("RESOURCE: " + resource)
          props.load(resource)
        } catch {
          case e : Exception => {
            sys.error(e.getStackTraceString)
            sys.error("Could not load default properties file from resource: %s".format(fileName))
            sys.exit(1)
          }
        }
      }
    }
    props
  }
}

}