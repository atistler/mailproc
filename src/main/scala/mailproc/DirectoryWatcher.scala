package mailproc {

import akka.actor.Actor
import akka.event.EventHandler
import java.io.File
import logicops.db._

class DirectoryWatcher(val directory: String) extends Actor {
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

  def receive = {
    case StartWatch() => {
      EventHandler.info(this, "Starting watching directory for new email: %s".format(directory))
      while (true) {
        for (file <- new File(directory).listFiles().take(2000)) {
          // EventHandler.debug(this, "Parsing file: %s".format(file))
          if ( emailParser.isRunning ) {
            emailParser ! EmailFile(file)
          }
        }
        // EventHandler.debug(this, "Sleeping for 2 sec")
        sys.exit()
        Thread.sleep(2000)
      }
    }
    case _ => EventHandler.error(this, "Unknown message sent to DirectoryWatcher actor")
  }
}

}