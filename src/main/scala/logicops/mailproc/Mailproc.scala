package logicops.mailproc

import akka.config.Supervision._
import akka.actor._
import akka.event.EventHandler
import java.util.concurrent.TimeUnit
import java.lang.Thread
import collection.script.Start

object MailProc extends App {

  private val emailBucketSize = PROPS.getProperty("email-bucket-size", "100").toInt
  private val emailProcessDelay = PROPS.getProperty("email-process-delay", "20").toInt
  private val userReloadInterval = PROPS.getProperty("user-reload-interval", "10").toInt


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
      Thread.sleep(500)
      sys.runtime.halt(0)
    }
  })

  val starter = supervisor.start

  /* Reload user email cache every X minutes */
  val userCheckScheduler = Scheduler.schedule(userCheck, Reload(), userReloadInterval, userReloadInterval, TimeUnit.MINUTES)

  val directoryWatcherScheduler = Scheduler.schedule(directoryWatcher, ProcessFiles(emailBucketSize), 0, emailProcessDelay, TimeUnit.SECONDS)
}
