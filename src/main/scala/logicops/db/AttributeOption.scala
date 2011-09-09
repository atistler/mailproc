package logicops.db {

import org.orbroker._

class AttributeOption(val id : Option[Int], val attributeId : Int, val value : String) extends Dao[AttributeOption] {
  override def toString = "AttributeOption[%s] (attribute: %s[%d], value: %s".format(
    id.get, attribute.name, attributeId, value
  )

  protected val companion = AttributeOption

  def copy(id : Option[Int] = this.id, attributeId : Int = this.attributeId, value : String = this.value) = {
    new AttributeOption(id, attributeId, value)
  }

  lazy val attribute = Attribute.getMem(attributeId)
}

object AttributeOption extends DaoHelper[AttributeOption] {

  val tableName = "attribute_options"
  val pK = "attribute_option_id"
  val extractor = AttributeOptionExtractor
  val columnMap = Map("attribute_id" -> "attributeId", "value" -> "value")

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

  object Tokens extends BasicTokens {
    val selectOptionsByAttributeId = Token('selectAttributeOptionsByAttributeId, AttributeOptionExtractor)
    /*
    val update = Token('updateAttributeOption)
    val insert = Token('insertAttributeOption)
    */
  }

}

object AttributeOptionExtractor extends RowExtractor[AttributeOption] {
  val key = Set("attribute_option_id")

  def extract(row : Row) = {
    AttributeOption(
      row.integer("attribute_option_id").get,
      row.integer("attribute_id").get,
      row.string("value").get
    )
  }
}

}