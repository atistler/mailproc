package logicops.mailproc

import akka.actor.Actor
import akka.event.EventHandler
import java.io.File
import MailProc._

class DirectoryWatcher(var directory : String) extends Actor {

  override def preStart() {
    EventHandler.debug(this, "In preStart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  override def preRestart(reason : Throwable) {
    EventHandler.error(reason, this, "Actor %s %s, restarted".format(self.getClass.getName, self.uuid))
  }

  override def postRestart(reason : Throwable) {
    EventHandler.debug(this, "In postRestart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  lazy val new_directory = "%s/%s".format(directory, "new")

  def receive = {
    case ProcessFiles(num) => {
      EventHandler.info(this, "Processing up to %d files in %s".format(num, new_directory))
      for (file <- new File(new_directory).listFiles().take(num)) {
        // EventHandler.debug(this, "Parsing file: %s".format(file))
        if (emailParser.isRunning) {
          emailParser ! EmailFile(file)
        }
      }
    }
    case _ => EventHandler.error(this, "Unknown message sent to DirectoryWatcher actor")
  }
}