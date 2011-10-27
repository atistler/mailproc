package logicops.mailproc

import akka.actor.Actor
import java.util.{UUID, Properties}
import javax.mail.internet.{MimeBodyPart, MimeMultipart, MimeMessage, InternetAddress}
import akka.event.EventHandler
import javax.mail.{Transport, Message, Session, Address}
import logicops.db._
import MailProc._
import java.io._
import org.apache.commons.lang.StringUtils

class EmailSender extends Actor {
  private def getAddresses(addrs : Option[String]*) = {
    addrs.foldLeft(List.empty[Address]) {
      (l, a) => InternetAddress.parse(a.getOrElse("")).toList ::: l
    }
  }

  private def getEmailSession = {
    val props = new Properties()
    // props.put("mail.smtp.auth", "true")
    // props.put("mail.smtp.starttls.enable", "true")
    props.put(
      "mail.smtp.connectiontimeout", "5000"
    ) /* 	Socket connection timeout value in milliseconds. Default is infinite timeout. */
    props.put("mail.smtp.timeout", "5000") /* Socket I/O timeout value in milliseconds. Default is infinite timeout. */
    val session = Session.getInstance(props)

    if (isProd || isTest) {
      session.getTransport("smtp").connect(
        PROPS.getProperty("smtp-host"), PROPS.getProperty("smtp-port").toInt, PROPS.getProperty("smtp-user"),
        PROPS.getProperty("smtp-pass")
      )
    }
    session.setDebug(true)
    session.setDebugOut(
      new PrintStream(
        new ByteArrayOutputStream {
          override def flush() {
            val s = StringUtils.chomp(this.toString, "\n")
            if (s.nonEmpty) {
              EventHandler.info(this, s)
            }
            reset()
          }
        }, true
      )
    )
    session
  }

  private def sendConfirmEmail(user : Node, serviceRequest : Node, subject : String) {

    def getConfirmUrl(serviceRequest : Node) = {
      val confirm_token = UUID.randomUUID().toString
      serviceRequest.setAttr("Confirmation Token", confirm_token)
      "%s?token=%s".format(PROPS.getProperty("confirm-url"), confirm_token)
    }

    def getPlainText(user : Node, confirmUrl : String, f : Boolean => String) = {
      """
Dear %s,

Your email to support@logicworks.net has generated a new Service Request.
In order for us to process your request it must first be confirmed.

Please click on this URL to confirm: %s

Logicops NOC

%s
      """.format(user.valueOf("Name").get, confirmUrl, f(isDev || isTest))
    }

    def getHtmlContent(user : Node, confirmUrl : String, f : Boolean => xml.Node) : xml.Node = {
      <html>
        <body>
          <p>Dear
            {user.valueOf("Name")}
            ,
          </p>
          <p>Your email to support@logicworks.net has generated a new Service Request.
            In order for us to process your request it must first be confirmed.</p>
          <p>Please click on this URL to confirm:
            <a href={confirmUrl}>
              {confirmUrl}
            </a>
          </p>
          <p>Logicops NOC</p>{f(isDev || isTest)}
        </body>
      </html>
    }

    def buildMessage(session : Session, user : Node, confirmUrl : String) = {
      val message = new MimeMessage(session)
      message.setFrom(new InternetAddress(PROPS.getProperty("confirm-email-from")))
      message.setReplyTo(InternetAddress.parse(PROPS.getProperty("confirm-email-replyto")).toArray)

      message.setSubject("Re: %s (SR 3-%d) [New SR]".format(subject, serviceRequest.id.get))

      val to = getAddresses(user.valueOf("Email Address"), user.valueOf("Alternate Email Address"))
      if (isProd) {
        message.setRecipients(Message.RecipientType.TO, to.toArray)
        if (PROPS.getProperty("confirm-email-bcc", "") != "") {
          message.setRecipients(
            Message.RecipientType.BCC,
            InternetAddress.parse(PROPS.getProperty("confirm-email-bcc")).map(_.asInstanceOf[Address]).toArray
          )
        }
      } else if (isTest) {
        message.setRecipients(
          Message.RecipientType.TO,
          InternetAddress.parse(PROPS.getProperty("test-mode-recipient")).map(_.asInstanceOf[Address]).toArray
        )
      } else if (isDev) {
        message.setRecipients(
          Message.RecipientType.TO,
          InternetAddress.parse(PROPS.getProperty("dev-mode-recipient")).map(_.asInstanceOf[Address]).toArray
        )
      }

      val plaintext_content = getPlainText(
      user, confirmUrl, {
        debug =>
          if (debug) "\n\nRecipient List:\n\tTo: %s\n\tBcc: %s\n".format(
            to.mkString(", "), PROPS.getProperty("confirm-email-bcc")
          )
          else ""
      }
      )

      val html_content = getHtmlContent(
      user, confirmUrl, {
        debug =>
          if (debug) <p>Recipient List:
              <br/>
            To:
            {to.mkString(", ")}<br/>
            Bcc:
            {PROPS.getProperty("confirm-email-bcc")}<br/>
          </p>
          else <p></p>
      }
      )

      val multipart = new MimeMultipart("alternative")
      val plaintext = new MimeBodyPart()
      val html = new MimeBodyPart()

      plaintext.setContent(plaintext_content, "text/plain")
      html.setContent(html_content.toString(), "text/html")
      multipart.addBodyPart(plaintext)
      multipart.addBodyPart(html)
      message.setContent(multipart)
      message
    }

    val session = getEmailSession

    val message = buildMessage(session, user, getConfirmUrl(serviceRequest))
    EventHandler.info(
      this, "Sending New SR confirmation email to: %s, %s".format(user.valueOf("Name"), user.valueOf("Email Address"))
    )
    val bao = new ByteArrayOutputStream
    message.writeTo(bao)
    // EventHandler.debug(this, bao.toString)
    if (isProd || isTest) {
      EventHandler.debug(this, "Production/Test mode enabled, sending confirmation email")
      Transport.send(message);
    } else {
      EventHandler.debug(this, "Production/Test mode disabled, not actually sending confirmation email")
    }
  }

