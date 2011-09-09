package logicops.util {

import org.specs2.mutable._
import org.specs2.specification.{Step, Fragments}
import logicops.db._

trait RollbackSpec {
  this : Specification =>
  def init() {}

  def cleanup() {
    DatabaseContext.get().rollback()
  }

  override def map(fs : => Fragments) = Step(init) ^ fs ^ Step(cleanup)
}

}