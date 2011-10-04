package logicops.mailproc

import akka.actor.Actor
import akka.event.EventHandler
import java.io.File

class DirectoryWatcher(var directory : String) extends Actor {
  EventHandler.info(this, "DirectoryWatcher constructor initialized")

  override def preStart() {
    EventHandler.info(this, "preStart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  override def preRestart(reason : Throwable) {
    EventHandler.info(this, "preRestart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  override def postRestart(reason : Throwable) {
    EventHandler.info(this, "postRestart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  lazy val new_directory = "%s/%s".format(directory, "new")

  def receive = {
    case ProcessFiles(num) => {
      EventHandler.info(this, "Starting watching directory for new email: %s".format(new_directory))
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