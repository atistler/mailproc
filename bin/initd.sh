#!/bin/bash
#
# chkconfig: 345 99 05
# description: mailproc daemon script
#
#

#APP_HOME=/usr/local/mailproc
#java \
#-Dmailproc.config=$APP_HOME/config/test-mailproc.properties \
#-Ddatabase.config=$APP_HOME/config/database.properties \
#-Dakka.config=$APP_HOME/config/akka.conf \
#-Dorbroker.sql=$APP_HOME/sql/ \
#-jar $APP_HOME/target/scala-2.9.1/mailproc_2.9.1-0.1-one-jar.jar

JAVA_HOME=/usr
SERVICE="mailproc"                                  # service name with the first letter in lowercase
SERVICE_NAME="MailProc"                                    # service name
SERVICE_USER="root"                                      # OS user name for the service
SERVICE_GROUP="root"                                    # OS group name for the service
APP_DIR="/usr/local/$SERVICE"                          # home directory of the service application
SERVICE_HOME="/home/$SERVICE_USER"                       # home directory of the service user
SERVICE_LOG_FILE="/var/log/$SERVICE.log"               # log file for StdOut/StdErr
MAX_SHUTDOWN_TIME=15                                         # maximum number of seconds to wait for the daemon to terminate normally
PID_FILE="/var/run/$SERVICE.pid"                      # name of PID file (PID = process ID number)
JAVA_COMMAND="java"                                         # name of the Java launcher without the path
JAVA_EXE="$JAVA_HOME/bin/$JAVA_COMMAND"                      # file name of the Java application launcher executable
JAVA_ARGS="-Dmailproc.config=$APP_DIR/config/test-mailproc.properties \
           -Ddatabase.config=$APP_DIR/config/database.properties \
           -Dakka.config=$APP_DIR/config/akka.conf \
           -Dorbroker.sql=$APP_DIR/sql/ \
           -jar $APP_DIR/bin/mailproc.jar"                     # arguments for Java launcher
JAVA_COMMANDLINE="$JAVA_EXE $JAVA_ARGS"                       # command line to start the Java service application
JAVA_COMMANDLINEKEYWORD="mailproc.jar"                     # a keyword that occurs on the commandline, used to detect an already running service process and to distinguish it from others
 
# Makes the file $1 writable by the group $SERVICE_GROUP.
function makeFileWritable {
   local filename="$1"
   touch $filename || return 1
   chgrp $SERVICE_GROUP $filename || return 1
   chmod g+w $filename || return 1
   return 0; }
 
# Returns 0 if the process with PID $1 is running.
function checkProcessIsRunning {
   local pid="$1"
   if [ -z "$pid" -o "$pid" == " " ]; then return 1; fi
   if [ ! -e /proc/$pid ]; then return 1; fi
   return 0; }
 
# Returns 0 if the process with PID $1 is our Java service process.
function checkProcessIsOurService {
   local pid="$1"
   if [ "$(ps -p $pid --no-headers -o comm)" != "$JAVA_COMMAND" ]; then return 1; fi
   grep -q --binary -F "$JAVA_COMMANDLINEKEYWORD" /proc/$pid/cmdline
   if [ $? -ne 0 ]; then return 1; fi
   return 0; }
 
# Returns 0 when the service is running and sets the variable $pid to the PID.
function getServicePID {
   if [ ! -f $PID_FILE ]; then return 1; fi
   pid="$(<$PID_FILE)"
   checkProcessIsRunning $pid || return 1
   checkProcessIsOurService $pid || return 1
   return 0; }
 
function startServiceProcess {
   cd $APP_DIR || return 1
   rm -f $PID_FILE
   makeFileWritable $PID_FILE || return 1
   makeFileWritable $SERVICE_LOG_FILE || return 1
   cmd="nohup $JAVA_COMMANDLINE >>$SERVICE_LOG_FILE 2>&1 & echo \$! >$PID_FILE"
   su -m $SERVICE_USER -s $SHELL -c "$cmd" || return 1
   sleep 0.1
   pid="$(<$PID_FILE)"
   if checkProcessIsRunning $pid; then :; else
      echo -ne "\n$SERVICE_NAME start failed, see logfile."
      return 1
   fi
   return 0; }
 
function stopServiceProcess {
   kill $pid || return 1
   for ((i=0; i<$MAX_SHUTDOWN_TIME*10; i++)); do
      checkProcessIsRunning $pid
      if [ $? -ne 0 ]; then
         rm -f $PID_FILE
         return 0
         fi
      sleep 0.1
      done
   echo -e "\n$SERVICE_NAME did not terminate within $MAX_SHUTDOWN_TIME seconds, sending SIGKILL..."
   kill -s KILL $pid || return 1
   local killWaitTime=15
   for ((i=0; i<killWaitTime*10; i++)); do
      checkProcessIsRunning $pid
      if [ $? -ne 0 ]; then
         rm -f $PID_FILE
         return 0
         fi
      sleep 0.1
      done
   echo "Error: $SERVICE_NAME could not be stopped within $MAX_SHUTDOWN_TIME+$killWaitTime seconds!"
   return 1; }
 
function startService {
   getServicePID
   if [ $? -eq 0 ]; then echo -n "$SERVICE_NAME is already running"; RETVAL=0; return 0; fi
   echo -n "Starting $SERVICE_NAME   "
   startServiceProcess
   if [ $? -ne 0 ]; then RETVAL=1; echo "failed"; return 1; fi
   echo "started PID=$pid"
   RETVAL=0
   return 0; }
 
function stopService {
   getServicePID
   if [ $? -ne 0 ]; then echo -n "$SERVICE_NAME is not running"; RETVAL=0; echo ""; return 0; fi
   echo -n "Stopping $SERVICE_NAME   "
   stopServiceProcess
   if [ $? -ne 0 ]; then RETVAL=1; echo "failed"; return 1; fi
   echo "stopped PID=$pid"
   RETVAL=0
   return 0; }
 
function checkServiceStatus {
   echo -n "Checking for $SERVICE_NAME:   "
   if getServicePID; then
    echo "running PID=$pid"
    RETVAL=0
   else
    echo "stopped"
    RETVAL=3
   fi
   return 0; }
 
function main {
   RETVAL=0
   case "$1" in
      start)                                               # starts the Java program as a Linux service
         startService
         ;;
      stop)                                                # stops the Java program service
         stopService
         ;;
      restart)                                             # stops and restarts the service
         stopService && startService
         ;;
      status)                                              # displays the service status
         checkServiceStatus
         ;;
      *)
         echo "Usage: $0 {start|stop|restart|status}"
         exit 1
         ;;
      esac
   exit $RETVAL
}
 
main $1
