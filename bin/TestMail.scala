import java.io._
import java.util.Properties
import javax.mail.internet.{MimeBodyPart, MimeMultipart, InternetAddress, MimeMessage}
import javax.mail.{Transport, Message, Session}
import akka.event._
import org.apache.commons.lang.StringUtils

object TestMail extends App {

  val smtphost = "relay.ext.logicworks.net"
  val smtpport = 25
  val smtpuser = "logicops@logicworks.net"
  val smtppassword = "Th15is4l0ng4ndc0mpl1cat3Dpassw0rd!"

  val props = new Properties()
  //  props.put("mail.smtp.auth", "true")
  // props.put("mail.smtp.starttls.enable", "true")
  props.put("mail.smtp.connectiontimeout", "5000")
  /* 	Socket connection timeout value in milliseconds. Default is infinite timeout. */
  props.put("mail.smtp.timeout", "5000")
  /* Socket I/O timeout value in milliseconds. Default is infinite timeout. */
  val session = Session.getInstance(props)

  EventHandler.info(this, "EventHandler is logging")
  session.getTransport("smtp").connect(smtphost, smtpport, smtpuser, smtppassword)


  session.setDebug(true)
  session.setDebugOut(new PrintStream(new ByteArrayOutputStream {
    override def flush() {
      val s = StringUtils.chomp(this.toString, "\n")
      if ( s.nonEmpty ) {
        EventHandler.info(this, s)
      }
      reset()
    }
  }, true))




  val message = new MimeMessage(session)
  message.setFrom(new InternetAddress("logicops@logicworks.net"))


  message.setSubject("Test email")

  message.setRecipients(Message.RecipientType.TO, "atistler@gmail.com")
  message.setRecipients(
    Message.RecipientType.BCC, "atistler@logicworks.net"
  )
  val multipart = new MimeMultipart("alternative")
  val plaintext = new MimeBodyPart()
  val html = new MimeBodyPart()

  val html_content = <p>Hello Dolly</p>

  plaintext.setContent("Hello Dolly", "text/plain")
  html.setContent(html_content.toString(), "text/html")
  multipart.addBodyPart(plaintext)
  multipart.addBodyPart(html)
  message.setContent(multipart)

  Transport.send(message);
}
