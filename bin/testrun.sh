#!/bin/sh

APP_HOME=/usr/local/mailproc

java \
-Dmailproc.config=$APP_HOME/config/test-mailproc.properties \
-Ddatabase.config=$APP_HOME/config/database.properties \
-Dakka.config=$APP_HOME/config/akka.conf \
-Dorbroker.sql=$APP_HOME/sql/ \
-jar $APP_HOME/target/scala-2.9.1/mailproc_2.9.1-0.1-one-jar.jar
