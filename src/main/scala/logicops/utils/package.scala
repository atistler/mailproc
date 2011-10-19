package logicops {

import java.util.Properties
import java.io.{PrintWriter, File, FileInputStream}
import akka.event.EventHandler

package object utils {
  def loadConfig(file : File) = {
    EventHandler.info(this, "Loading configuration: %s".format(file))
    val props = new Properties()
    try {
      props.load(new FileInputStream(file))
      EventHandler.info(this, props)
    } catch {
      case e : Exception => {
        sys.error("Could not load properties file from filepath: " + file)
        sys.error(e.getStackTraceString)
        sys.exit(1)
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

  private[logicops] class Memoize1[-T, +R](f : T => R) extends Function1[T, R] {

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