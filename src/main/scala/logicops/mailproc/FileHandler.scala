package logicops.mailproc

import akka.actor.Actor
import akka.event.EventHandler
import java.io.File
import logicops.utils._

class FileHandler(directory : String) extends Actor {
  private val successDir = "%s/%s".format(directory, "success")
  private val failedDir = "%s/%s".format(directory, "failed")
  private val ignoredDir = "%s/%s".format(directory, "ignored")

  override def preStart() {
    EventHandler.debug(this, "In preStart() Actor %s %s".format(self.getClass.getName, self.uuid))
    for (dir <- List(successDir, failedDir, ignoredDir)) {
      if (!new File(dir).isDirectory) {
        EventHandler.info(this, "Directory: %s does not exist, creating it now".format(dir))
        new File(dir).mkdir()
      }
    }
  }

  override def preRestart(reason : Throwable) {
    EventHandler.error(reason, this, "Actor %s %s, restarted".format(self.getClass.getName, self.uuid))
  }

  override def postRestart(reason : Throwable) {
    EventHandler.debug(this, "In postRestart() Actor %s %s".format(self.getClass.getName, self.uuid))
  }

  def receive = {
    case FileSuccess(file : File) => {
      val moveto = "%s/%s".format(successDir, file.getName)
      EventHandler.info(this, "Successfully processed email file: %s, moving to: %s".format(file.getName, moveto))
      file.renameTo(new File(moveto))
    }
    case FileIgnored(file : File) => {
      val moveto = "%s/%s".format(ignoredDir, file.getName)
      EventHandler.debug(this, "Ignored email file: %s, moving to: %s".format(file.getName, moveto))
      file.renameTo(new File(moveto))
    }
    case FileFailed(file : File, e : Exception) => {
      val moveto = "%s/%s".format(failedDir, file.getName)
      val exception_msg = "Failed to process email file: %s, moving to: %s\nException info: %s\n\n%s".format(
        file.getName, moveto, e.getMessage, e.getStackTraceString
      )
      EventHandler.error(this, exception_msg)
      file.renameTo(new File(moveto))
      val exception_file = new File("%s/%s.exception".format(failedDir, file.getName))
      printToFile(exception_file) {
        p => p.write(exception_msg)
      }

    }
    case _ => EventHandler.error(this, "Unknown message sent to FileHandler actor")
  }

}