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
        Supervise(directoryWatcher, Permanent)
      )
    )
  )

  sys.runtime.addShutdownHook(new Thread() {
    override def run() {
      EventHandler.info(this, "shutting down schedulers")
      userCheckScheduler.cancel(true)
      directoryWatcherScheduler.cancel(true)
      EventHandler.info(this, "shutting down supervisor")
      supervisor.shutdown()

      EventHandler.info(this, "shutting down all remaining actors via registry")
      Actor.registry.shutdownAll()
      Actor.registry.actors.foreach(a => println("Actor %s is shutdown: %s".format(a, a.isShutdown)))
      sys.runtime.exit(0)
    }
  })

  val starter = supervisor.start

  /* Reload user email cache every X minutes */
  val userCheckScheduler = Scheduler.schedule(userCheck, Reload(), 5, 5, TimeUnit.MINUTES)

  private val numFiles = PROPS.getProperty("max-process-emails", "100").toInt
  private val waitInterval = PROPS.getProperty("process-sleep", "20").toInt

  val directoryWatcherScheduler = Scheduler.schedule(directoryWatcher, ProcessFiles(numFiles), 0, waitInterval, TimeUnit.MINUTES)
}
