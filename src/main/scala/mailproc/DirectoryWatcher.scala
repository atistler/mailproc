package mailproc {

import akka.actor.Actor
import akka.event.EventHandler
import java.io.File

class DirectoryWatcher(var directory: String) extends Actor {
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

  lazy val cur_directory = "%s/%s".format(directory, "cur")

  def receive = {
    case StartWatch() => {
      EventHandler.info(this, "Starting watching directory for new email: %s".format(cur_directory))
      while (true) {
        for (file <- new File(cur_directory).listFiles().take(2000)) {
          // EventHandler.debug(this, "Parsing file: %s".format(file))
          if ( emailParser.isRunning ) {
            emailParser ! EmailFile(file)
          }
        }
        // EventHandler.debug(this, "Sleeping for 2 sec")
        MailProc.supervisor.shutdown()
        EventHandler.debug(this, "Done shutting down all actors")
        sys.exit()
        Thread.sleep(2000)
      }
    }
    case _ => EventHandler.error(this, "Unknown message sent to DirectoryWatcher actor")
  }
}

}