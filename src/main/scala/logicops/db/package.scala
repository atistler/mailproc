package logicops {

package object db {

  import java.io.File
  import org.orbroker._
  import javax.sql.DataSource
  import config.FileSystemRegistrant
  import config.{TokenSet, dynamic, BrokerBuilder, SimpleDataSource}


  private val dbhost = sys.props.getOrElse("logicops.dbhost", "lw-dev")

  private val DB_URL = "jdbc:postgresql://" + dbhost + "/logicops2"
  private val DB_USERNAME = "logicops2"
  private val DB_DRIVER = "org.postgresql.Driver"

  private[db] val ds : DataSource = new SimpleDataSource(DB_URL, DB_DRIVER) {
    override def getConnection() = {
      getConnection(DB_USERNAME, "")
    }
  }
  private[db] val configFolder = new File("sql")
  private[db] val builder = new BrokerBuilder(ds) with dynamic.FreeMarkerSupport
  FileSystemRegistrant(configFolder).register(builder)
  private[db] val tokens : List[TokenSet] = List(
    Attribute.Tokens, AttributeOption.Tokens, Connection.Tokens, ConnectionType.Tokens, Node.Tokens,
    NodeAttribute.Tokens, NodeType.Tokens, Template.Tokens, TemplateAttribute.Tokens
  )
  val broker = builder.build()

  object Database {

    private object connection extends ThreadLocal[java.sql.Connection] {
      override def initialValue() = {
        val conn = ds.getConnection
        conn.setAutoCommit(false)
        conn
      }
    }

    def getConnection = {
      if (connection.get == null || connection.get.isClosed) {
        val conn = ds.getConnection
        conn.setAutoCommit(false)
        connection.set(conn)
      }
      connection.get
    }
  }

  private[db] def mapToIds(iter : Iterable[Dao[_]]) : Array[Int] = {
    iter.map(_.id.get).toArray
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

  implicit def NodeMap2Nodes(map : collection.mutable.Map[Int, Node]) = {
    new Nodes(map)
  }

  /*
  implicit def IndexedSeq2ConnectionSeq(i : IndexedSeq[Connection]) = {
    new ConnectionSeq(i);
  }
  implicit def IndexedSeq2NodeSeq(n : IndexedSeq[Node]) = {
    new NodeSeq(n)
  }
  */


  class DaoException(msg : String) extends RuntimeException(msg)

  private[db] trait Dao[T] {

    val id : Option[Int]

    protected val companion : DaoHelper[T]

    def save() : Option[T] = {
      id match {
        case Some(_id) => {
          broker.transactional(Database.getConnection) {
            _.executeForKey(companion.Tokens.update, "object" -> this)
          }
        }
        case None => {
          broker.transactional(Database.getConnection) {
            _.executeForKey(companion.Tokens.insert, "object" -> this)
          }
        }
      }
    }

    def delete() {
      broker.transactional(Database.getConnection) {
        _.execute(companion.Tokens.deleteById, "id" -> this.id.get)
      }
    }

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

  private[db] trait NamedDao[T] extends Dao[T] {
    override def hashCode = id.get
    override def equals(that: Any) =
      that match {
        case nd : NamedDao[_] => {
          nd.getClass == getClass && nd.id.get == id.get
        }
        case _ => false
      }
  }

  private[db] trait DaoHelper[T] {
    val extractor : QueryExtractor[T]
    val pK : String
    val tableName : String
    val Tokens : BasicTokens
    val columnMap : Map[String, String]

    def getDb(id : Int) : T = getOptionDb(id).get

    def getOptionDb(id : Int) : Option[T] = selectOneOption(Tokens.selectById, "id" -> id)

    private val _getById = Memoize1(getDb)
    private val _getByIdOption = Memoize1(getOptionDb)

    def get(id : Int) = _getById(id)

    def getOption(id : Int) = _getByIdOption(id)

    protected def selectOneOption(token : Token[T], params : (String, Any)*) = {
      broker.transactional(Database.getConnection) {
        _.selectOne(token, params : _*)
      }
    }

    protected def selectAllOption(token : Token[T], params : (String, Any)*) = {
      broker.transactional(Database.getConnection) {
        _.selectAll(token, params : _*)
      }
    }



    class BasicTokens extends TokenSet(true) {

      val selectById = Token("SELECT * FROM " + tableName + " WHERE " + pK + " = :id", 'id, extractor)
      val deleteById = Token("DELETE FROM " + tableName + " WHERE " + pK + " = :id", 'id)

      val update = Token(_createUpdateStmt, 'id, extractor)
      val insert = Token(_createInsertStmt, 'id, extractor)

      private def _createUpdateStmt : String = {
        var stmt = "UPDATE " + tableName + " SET "
        stmt += columnMap.map(
          x => {
            "%s = :%s.%s".format(x._1, "object", x._2)
          }
        ).mkString(", ")
        stmt += " WHERE %s = :%s.id".format(pK, "object")
        stmt
      }

      private def _createInsertStmt : String = {
        "INSERT INTO " + tableName + "(%s) VALUES (%s)".format(
          columnMap.keys.mkString(","), columnMap.values.map(":%s.%s".format("object", _)).mkString(",")
        )
      }
    }
  }

  implicit def tuple2Na(s: (String,String)) : ( Attribute, String ) = {
     Attribute.get(s._1) -> s._2
  }

  implicit def string2Ct(s : String) : ConnectionType = {
    ConnectionType.getOption(s) match {
      case Some(ct) => ct
      case None => throw new DaoException("Invalid interface_name for ConnectionType: %s".format(s))
    }
  }
  implicit def string2Nt(s : String) : NodeType = {
    NodeType.getOption(s) match {
      case Some(nt) => nt
      case None => throw new DaoException("Invalid interface_name for NodeType: %s".format(s))
    }
  }
  implicit def string2A(s : String) : Attribute = {
    Attribute.getOption(s) match {
      case Some(a) => a
      case None => throw new DaoException("Invalid interface_name for Attribute: %s".format(s))
    }
  }
  implicit def string2T(s : String) : Template = {
    Template.getOption(s) match {
      case Some(t) => t
      case None => throw new DaoException("Invalid interface_name for Template: %s".format(s))
    }
  }

  private[db] trait NamedDaoHelper[T <: Dao[_]] extends DaoHelper[T] {

    override val Tokens : NamedBasicTokens

    def getDb(name : String) : T = getOptionDb(name).get

    def getOptionDb(name : String) : Option[T] = selectOneOption(Tokens.selectByName, "name" -> name)

    private val _getByName = Memoize1(getDb)
    private val _getByNameOption = Memoize1(getOptionDb)

    def get(name : String) = _getByName(name)

    def getOption(name : String) = _getByNameOption(name)

    class NamedBasicTokens extends BasicTokens {
      val selectByName = Token("SELECT * FROM " + tableName + " WHERE interface_name = :name", 'name, extractor)
    }

  }

}

}