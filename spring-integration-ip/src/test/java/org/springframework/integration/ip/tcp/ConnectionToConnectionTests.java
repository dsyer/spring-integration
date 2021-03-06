/*
 * Copyright 2002-2011 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ip.tcp;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;

import java.util.Properties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.ip.tcp.connection.AbstractClientConnectionFactory;
import org.springframework.integration.ip.tcp.connection.AbstractServerConnectionFactory;
import org.springframework.integration.ip.tcp.connection.TcpConnection;
import org.springframework.integration.ip.tcp.serializer.ByteArrayRawSerializer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


/**
 * @author Gary Russell
 * @since 2.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ConnectionToConnectionTests {
	
	@Autowired
	AbstractApplicationContext ctx;
	
	@Autowired
	private AbstractClientConnectionFactory client;
	
	@Autowired
	private AbstractServerConnectionFactory server;
	
	@Autowired
	private QueueChannel serverSideChannel;

	// Test jvm shutdown
	public static void main(String[] args) {
		ConfigurableApplicationContext ctx = new ClassPathXmlApplicationContext(
				ConnectionToConnectionTests.class.getPackage().getName()
						.replaceAll("\\.", "/")
						+ "/common-context.xml");
		ctx.close();
		ctx = new ClassPathXmlApplicationContext(
				ConnectionToConnectionTests.class.getPackage().getName()
						.replaceAll("\\.", "/")
						+ "/ConnectionToConnectionTests-context.xml");
		ctx.close();		
	}
	
	@Test
	public void testConnect() throws Exception {
		int n = 0;
		while (!server.isListening()) {
			Thread.sleep(100);
			if (n++ > 100) {
				throw new Exception("Failed to listen");
			}
		}
		client.start();
		for (int i = 0; i < 100; i++) {
			TcpConnection connection = client.getConnection();
			connection.send(MessageBuilder.withPayload("Test").build());
			Message<?> message = serverSideChannel.receive(10000);
			MessageHistory history = MessageHistory.read(message);
			//org.springframework.integration.test.util.TestUtils
			Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "looper", 0);
			assertNotNull(componentHistoryRecord);
			assertTrue(componentHistoryRecord.get("type").equals("ip:tcp-inbound-gateway"));
			assertNotNull(message);
			assertEquals("Test", new String((byte[]) message.getPayload()));
		}
	}

	@Test
	public void testConnectRaw() throws Exception {
		ByteArrayRawSerializer serializer = new ByteArrayRawSerializer();
		client.setSerializer(serializer);
		server.setDeserializer(serializer);
		client.start();
		TcpConnection connection = client.getConnection();
		connection.send(MessageBuilder.withPayload("Test").build());
		Message<?> message = serverSideChannel.receive(10000);
		MessageHistory history = MessageHistory.read(message);
		//org.springframework.integration.test.util.TestUtils
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "looper", 0);
		assertNotNull(componentHistoryRecord);
		assertTrue(componentHistoryRecord.get("type").equals("ip:tcp-inbound-gateway"));
		assertNotNull(message);
		assertEquals("Test", new String((byte[]) message.getPayload()));
	}
	
	@Test
	public void testLookup() throws Exception {
		client.start();
		TcpConnection connection = client.getConnection();
		assertFalse(connection.getConnectionId().contains("localhost"));
		connection.close();
		client.setLookupHost(true);
		connection = client.getConnection();
		assertTrue(connection.getConnectionId().contains("localhost"));
		connection.close();
		client.setLookupHost(false);
		connection = client.getConnection();
		assertFalse(connection.getConnectionId().contains("localhost"));
		connection.close();
	}
	
}
