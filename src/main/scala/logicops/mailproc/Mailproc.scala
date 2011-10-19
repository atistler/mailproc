package logicops.mailproc

import akka.config.Supervision._
import akka.actor._
import akka.event.EventHandler
import java.util.concurrent.TimeUnit
import java.lang.Thread
import logicops.utils._
import akka.actor.Actor._
import java.io.{PrintWriter, File}

object MailProc extends App {

  private val mailproc_conf = sys.props.get("mailproc.config") match {
    case Some(path) => {
      val file = new File(path)
      if (file == null || !file.isFile) {
        sys.error("System property mailproc.config points to invalid path: '%s'".format(path))
        sys.exit(1)
      } else {
        file
      }
    }
    case None => {
      sys.error("System property mailproc.config must point to the mailproc.properties configuration file")
      sys.exit(1)
    }
  }

  val PROPS = loadConfig(mailproc_conf)



  lazy val isProd = PROPS.getProperty("mode") == "prod"
  lazy val isTest = PROPS.getProperty("mode") == "test"
  lazy val isDev = PROPS.getProperty("mode") == "dev" || !isProd || !isTest

  private[mailproc] val maildir_directory = PROPS.getProperty("maildir-directory")
  private[mailproc] val support_addresses = PROPS.getProperty("support-addresses").split(",").map(_.trim()).toSet
  private[mailproc] val internal_addresses = PROPS.getProperty("internal-addresses").split(",").map(_.trim()).toSet
  private[mailproc] val userCheck = actorOf(new UserCheck)
  private[mailproc] val emailParser = actorOf(new EmailParser(support_addresses, internal_addresses))
  private[mailproc] val fileHandler = actorOf(new FileHandler(maildir_directory))
  private[mailproc] val ticketHandler = actorOf(new TicketHandler)
  private[mailproc] val emailSender = actorOf(new EmailSender)
  private[mailproc] val directoryWatcher = actorOf(new DirectoryWatcher(maildir_directory))

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
        Supervise(emailSender, Permanent),
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

  val mode = if ( isDev ) "Development" else if ( isTest ) "Test" else "Production"
  EventHandler.info(this, "Starting Mailproc in %s mode".format(mode))

  val starter = supervisor.start

  /* Reload user email cache every X minutes */
  val userCheckScheduler = Scheduler.schedule(userCheck, Reload(), userReloadInterval, userReloadInterval, TimeUnit.MINUTES)

  val directoryWatcherScheduler = Scheduler.schedule(directoryWatcher, ProcessFiles(emailBucketSize), 0, emailProcessDelay, TimeUnit.SECONDS)
}
