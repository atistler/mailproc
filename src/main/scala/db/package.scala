import java.io.File
import org.orbroker.config.FileSystemRegistrant
import org.orbroker.config.{TokenSet, dynamic, BrokerBuilder, SimpleDataSource}
import org.orbroker._

package object db {

  private val URL = "jdbc:postgresql://lw-dev/logicops2"
  private val USERNAME = "logicops2"

  private val ds : javax.sql.DataSource = new SimpleDataSource(URL, "org.postgresql.Driver") {
    override def getConnection() = {
      getConnection(USERNAME, "")
    }
  }

  implicit def IndexedSeq2ConnectionSeq(i : IndexedSeq[Connection]) = {
    new ConnectionSeq(i);
  }

  implicit def IndexedSeq2NodeSeq(n : IndexedSeq[Node]) = {
    new NodeSeq(n)
  }

  private val configFolder = new File("sql")
  private val builder = new BrokerBuilder(ds) with dynamic.FreeMarkerSupport
  FileSystemRegistrant(configFolder).register(builder)
  builder.verify(Tokens.idSet)
  val broker = builder.build()

  def mapToIds(d : List[Dao]) = {
    d.map(_.id.get)
  }

  private[db] class Memoize1[-T, +R](f : T => R) extends (T => R) {

    import scala.collection.mutable

    private[this] val vals = mutable.Map.empty[T, R]

    def apply(x : T) : R = {
      if (vals.contains(x)) {
        vals(x)
      } else {
        val y = f(x)
        vals += ((x, y))
        y
      }
    }
  }

  private[db] object Memoize1 {
    def apply[T, R](f : T => R) = new Memoize1(f)
  }

  private[db] class Def[C](implicit desired : Manifest[C]) {
    def unapply[X](c : X)(implicit m : Manifest[X]) : Option[C] = {
      def sameArgs =
        desired.typeArguments.zip(m.typeArguments).forall {
          case (desired, actual) => desired >:> actual
        }
      if (desired >:> m && sameArgs) Some(c.asInstanceOf[C])
      else None
    }
  }

  class DaoException(msg : String) extends Exception(msg)

  trait Dao {

    var id : Option[Int]

    sealed trait State {
      def name : String
    }

    case object NEW extends State {
      val name = "NEW"
    }

    case object MODIFIED extends State {
      val name = "MODIFIED"
    }

    case object FROMDB extends State {
      val name = "FROMDB"
    }

  }

  trait DaoHelper[T] {
    def get(id : Int) : T = {
      getOption(id).get
    }

    val getMem = Memoize1(get)

    val getOptionMem = Memoize1(getOption)

    def getOption(id : Int) : Option[T]

    protected def selectOneOption(token : Token[T], params : (String, Any)*) = {
      broker.readOnly() {
        _.selectOne(token, params : _*)
      }
    }

    protected def selectAllOption(token : Token[T], params : (String, Any)*) = {
      broker.readOnly() {
        _.selectAll(token, params : _*)
      }
    }
  }

  object Tokens extends TokenSet(true) {

    object Node {
      val selectById = Token('selectNodeById, NodeExtractor)
      val selectByIds = Token('selectNodeByIds, NodeExtractor)
      val insert = Token[Int]('insertNode)
      val selectByConnector = Token('selectNodesByConnector, NodeExtractor)
      val selectByConnectee = Token('selectNodesByConnectee, NodeExtractor)
    }

    object NodeType {
      val selectById = Token('selectNodeTypeById, NodeTypeExtractor)
      val selectByName = Token('selectNodeTypeByName, NodeTypeExtractor)
      val selectByPoolId = Token('selectNodeTypesByPoolId, NodeTypeExtractor)
      val update = Token('updateNodeType)
      val insert = Token[Int]('insertNodeType)

    }

    object NodeAttribute {
      val selectById = Token('selectNodeAttributeById, NodeAttributeExtractor)
      val insert = Token[Int]('insertNodeAttribute)
      val update = Token('updateNodeAttribute)
    }

    object Connection {
      val selectById = Token('selectConnectionById, ConnectionExtractor)
      /*
      val selectByConnectorNodeId = Token('selectConnectionsByConnectorNodeId, ConnectionExtractor)
      val selectByConnecteeNodeId = Token('selectConnectionsByConnecteeNodeId, ConnectionExtractor)
      */
      val insert = Token[Int]('insertConnection)
    }

    object ConnectionType {
      val selectById = Token('selectConnectionTypeById, ConnectionTypeExtractor)
      val selectByName = Token('selectConnectionTypeByName, ConnectionTypeExtractor)
      val insert = Token[Int]('insertConnectionType)
      val update = Token('updateConnectionType)
    }

    object Template {
      val selectById = Token('selectTemplateById, TemplateExtractor)
      val selectByName = Token('selectTemplateByName, TemplateExtractor)
      val selectByQuery = Token('selectTemplateByQuery, TemplateExtractor)
      val update = Token('updateTemplate)
      val insert = Token('insertTemplate)
    }

    object TemplateAttribute {
      val selectById = Token('selectTemplateAttributeById, TemplateAttributeExtractor)
      val update = Token('updateTemplateAttribute)
      val insert = Token[Int]('insertTemplateAttribute)
    }

    object Attribute {
      val selectById = Token('selectAttributeById, AttributeExtractor)
      val selectByName = Token('selectAttributeByName, AttributeExtractor)
      val update = Token('updateAttribute)
      val insert = Token[Int]('insertAttribute)
    }

    object AttributeOption {
      val selectById = Token('selectAttributeOptionById, AttributeOptionExtractor)
      val selectOptionsByAttributeId = Token('selectAttributeOptionsByAttributeId, AttributeOptionExtractor)
      val update = Token('updateAttributeOption)
      val insert = Token[Int]('insertAttributeOption)
    }

  }

}