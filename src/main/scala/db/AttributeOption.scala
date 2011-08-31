package db {

import org.orbroker._

class AttributeOption(var id : Option[Int], var attributeId : Int, var value : String) extends Dao {
  override def toString = "AttributeOption[%s] (attribute: %s[%d], value: %s".format(
    id.get, attribute.name, attributeId, value
  )

  lazy val attribute = Attribute.getMem(attributeId)

  def save() = {
    broker.transaction() {
      t =>
        id match {
          case Some(_id) => t.callForUpdate(
            Tokens.AttributeOption.update, "attribute_option_id" -> _id, "attribute_id" -> attributeId,
            "value" -> value
          )
          case None => t.callForKeys(Tokens.AttributeOption.insert, "attribute_id" -> attributeId, "value" -> value) {
            k : Int => id = Some(k)
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

  def apply(attributeId : Int, value : String) : AttributeOption = {
    new AttributeOption(None, attributeId, value)
  }

  def apply(attribute : Attribute, value : String) : AttributeOption = {
    apply(attribute.id.get, value)
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