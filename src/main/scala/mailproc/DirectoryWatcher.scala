package mailproc {

import akka.actor.Actor
import akka.event.EventHandler
import java.io.File

class DirectoryWatcher(val directory: String) extends Actor {

  def receive = {
    case StartWatch() => {
      EventHandler.info(this, "Starting watching directory for new email: %s".format(directory))
      while (true) {
        for (file <- new File(directory).listFiles().take(30)) {
          // EventHandler.debug(this, "Parsing file: %s".format(file))
          if ( emailParser.isRunning ) {
            emailParser ! EmailFile(file)
          }
        }
        // EventHandler.debug(this, "Sleeping for 5 sec")
        Thread.sleep(2000)
      }
    }
    case _ => EventHandler.error(this, "Unknown message sent to DirectoryWatcher actor")
  }
}

}