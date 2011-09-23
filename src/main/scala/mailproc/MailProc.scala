package mailproc {

import akka.config.Supervision._
import akka.actor._
import akka.event.EventHandler
import java.util.concurrent.TimeUnit


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
  supervisor.start

  /* Reload user email cache every X minutes */
  Scheduler.schedule(userCheck, Reload(), 1, 1, TimeUnit.MINUTES)

  EventHandler.info(this, "Sending StartWatch() message to directoryWatcher")
  directoryWatcher ! StartWatch()

  Thread.sleep(5000)

  /*
  class ShutdownHandler extends sun.misc.SignalHandler {
    def handle(sig : sun.misc.Signal) {
      println("Shutting down")
      registry.shutdownAll()
    }
  }

  sun.misc.Signal.handle(new Signal("TERM"), new ShutdownHandler)
  sun.misc.Signal.handle(new Signal("INT"), new ShutdownHandler)
  */
}
}