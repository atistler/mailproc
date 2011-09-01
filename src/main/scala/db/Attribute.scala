package db {

import org.orbroker._

class Attribute(val id : Option[Int], val name : String, val openended : Boolean = true) extends Dao {
  override def toString = "Attribute[%s] (name: %s)".format(id, name)

  def copy(id : Option[Int] = this.id, name : String = this.name, openended : Boolean = this.openended) = {
    new Attribute(id, name, openended)
  }

  def save() = {
    broker.transactional(connection) {
      t =>
        id match {
          case Some(_id) => t.execute(
            Tokens.Attribute.update, "attribute" -> this
          )
          case None => t.executeForKeys(Tokens.Attribute.insert, "attribute" -> this) {
            k : Int => copy(id = Some(k))
          }
        }
    }
  }

  lazy val options = {
    broker.transactional(connection) {
      _.selectAll(Tokens.AttributeOption.selectOptionsByAttributeId, "attribute_id" -> id.get)
    }
  }
}

object Attribute extends DaoHelper[Attribute] {
  def apply(id : Int, name : String, openended : Boolean) : Attribute = {
    new Attribute(Some(id), name, openended)
  }

  def apply(name: String, openended : Boolean) : Attribute = {
    new Attribute(None, name, openended)
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