  private def sendReopenedEmail(user : Node, serviceRequest : Node, subject : String) {

    def buildMessage(session : Session, user : Node, serviceRequest : Node, subject : String) = {
      val message = new MimeMessage(session)
      message.setFrom(new InternetAddress(PROPS.getProperty("confirm-email-from")))

      message.setSubject("Re: %s (SR 3-%d) [Reopened]".format(subject, serviceRequest.id.get))

      val to = getAddresses(user.valueOf("Email Address"), user.valueOf("Alternate Email Address"))
      if (isProd) {
        message.setRecipients(Message.RecipientType.TO, to.toArray)
        if (PROPS.getProperty("notify-bcc", "") != "") {
          message.setRecipients(
            Message.RecipientType.BCC,
            InternetAddress.parse(PROPS.getProperty("notify-bcc")).map(_.asInstanceOf[Address]).toArray
          )
        }
      } else if (isTest) {
        message.setRecipients(
          Message.RecipientType.TO,
          InternetAddress.parse(PROPS.getProperty("test-mode-recipient")).map(_.asInstanceOf[Address]).toArray
        )
      } else if (isDev) {
        message.setRecipients(
          Message.RecipientType.TO,
          InternetAddress.parse(PROPS.getProperty("dev-mode-recipient")).map(_.asInstanceOf[Address]).toArray
        )
      }

      def getPlainText(user : Node, serviceRequest : Node, f : Boolean => String) = {
        "%s,\n\n%s has been reopened, and has been assigned to you\n\n%s".format(
          user.valueOf("Name").get, serviceRequest.valueOf("Name").get, f(isDev || isTest)
        )
      }

      def getHtmlContent(user : Node, serviceRequest : Node, f : Boolean => xml.Node) : xml.Node = {
        <html>
          <body>
            <p>
              {user.valueOf("Name").get}
              ,</p>
            <p>
              {serviceRequest.valueOf("Name").get}
              has been reopened, and has been assigned to you
            </p>{f(isDev || isTest)}
          </body>
        </html>
      }

      val plaintext_content = getPlainText(
      user, serviceRequest, {
        debug =>
          if (debug) "\n\nRecipient List:\n\tTo: %s\n".format(
            to.mkString(", ")
          )
          else ""
      }
      )

      val html_content = getHtmlContent(
      user, serviceRequest, {
        debug =>
          if (debug) <p>Recipient List:
              <br/>
            To:
            {to.mkString(", ")}<br/>
          </p>
          else <p></p>
      }
      )

      val multipart = new MimeMultipart("alternative")
      val plaintext = new MimeBodyPart()
      val html = new MimeBodyPart()

      plaintext.setContent(plaintext_content, "text/plain")
      html.setContent(html_content.toString(), "text/html")
      multipart.addBodyPart(plaintext)
      multipart.addBodyPart(html)
      message.setContent(multipart)
      message
    }
    val session = getEmailSession
    val message = buildMessage(session, user, serviceRequest, subject)

    EventHandler.info(
      this,
      "Sending reopened SR notification email to: %s, %s".format(user.valueOf("Name"), user.valueOf("Email Address"))
    )
    val bao = new ByteArrayOutputStream
    message.writeTo(bao)
    EventHandler.debug(this, bao.toString)
    if (isProd || isTest) {
      EventHandler.debug(this, "Production/Test mode enabled, sending SR reopened email")
      Transport.send(message);
    } else {
      EventHandler.debug(this, "Production/Test mode disabled, NOT actually sending SR reopened email")
    }
  }

  def receive = {
    case SendReopenedEmail(user, serviceRequest, subject) => {
      sendReopenedEmail(user, serviceRequest, subject)
    }
    case SendConfirmEmail(user, serviceRequest, subject) => {
      sendConfirmEmail(user, serviceRequest, subject)
    }
    case x => EventHandler.error(this, "Unknown message sent to EmailSender: %s".format(x))
  }
}