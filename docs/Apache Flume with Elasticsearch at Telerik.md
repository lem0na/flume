Aim of this document is to decsribe how to use Apache Flume with Elasticsearch (ES) sink.

There is official Apche Flume sink that sends data to ES. However it is designed to mimic Logstash behaviour. It is intended work with plain text data. Any additional processing and field mapping can be done with interceptors.

As version 1.5.0.1 this scenario does not fit in our requirements where we want to log json message without any further processing. Other drawback is that index name and index type are set in config file and cannot be changed dynamically. 

To overcome this issue we used latest sources where index name and index type can be dynamically set. We created custom serializer that can handle json payload. 

Source code is hosted on Telerik's Gitlab server and can be found here: https://gitlab.telerik.com/foundation-services/flumr-ng/commits/es-fume

Example how log event message should look like:

    [{
      "headers": {
        "indexName" : "delme", "indexType" : "logentry"
      },
      "body": "{\"Metadata\": [\"0\",\"Information\",\"PLATFORM-PL1\",\"Platform.CPBC.Container.exe\",\"5584\",\"Platform.CPBC.Container\",null,\"2244\"],\"Categories\": [\"PLATFORM-PL1.platform.cpbc.bl.xxxx\"],\"ExtendedProperties\": {\"Controller.PipelineName\":\"Messaging_ProcessPerformCalculationMessages\"},\"Message\": \"We are simultaneously running bulk operations on the two documents and their respective indices\",\"Timestamp\":\"2013-10-04T13:41:45.5523494Z\"}"    
    }]

It is standard Apache Flume message that consists from two parts - headers and body.
Headers is used to send additional information that will be used to notify about index name and index type.
Body contains string presentation of the log entry.

Example of Apache Flume configuration file that uses the new serializer:

    agent1.souces = r1
    agent1.channels = ch1
    agent1.sinks = k1
    
    #sources
    agent1.sources.r1.type = avro
    agent1.sources.r1.port = 10000
    agent1.sources.r1.bind = 0.0.0.0
    agent1.sources.r1.channels = ch1
    
    #channels
    agent1.channels.ch1.type = memory
    agent1.channels.ch1.capacity = 100000
    agent1.channels.ch1.transactionCapacity = 10000
    
    #sinks
    agent1.sinks = elasticsearch
    agent1.sinks.elasticsearch.channel = ch1
    agent1.sinks.elasticsearch.type= org.apache.flume.sink.elasticsearch.ElasticSearchSink
    agent1.sinks.elasticsearch.batchSize = 100
    agent1.sinks.elasticsearch.hostNames = 172.30.50.89:9300
    agent1.sinks.elasticsearch.clusterName = cloudfs
    agent1.sinks.elasticsearch.serializer = org.apache.flume.sink.elasticsearch.ElasticSearchJsonSerializer
    agent1.sinks.elasticsearch.indexName = %{indexName}
    agent1.sinks.elasticsearch.indexType = %{indexType}

Config file is straiforward. Most interesting are last 3 lines:

 - *agent1.sinks.elasticsearch.serializer =
   org.apache.flume.sink.elasticsearch.ElasticSearchJsonSerializer*
   specifies our new custom serializer
 - *agent1.sinks.elasticsearch.indexName = %{indexName}* specifies template that will be applied to the fields from headerd to set dinamicly index name 
 - *agent1.sinks.elasticsearch.indexType = %{indexType}* specifies template that will be applied to the fields from headerds to set dynamically index type 

If this templates are not specified or did not match to a fields from headers section of the payload if will be used default values. For index name it is "flume" and for index type it is "log"

Since Apache Flume nodes comunicate using avro protocol it is possible to have Apache Flume agent version 1.3 (used in our current disk images) that comunicates with Apache Flume concentrator build from this sources.

In ES above example message will be stored like this:
    {
        "body": {
          "Message": "We are simultaneously running bulk operations on the two documents and their respective indices",
          "Metadata": [
            "0",
            "Information",
            "PLATFORM-PL1",
            "Platform.CPBC.Container.exe",
            "5584",
            "Platform.CPBC.Container",
            null,
            "2244"
          ],
          "ExtendedProperties": {
            "Controller.PipelineName": "Messaging_ProcessPerformCalculationMessages"
          },
          "Timestamp": "2013-10-04T13:41:45.5523494Z",
          "Categories": [
            "PLATFORM-PL1.platform.cpbc.bl.xxxx"
          ]
        },
        "host": "localhost"
    }

When comparing with messages stored when using Apache Flume RabbitMQ sink + RabbitMQ +  Elasticsearch RabbitMQ river there is an additional field "body" that contains the log entry
