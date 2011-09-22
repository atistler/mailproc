import java.util.Properties

package object testutil {

  import org.specs2.mutable._
  import org.specs2.specification.{Step, Fragments}
  import logicops.db._

  val PROPS = new Properties
  PROPS.load(getClass.getResourceAsStream("/mailproc.properties"))

  trait RollbackSpec {
    this : Specification =>
    def init() {}
    def cleanup() {
      Database.getConnection.rollback()
    }
    override def map(fs : => Fragments) = Step(init) ^ fs ^ Step(cleanup)
  }
}