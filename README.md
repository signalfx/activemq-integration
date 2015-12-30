# activemq-integration

Query activemq for message age metrics and publish them

## What is this plugin for

This plugin is for reporting messages age in the ActiveMQ-queues.
It reports two metrics:

- message.age.average - (type:gauge, value in millis) average message age in each queue

- message.age.maximum - (type:gauge, value in millis) maximum message age in each queue

## Requirements

- Linux/Unix
- Java 1.5 or higher
- ActiveMQ
- Maven

## Installation

- clone repo (`git clone https://github.com/signalfx/activemq-integration.git`)
- Edit `<repo>/amq-message-age/properties` file to config plugin
- `cd <repo>/amq-message-age`
- `./run.sh`

## "properties" file

- path - path to ActiveMQ executable
- token - SignalFX-API-TOKEN
- sfx_host - let it be `https://ingest.signalfx.com` - host to send data to
- interval - interval how often to query on ActiveMQ messages age
- host - activemq host
- host_name - Name of a host in "host" dimension
- broker_name - Name of a broker in "broker" dimension
