package db {

import org.orbroker._

class Attribute(var id : Option[Int], var name : String, var openended : Boolean = true) extends Dao {
  override def toString = "Attribute[%s] (name: %s)".format(id, name)

  def save() = {
    broker.transaction() {
      t =>
        id match {
          case Some(_id) => t.callForUpdate(
            Tokens.Attribute.update, "attribute_id" -> _id, "interface_name" -> name, "openended" -> openended
          )
          case None => t.callForKeys(Tokens.NodeAttribute.insert, "interface_name" -> name, "openended" -> openended) {
            k : Int => id = Some(k)
          }
        }
    }
  }

  lazy val options = {
    broker.readOnly() {
      _.selectAll(Tokens.AttributeOption.selectOptionsByAttributeId, "attribute_id" -> id.get)
    }
  }
}

object Attribute extends DaoHelper[Attribute] {
  def apply(id : Int, name : String, openended : Boolean) : Attribute = {
    new Attribute(Some(id), name, openended)
  }
  def getOption(id : Int) = {
    selectOneOption(Tokens.Attribute.selectById, "attribute_id" -> id)
  }
  def getOption(name : String) = {
    selectOneOption(Tokens.Attribute.selectByName, "interface_name" -> name)
  }
  def get(name : String) = {
    getOption(name).get
  }
}

object AttributeExtractor extends JoinExtractor[Attribute] {
  val key = Set("attribute_id")

  def extract(row : Row, join : Join) = {
    Attribute(
      row.integer("attribute_id").get,
      row.string("interface_name").get,
      row.bit("openended").get
    )
  }
}

}