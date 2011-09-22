package logicops.db {

import org.orbroker._

class Attribute(val id : Option[Int], val name : String, val openended : Boolean = true) extends NamedDao[Attribute] {
  override def toString = "Attribute[%s] (name: %s)".format(id, name)

  protected val companion = Attribute

  def copy(id : Option[Int] = this.id, name : String = this.name, openended : Boolean = this.openended) = {
    new Attribute(id, name, openended)
  }

  lazy val options = {
    broker.transactional(Database.getConnection) {
      _.selectAll(AttributeOption.Tokens.selectOptionsByAttributeId, "attribute_id" -> id.get)
    }
  }
}

object Attribute extends NamedDaoHelper[Attribute] {

  val tableName = "attributes"
  val pK = "attribute_id"
  val extractor = AttributeExtractor
  val columnMap = Map("interface_name" -> "name", "openended" -> "openended")

  def apply(id : Int, name : String, openended : Boolean) : Attribute = {
    new Attribute(Some(id), name, openended)
  }

  def apply(name : String, openended : Boolean) : Attribute = {
    new Attribute(None, name, openended)
  }

  object Tokens extends NamedBasicTokens {
  }
}

object AttributeExtractor extends RowExtractor[Attribute] {
  val key = Set("attribute_id")

  def extract(row : Row) = {
    Attribute(
      row.integer("attribute_id").get,
      row.string("interface_name").get,
      row.bit("openended").get
    )
  }
}

}