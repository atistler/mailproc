package logicops.mailproc

import akka.config.Supervision._
import akka.actor._
import akka.event.EventHandler
import java.util.concurrent.TimeUnit
import java.lang.Thread
import collection.script.Start

object MailProc extends App {

  private[mailproc] val supervisor = Supervisor(
    SupervisorConfig(
      OneForOneStrategy(List(classOf[Exception]), 3, 5000),
      List(
        Supervise(userCheck, Permanent),
        Supervise(emailParser, Permanent),
        Supervise(ticketHandler, Permanent),
        Supervise(fileHandler, Permanent),
        Supervise(directoryWatcher, Permanent),
        Supervise(mailproc, Permanent)
      )
    )
  )

  sys.runtime.addShutdownHook(new Thread() {
    override def run() {
      EventHandler.info(this, "shutting down supervisor")
      supervisor.shutdown()

      EventHandler.info(this, "shutting down all remaining actors via registry")
      Actor.registry.shutdownAll()
      sys.exit()
    }
  })

  supervisor.start

  /* Reload user email cache every X minutes */
  Scheduler.schedule(userCheck, Reload(), 5, 5, TimeUnit.MINUTES)

  EventHandler.info(this, "Sending StartWatch() message to MailProc actor")

  mailproc ! StartWatch()


}

class MailProc extends Actor {
  val numFiles = PROPS.getProperty("max-process-emails", "100").toInt
  val waitInterval = PROPS.getProperty("process-sleep", "20000").toLong

  def receive = {
    case StartWatch() => {
       directoryWatcher ! ProcessFiles(numFiles)
    }
    case DoneProcessingFiles() => {
      EventHandler.info(this, "Sleeping for %dms".format(waitInterval))
      Thread.sleep(waitInterval)
      directoryWatcher ! ProcessFiles(numFiles)
    }
    case _ => EventHandler.error(this, "Unknown message sent to MailProc actor")
  }
}
