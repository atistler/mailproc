package db {
  import org.orbroker._

  class TemplateAttribute(
    var id : Option[Int], var templateId : Int, var attributeId : Int, var optional : Boolean, var value : String
    ) extends Dao {
    override def toString = "TemplateAttribute[%s] (template: %s[%d], attribute: %s[%d], value: %s)".format(
      id, template.name, templateId, attribute.name, attributeId, value
    )

    lazy val template = Template.getMem(templateId)
    lazy val attribute = Attribute.getMem(attributeId)

    def save() = {
      broker.transaction() {
        t =>
          id match {
            case Some(_id) => t.callForUpdate(
              Tokens.TemplateAttribute.update, "template_attribute_map_id" -> _id, "template_id" -> template.id.get,
              "attribute_id" -> attribute.id.get, "optional" -> optional, "default_value" -> value
            )
            case None => t.callForKeys(
              Tokens.TemplateAttribute.insert, "template_id" -> template.id.get, "attribute_id" -> attribute.id.get,
              "optional" -> optional, "default_value" -> value
            ) {
              k : Int => id = Some(k)
            }
          }
      }
    }
  }

  object TemplateAttribute extends DaoHelper[TemplateAttribute] {

    def apply(
      id : Int, template_id : Int, attributeId : Int, optional : Boolean, value : String
      ) : TemplateAttribute = {
      new TemplateAttribute(Some(id), template_id, attributeId, optional, value)
    }

    def apply(
      id : Int, template_id : Int, attribute : Attribute, optional : Boolean, value : String
      ) : TemplateAttribute = {
      apply(id, template_id, attribute.id.get, optional, value)
    }

    def apply(template_id : Int, attributeId : Int, optional : Boolean, value : String) : TemplateAttribute = {
      new TemplateAttribute(None, template_id, attributeId, optional, value)
    }

    def apply(template_id : Int, attribute : Attribute, optional : Boolean, value : String) : TemplateAttribute = {
      apply(template_id, attribute.id.get, optional, value)
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