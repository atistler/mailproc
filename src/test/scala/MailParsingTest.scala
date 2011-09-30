import java.io.{FileInputStream, File}
import javax.mail.internet.MimeMessage
import org.specs2.mutable._
import logicops.mailproc._
import logicops.db._
import org.specs2.specification.Scope


class MailParsingTest extends Specification {
  "Parsing a multipart email" should {
    "Have a 'text/plain' part" in new mail {
      val parts = EmailParser.findMimeTypes(msg, "text/plain")
      parts.size must_== 1
    }
    "Have a non empty content" in new mail {
      val parts = EmailParser.findMimeTypes(msg, "text/plain")
      parts.head._2.getContent.toString.length must be_>(50)
    }
  }
  "Parsing all mail in test-mail-files/" should {
    "Total collected must be 1000" in new allmail {
      total_collected must_== 1000
    }
    "Count html must be 117" in new allmail {
      count_html must_== 117
    }
    "Count plain must be 941" in new allmail {
      count_plain must_== 941
    }
  }
}

trait mail extends Scope {
  private val input = getClass.getResourceAsStream("test-mail-files/multipart-email.sample")
  val msg = new MimeMessage(EmailParser.session, input)
}

trait allmail extends Scope {
  private val url = getClass.getResource("new/")
  var count_html = 0
  var count_plain = 0
  var count_total = 0
  var total_collected = 0
  val tocollect = 1000
  for ( f <- new File(url.getFile).listFiles().zipWithIndex if f._2 < tocollect) {
    val msg = new MimeMessage(EmailParser.session, new FileInputStream(f._1))
    var counted = false
    val parts = EmailParser.findMimeTypes(msg, "text/plain", "text/html")
   // println("\nFile: " + f._1)
   // println("Number of 'text/plain' parts:" + parts.contains("text/plain"))

    parts.get("text/plain").map { p =>
      val content = p.getContent.toString
      val length = if (content.length() < 30) content.length() else 30
    //  println(content.substring(0, length))
      count_plain += 1
      total_collected += 1
      counted = true
    }
   // println("Number of 'text/html' parts:" + parts.contains("text/html"))
    parts.get("text/html").map { p =>
      val content = p.getContent.toString
      val length = if (content.length() < 30) content.length() else 30
      // println(content.substring(0, length))
      count_html += 1
      if ( ! counted ) total_collected += 1
    }
    if ( parts.contains("text/html") && !parts.contains("text/plain")) {
       val cleaned = EmailParser.getPlainTextContent(parts)
       // println("\n\n" + cleaned)
    }
  }
  //println("Total: " + total_collected)
  //println("Plain: " + count_plain)
  //println("HTML: " + count_html)
}