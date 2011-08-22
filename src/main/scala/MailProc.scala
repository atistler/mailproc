import scala.io.Source
import scala.actors.Actor
import scala.actors.Actor._
import java.io._
import javax.mail.{Session => MailSession}
import javax.mail.internet._
import db._


object MailProc {

  val LO_MAIL_DIR = "/home/logicops/Maildir/new"

  case class EmailFile(file : File)

  class EmailParser extends Actor {
    val properties = System.getProperties;
    val session = MailSession.getDefaultInstance(properties);

    def act() {
      loop {
        react {
          case EmailFile(file) => {
            val lines = Source.fromFile(file).mkString
            val is = new ByteArrayInputStream(lines.getBytes)
            val message = new MimeMessage(session, is)
            val subject = message.getHeader("Subject")
            println("In parser thread: " + Thread.currentThread())
            println("Subject: " + subject(0))
          }
        }
      }
    }
  }


  def main(args : Array[String]) {
    /*
    val parser = new EmailParser()
    parser.start()
    */


    try {
      val node = Node.get(1)
      println(node)
      println(node.getAttr("Description"))
      val template = Template.get(4)
      println(template)
      println(template.getAttr("Name"))

    } catch {
      case e : NoSuchElementException => println(e.getMessage)
      case e : Exception => println(e.getMessage)
    }
    /*
    println(node.getAttr("Description"))
    println(node.getAttr("Descriptio"))
    */
    /*
    val MAX = 5
    while(true) {
      var i = 0;
      for (file <- new File(LO_MAIL_DIR).listFiles if i < MAX) {
	println("Sending " + i + ": " + file + " to parser")
    	parser ! EmailFile(file)
	i = i + 1
      }
      println("Sleeping for 10000")
      println("In main thread: " + Thread.currentThread())
      Thread.sleep(10000)
    }
    */
  }
}
