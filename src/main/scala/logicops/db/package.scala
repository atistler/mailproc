package logicops {

package object db {

  import java.io.File
  import org.orbroker._
  import javax.sql.DataSource
  import config.FileSystemRegistrant
  import config.{TokenSet, dynamic, BrokerBuilder, SimpleDataSource}

  private val DB_URL = "jdbc:postgresql://lw-dev/logicops2"
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
      if ( connection.get == null || connection.get.isClosed ) {
        val conn = ds.getConnection
        conn.setAutoCommit(false)
        connection.set(conn)
      }
      connection.get
    }
  }

  private[db] def mapToIds(d : Iterable[Dao[_]]) = {
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

    def delete() {
      broker.transactional(Database.getConnection) {
        _.execute(companion.Tokens.deleteById, "id" -> id)
      }
    }

    def save() : T = {
      id match {
        case Some(_id) => {
          broker.transactional(Database.getConnection) {
            _.execute(
              companion.Tokens.update, "object" -> this
            )
          }
          this.asInstanceOf[T]
        }
        case None => {
          var newDao : Option[T] = None
          broker.transactional(Database.getConnection) {
            _.executeForKeys[T](companion.Tokens.insert, "object" -> this) {
              dao : T => newDao = Some(dao)
            }
          }
          newDao.get
        }
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

  private[db] trait DaoHelper[T] {
    val extractor : QueryExtractor[T]
    val pK : String
    val tableName : String
    val Tokens : BasicTokens
    val columnMap : Map[String, String]

    def get(id : Int) : T = {
      getOption(id).get
    }

    val getMem = Memoize1(get)

    val getOptionMem = Memoize1(getOption)

    def getOption(id : Int) : Option[T] = {
      selectOneOption(Tokens.selectById, "id" -> id)
    }

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

      val update = Token[T](_createUpdateStmt, 'id)
      val insert = Token[T](_createInsertStmt, 'id, extractor)

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

  private[db] trait NamedDaoHelper[T] extends DaoHelper[T] {
    override val Tokens : NamedBasicTokens

    def get(name : String) : T = {
      getOption(name).get
    }

    def getOption(name : String) : Option[T] = {
      selectOneOption(Tokens.selectByName, "name" -> name)
    }

    class NamedBasicTokens extends BasicTokens {
      val selectByName = Token("SELECT * FROM " + tableName + " WHERE interface_name = :name", 'name, extractor)
    }

  }

}

}