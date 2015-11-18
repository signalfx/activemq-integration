/**
* Copyright (C) 2015 SignalFx, Inc.
*/

package com.signalfx.amq;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import org.apache.activemq.ActiveMQConnection;
import org.apache.activemq.ActiveMQConnectionFactory;
import org.apache.activemq.command.ActiveMQQueue;

import com.signalfx.endpoint.SignalFxEndpoint;
import com.signalfx.endpoint.SignalFxReceiverEndpoint;
import com.signalfx.metrics.auth.StaticAuthToken;
import com.signalfx.metrics.connection.HttpDataPointProtobufReceiverFactory;
import com.signalfx.metrics.errorhandler.OnSendErrorHandler;
import com.signalfx.metrics.flush.AggregateMetricSender;
import com.signalfx.metrics.protobuf.SignalFxProtocolBuffers;

import javax.jms.JMSException;
import javax.jms.Session;
 
public class CollectMessageAge {
    
    private static final String PLUGIN = "amq.message.age";
	
    public static void main(String[] args) throws Exception {
    	
    	Properties prop = new Properties();
        prop.load(new FileInputStream("properties"));
        final String path = prop.getProperty("path", "activemq");
        final String token = prop.getProperty("token");
        final String sfxHost = prop.getProperty("sfx_host");
        final long interval = Long.valueOf(prop.getProperty("interval", "5000"));
        final String host = prop.getProperty("host", "tcp://localhost:61616");
        final String hostName = prop.getProperty("host_name", "Host1");
        final String brokerName = prop.getProperty("broker_name", "Broker1");
        
        final URL hostUrl = new URL(sfxHost);
        
        SignalFxReceiverEndpoint endpoint = new SignalFxEndpoint(hostUrl.getProtocol(), hostUrl.getHost(), hostUrl.getPort());
        
        AggregateMetricSender mf = new AggregateMetricSender(
        		"amq.message.age",
        		new HttpDataPointProtobufReceiverFactory(endpoint).setVersion(2),
        		new StaticAuthToken(token),
                Collections.<OnSendErrorHandler>emptyList()
        		);

        while (true) {

            Thread.sleep(interval);
            AggregateMetricSender.Session i = mf.createSession();
            
            List<MessageAge> data = collect(path, host);
            
            try {

				for (MessageAge messageAge : data) {

					System.out.println("Sending data:" + messageAge.toString());
					
					i.setDatapoint(SignalFxProtocolBuffers.DataPoint
							.newBuilder()
							.setMetric("message.age.average")
							.setValue(
									SignalFxProtocolBuffers.Datum.newBuilder()
											.setIntValue(messageAge.average))
							.addDimensions(
                                    SignalFxProtocolBuffers.Dimension
                                            .newBuilder().setKey("plugin")
                                            .setValue(PLUGIN))
							.addDimensions(
									SignalFxProtocolBuffers.Dimension
											.newBuilder().setKey("host")
											.setValue(hostName))
							.addDimensions(
                                    SignalFxProtocolBuffers.Dimension
                                            .newBuilder().setKey("broker")
                                            .setValue(brokerName))
							.addDimensions(
									SignalFxProtocolBuffers.Dimension
											.newBuilder().setKey("queue")
											.setValue(messageAge.queue))
							.build());
					
					i.setDatapoint(SignalFxProtocolBuffers.DataPoint
							.newBuilder()
							.setMetric("message.age.maximum")
							.setValue(
									SignalFxProtocolBuffers.Datum.newBuilder()
											.setIntValue(messageAge.maximum))
							.addDimensions(
                                    SignalFxProtocolBuffers.Dimension
                                            .newBuilder().setKey("plugin")
                                            .setValue(PLUGIN))
							.addDimensions(
									SignalFxProtocolBuffers.Dimension
											.newBuilder().setKey("host")
											.setValue(hostName))
							.addDimensions(
                                    SignalFxProtocolBuffers.Dimension
                                            .newBuilder().setKey("broker")
                                            .setValue(brokerName))
							.addDimensions(
									SignalFxProtocolBuffers.Dimension
											.newBuilder().setKey("queue")
											.setValue(messageAge.queue))
							.build());

				}
                
            } finally {
                i.close();
            }

        }
        
    }
    
    private static class MessageAge{
    	
    	public final String queue;
    	public final long average;
    	public final long maximum;
    	
    	public MessageAge(String queue, long average, long maximum){
    		this.queue = queue;
    		this.average = average;
    		this.maximum = maximum;
    	}
    	
    	@Override
    	public String toString(){
    		return "{queue:\"" + queue + "\", average:" + String.valueOf(average) + ", maximum:" + String.valueOf(maximum) + "}";
    	}
    	
    }
    
	public static List<MessageAge> collect(String path, String host) {

		List<MessageAge> result = new LinkedList<MessageAge>();
		
		try {

			ActiveMQConnectionFactory out = new ActiveMQConnectionFactory(host + "?jms.prefetchPolicy.all=1000");
			ActiveMQConnection connection;
			connection = (ActiveMQConnection) out.createConnection();

			connection.start();
			Session session = connection.createSession(false,Session.AUTO_ACKNOWLEDGE);

			Set<ActiveMQQueue> amqs = connection.getDestinationSource().getQueues();
			Iterator<ActiveMQQueue> queues = amqs.iterator();
			
			while (queues.hasNext()) {

				ActiveMQQueue queue = queues.next();
				String queueName = queue.getPhysicalName();
				
				MessageAge age = collectQueue(path, host, queueName);
				if(age != null){
				    result.add(age);
				}

				
			}
			
			session.close();
			connection.close();

		} catch (JMSException e) {
			e.printStackTrace();
		}

		return result;

	}
	
	public static MessageAge collectQueue(String path, String host, String queue) {
        
        BufferedReader br = null;
        
        try {

            Process p = Runtime.getRuntime().exec(path + " browse --amqurl " + host + " " + queue);

            br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        
            String line;
            Long now = System.currentTimeMillis();
            long max = 0;
            long sum = 0;
            long num = 0;
            while ((line = br.readLine()) != null) {
                
                if(line.contains("JMS_HEADER_FIELD:JMSTimestamp = ")){
                    String valueString = line.split(" = ")[1];
                    
                    long age = now - Long.valueOf(valueString);
                    
                    if(age > max){
                        max = age;
                    }
                    
                    sum = sum + age;
                    num++;
                }

            }
            
            long average = 0;
            if(num > 0){
                average = sum / num;
            }
            
            return new MessageAge(queue, average, max);
            
        } catch (Exception e) {
            e.printStackTrace();
        } finally{
            if(br != null){
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        
        return null;
        
    }
    
}