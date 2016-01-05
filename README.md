# activemq-integration

This is a tool that measures the age of messages in ActiveMQ queues, and publishes the results to SignalFx. These metrics are different from the metrics that ActiveMQ reports to JMX: this tool inspects the messages that are waiting to be delivered in each queue, whereas ActiveMQ reports to JMX the ages of only those messages that have been successfully delivered. For this reason, this tool is especially useful for detecting messages that are "stuck" in ActiveMQ queues and unable to be delivered.

## Metrics Reported

- message.age.average (type:gauge): Average age of messages in each queue, in milliseconds.
- message.age.maximum (type:gauge): Maximum age of messages in each queue, in milliseconds.

## Requirements

- Linux/Unix
- Java 1.5 or higher
- ActiveMQ
- Maven

## Installation

- clone repo (`git clone https://github.com/signalfx/activemq-integration.git`)
- Configure the tool by modifying `<repo>/amq-message-age/properties` (see below)
- `cd <repo>/amq-message-age`
- `./run.sh`

## Configuration

Modify the `properties` file to provide values that make sense for your ActiveMQ instance. 

| property name | definition |
| ------------- | ---------- |
| path | Filesystem path to ActiveMQ executable | 
| token | SignalFx API token |
| sfx_host | Host to which to transmit data (default: `https://ingest.signalfx.com`) |
| interval | Interval at which to measure message age, in milliseconds. (default: `3000`) |
| host | URL at which to connect to ActiveMQ broker. (default: `tcp://localhost:61616`) |
| host_name | Name of this ActiveMQ host. This value appears in the dimension "host" in SignalFx. |
| broker_name | Name of this ActiveMQ broker. This value appears in the dimension "broker" in SignalFx. |

## License 

 This tool is licensed under the Apache License, Version 2.0. See LICENSE for full license text.
