#!/bin/sh
java \
-Dmailproc.config=/Users/atistler/mailproc/config/mailproc.properties \
-Ddatabase.config=/Users/atistler/mailproc/config/database.properties \
-Dakka.config=/Users/atistler/mailproc/config/akka.conf \
-Dorbroker.sql=/Users/atistler/mailproc/sql/ \
-jar /Users/atistler/mailproc/target/scala-2.9.1/mailproc_2.9.1-1.0-one-jar.jar
