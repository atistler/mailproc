package db {

import org.orbroker._

class AttributeOption(val id : Option[Int], val attributeId : Int, val value : String) extends Dao {
  override def toString = "AttributeOption[%s] (attribute: %s[%d], value: %s".format(
    id.get, attribute.name, attributeId, value
  )

  def copy(id : Option[Int] = this.id, attributeId : Int = this.attributeId, value : String = this.value) = {
    new AttributeOption(id, attributeId, value)
  }

  lazy val attribute = Attribute.getMem(attributeId)

  def save() = {
    broker.transactional(connection) {
      t =>
        id match {
          case Some(_id) => t.execute(
            Tokens.AttributeOption.update, "attributeOption" -> this
          )
          case None => t.executeForKeys(Tokens.AttributeOption.insert, "attributeOption" -> this) {
            k : Int => copy(id = Some(k))
          }
        }
    }
  }
}

object AttributeOption extends DaoHelper[AttributeOption] {
  def apply(id : Int, attributeId : Int, value : String) : AttributeOption = {
    new AttributeOption(Some(id), attributeId, value)
  }

  def apply(id : Int, attribute : Attribute, value : String) : AttributeOption = {
    apply(id, attribute.id.get, value)
  }

  def apply(id : Int, attribute : String, value : String) : AttributeOption = {
    apply(id, Attribute.get(attribute), value)
  }

  def apply(attributeId : Int, value : String) : AttributeOption = {
    new AttributeOption(None, attributeId, value)
  }

  def apply(attribute : Attribute, value : String) : AttributeOption = {
    apply(attribute.id.get, value)
  }

  def apply(attribute : String, value : String) : AttributeOption = {
    apply(Attribute.get(attribute), value)
  }

  def getOption(id : Int) = {
    selectOneOption(Tokens.AttributeOption.selectById, "attribute_option_id" -> id)
  }
}

object AttributeOptionExtractor extends JoinExtractor[AttributeOption] {
  val key = Set("attribute_option_id")

  def extract(row : Row, join : Join) = {
    AttributeOption(
      row.integer("attribute_option_id").get,
      row.integer("attribute_id").get,
      row.string("value").get
    )
  }
}

}