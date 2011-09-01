package db {

import org.orbroker._

class TemplateAttribute(
  val id : Option[Int], val templateId : Int, val attributeId : Int, val optional : Boolean, val value : String
  ) extends Dao {

  override def toString = "TemplateAttribute[%s] (template: %s[%d], attribute: %s[%d], value: %s)".format(
    id, template.name, templateId, attribute.name, attributeId, value
  )

  def copy(id : Option[Int] = this.id, templateId : Int = this.templateId, attributeId : Int = this.attributeId, optional : Boolean = this.optional, value : String = this.value) = {
    new TemplateAttribute(id, templateId, attributeId, optional, value)
  }

  lazy val template = Template.getMem(templateId)
  lazy val attribute = Attribute.getMem(attributeId)

  def save() = {
    broker.transactional(connection) {
      t =>
        id match {
          case Some(_id) => t.execute(
            Tokens.TemplateAttribute.update, "templateAttribute" -> this
          )
          case None => t.executeForKeys(
            Tokens.TemplateAttribute.insert, "templateAttribute" -> this
          ) {
            k : Int => copy(id = Some(k))
          }
        }
    }
  }
}

object TemplateAttribute extends DaoHelper[TemplateAttribute] {

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

  def getOption(id : Int) = {
    selectOneOption(Tokens.TemplateAttribute.selectById, "template_attribute_map_id" -> id)
  }

  /* TODO:
   def all(attribute: Attribute*): IndexedSeq[TemplateAttribute]
   def all(template: Template*): IndexedSeq[TemplateAttribute]
  */
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