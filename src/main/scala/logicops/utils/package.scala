package logicops {

import java.util.Properties
import java.io.{PrintWriter, File, FileInputStream}

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

  def timed(blockName : String)(f : => Any) = {
    val start = System.currentTimeMillis
    f
    () => blockName + " took " + (System.currentTimeMillis - start) + "ms."
  }

  def printToFile(f : File)(op : PrintWriter => Unit) {
    val p = new PrintWriter(f)
    try {
      op(p)
    } finally {
      p.close()
    }
  }

  private[logicops] class Memoize1[-T, +R](f : T => R) extends (T => R) {

    import scala.collection.mutable

    private[this] val vals = mutable.Map.empty[T, R]

    def apply(x : T) : R = {
      if (vals.contains(x)) {
        vals(x)
      } else {
        val y = f(x)
        vals += ((x, y))
        y
      }
    }
  }

  private[logicops] object Memoize1 {
    def apply[T, R](f : T => R) = new Memoize1(f)
  }

  private[logicops] class Def[C](implicit desired : Manifest[C]) {
    def unapply[X](c : X)(implicit m : Manifest[X]) : Option[C] = {
      def sameArgs =
        desired.typeArguments.zip(m.typeArguments).forall {
          case (desired, actual) => desired >:> actual
        }
      if (desired >:> m && sameArgs) Some(c.asInstanceOf[C])
      else None
    }
  }

}

}