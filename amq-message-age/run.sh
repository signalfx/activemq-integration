#!/bin/bash

mvn clean
mvn install
mvn exec:java -Dexec.mainClass="com.signalfx.amq.CollectMessageAge"