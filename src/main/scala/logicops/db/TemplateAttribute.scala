package logicops.db {

import org.orbroker._

class TemplateAttribute(
  val id : Option[Int], val templateId : Int, val attributeId : Int, val optional : Boolean, val value : String
  ) extends Dao {

  override def toString = "TemplateAttribute[%s] (template: %s[%d], attribute: %s[%d], value: %s)".format(
    id, template.name, templateId, attribute.name, attributeId, value
  )

  protected val companion = TemplateAttribute

  def copy(
    id : Option[Int] = this.id, templateId : Int = this.templateId, attributeId : Int = this.attributeId,
    optional : Boolean = this.optional, value : String = this.value
    ) = {
    new TemplateAttribute(id, templateId, attributeId, optional, value)
  }

  lazy val template = Template.get(templateId)
  lazy val attribute = Attribute.get(attributeId)

}

object TemplateAttribute extends DaoHelper[TemplateAttribute] {

  val pK = "template_attribute_map_id"
  val tableName = "template_attributes_map"
  val extractor = TemplateAttributeExtractor
  val columnMap = Map(
    "template_id" -> "templateId", "attribute_id" -> "attributeId",
    "optional" -> "optional", "default_value" -> "value"
  )

  def apply(
    id : Int, templateId : Int, attributeId : Int, optional : Boolean, value : String
    ) : TemplateAttribute = {
    new TemplateAttribute(Some(id), templateId, attributeId, optional, value)
  }

  def apply(
    id : Int, template : Template, attribute : Attribute, optional : Boolean, value : String
    ) : TemplateAttribute = {
    apply(id, template.id.get, attribute.id.get, optional, value)
  }

  def apply(
    id : Int, template : String, attribute : String, optional : Boolean, value : String
    ) : TemplateAttribute = {
    apply(id, Template.get(template), Attribute.get(attribute), optional, value)
  }

  def apply(template_id : Int, attributeId : Int, optional : Boolean, value : String) : TemplateAttribute = {
    new TemplateAttribute(None, template_id, attributeId, optional, value)
  }

  def apply(template : Template, attribute : Attribute, optional : Boolean, value : String) : TemplateAttribute = {
    apply(template.id.get, attribute.id.get, optional, value)
  }

  def apply(template : String, attribute : String, optional : Boolean, value : String) : TemplateAttribute = {
    apply(Template.get(template), Attribute.get(attribute), optional, value)
  }

  /* TODO:
   def all(attribute: Attribute*): IndexedSeq[TemplateAttribute]
   def all(template: Template*): IndexedSeq[TemplateAttribute]
  */

  object Tokens extends BasicTokens
}

object TemplateAttributeExtractor extends JoinExtractor[TemplateAttribute] {
  val key = Set("template_attribute_map_id")

  def extract(row : Row, join : Join) = {
    TemplateAttribute(
      row.integer("template_attribute_map_id").get,
      row.integer("template_id").get,
      row.integer("attribute_id").get,
      row.bit("optional").get,
      row.string("default_value").getOrElse("")
    )
  }
}

}