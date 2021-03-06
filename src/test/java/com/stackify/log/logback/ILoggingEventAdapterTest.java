/*
 * Copyright 2014 Stackify
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.stackify.log.logback;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.ThrowableProxy;

import com.stackify.api.EnvironmentDetail;
import com.stackify.api.LogMsg;
import com.stackify.api.StackifyError;
import com.stackify.api.WebRequestDetail;
import com.stackify.api.common.log.ServletLogContext;

/**
 * ILoggingEventAdapter JUnit Test
 * @author Eric Martin
 */
public class ILoggingEventAdapterTest {

	/**
	 * testGetThrowable
	 */
	@Test
	public void testGetThrowable() {
		ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
		
		ILoggingEventAdapter adapter = new ILoggingEventAdapter(Mockito.mock(EnvironmentDetail.class));
		Throwable throwable = adapter.getThrowable(event);
		
		Assert.assertNull(throwable);
	}
	
	/**
	 * testGetThrowableWithoutException
	 */
	@Test
	public void testGetThrowableWithoutException() {
		ThrowableProxy proxy = Mockito.mock(ThrowableProxy.class);
		Mockito.when(proxy.getThrowable()).thenReturn(new NullPointerException());
		
		ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
		Mockito.when(event.getThrowableProxy()).thenReturn(proxy);
		
		ILoggingEventAdapter adapter = new ILoggingEventAdapter(Mockito.mock(EnvironmentDetail.class));
		Throwable throwable = adapter.getThrowable(event);
		
		Assert.assertNotNull(throwable);
	}
	
	/**
	 * testGetLogMsg
	 */
	@Test
	public void testGetLogMsg() {
		String msg = "msg";
		StackifyError ex = Mockito.mock(StackifyError.class);
		String th = "th";
		String level = "debug";
		String srcClass = "srcClass";
		String srcMethod = "srcMethod";
		Integer srcLine = Integer.valueOf(14);
		
		Map<String, String> properties = new HashMap<String, String>();
		properties.put("key", "value");

		StackTraceElement ste = new StackTraceElement(srcClass, srcMethod, "", srcLine);
		
		ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
		Mockito.when(event.getFormattedMessage()).thenReturn(msg);
		Mockito.when(event.getThreadName()).thenReturn(th);
		Mockito.when(event.getLevel()).thenReturn(Level.DEBUG);
		Mockito.when(event.getCallerData()).thenReturn(new StackTraceElement[]{ste});
		Mockito.when(event.getMDCPropertyMap()).thenReturn(properties);

		ILoggingEventAdapter adapter = new ILoggingEventAdapter(Mockito.mock(EnvironmentDetail.class));
		LogMsg logMsg = adapter.getLogMsg(event, ex);
		
		Assert.assertNotNull(logMsg);
		Assert.assertEquals(msg, logMsg.getMsg());
		Assert.assertEquals("{\"key\":\"value\"}", logMsg.getData());
		Assert.assertEquals(ex, logMsg.getEx());		
		Assert.assertEquals(th, logMsg.getTh());		
		Assert.assertEquals(level, logMsg.getLevel());			
		Assert.assertEquals(srcClass + "." + srcMethod, logMsg.getSrcMethod());		
		Assert.assertEquals(srcLine, logMsg.getSrcLine());		
	}
	
	/**
	 * testGetStackifyError
	 */
	@Test
	public void testGetStackifyError() {
		ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
		Mockito.when(event.getFormattedMessage()).thenReturn("Exception message");
		
		Throwable exception = Mockito.mock(Throwable.class);
		
		ILoggingEventAdapter adapter = new ILoggingEventAdapter(Mockito.mock(EnvironmentDetail.class));
		StackifyError error = adapter.getStackifyError(event, exception);
		
		Assert.assertNotNull(error);
	}
	
	/**
	 * testGetStackifyErrorServletContext
	 */
	@Test
	public void testGetStackifyErrorServletContext() {
		String user = "user";
		ServletLogContext.putUser(user);
		
		WebRequestDetail webRequest = WebRequestDetail.newBuilder().build();
		ServletLogContext.putWebRequest(webRequest);
		
		ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
		Mockito.when(event.getMessage()).thenReturn("Exception message");
		
		Throwable exception = Mockito.mock(Throwable.class);
		
		ILoggingEventAdapter adapter = new ILoggingEventAdapter(Mockito.mock(EnvironmentDetail.class));
		StackifyError error = adapter.getStackifyError(event, exception);
		
		Assert.assertNotNull(error);
		
		Assert.assertEquals(user, error.getUserName());
		Assert.assertNotNull(error.getWebRequestDetail());
	}
	
	/**
	 * testGetLogMsgServletContext
	 */
	@Test
	public void testGetLogMsgServletContext() {
		String transactionId = UUID.randomUUID().toString();
		ServletLogContext.putTransactionId(transactionId);
		
		ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
		Mockito.when(event.getLevel()).thenReturn(Level.DEBUG);

		ILoggingEventAdapter adapter = new ILoggingEventAdapter(Mockito.mock(EnvironmentDetail.class));
		LogMsg logMsg = adapter.getLogMsg(event, null);
		
		Assert.assertNotNull(logMsg);
		Assert.assertEquals(transactionId, logMsg.getTransId());
	}
	
	/**
	 * testIsErrorLevel
	 */
	@Test
	public void testIsErrorLevel() {
		ILoggingEvent debug = Mockito.mock(ILoggingEvent.class);
		Mockito.when(debug.getLevel()).thenReturn(Level.DEBUG);

		ILoggingEvent error = Mockito.mock(ILoggingEvent.class);
		Mockito.when(error.getLevel()).thenReturn(Level.ERROR);
		
		ILoggingEventAdapter adapter = new ILoggingEventAdapter(Mockito.mock(EnvironmentDetail.class));

		Assert.assertFalse(adapter.isErrorLevel(debug));
		Assert.assertTrue(adapter.isErrorLevel(error));
	}
	
	/**
	 * testGetStackifyErrorWithoutException
	 */
	@Test
	public void testGetStackifyErrorWithoutException() {
		StackTraceElement ste = new StackTraceElement("class", "method", "file", 123);
		
		ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
		Mockito.when(event.getLevel()).thenReturn(Level.ERROR);
		Mockito.when(event.getFormattedMessage()).thenReturn("Exception message");
		Mockito.when(event.getCallerData()).thenReturn(new StackTraceElement[]{ste});
		
		ILoggingEventAdapter adapter = new ILoggingEventAdapter(Mockito.mock(EnvironmentDetail.class));
		StackifyError error = adapter.getStackifyError(event, null);
		
		Assert.assertNotNull(error);
		Assert.assertEquals("StringException", error.getError().getErrorType());
	}
	
	/**
	 * testGetClassName
	 */
	@Test
	public void testGetClassName() {
		StackTraceElement ste = new StackTraceElement("class", "method", "file", 123);
		
		ILoggingEvent event = Mockito.mock(ILoggingEvent.class);
		Mockito.when(event.getCallerData()).thenReturn(new StackTraceElement[]{ste});
		
		ILoggingEventAdapter adapter = new ILoggingEventAdapter(Mockito.mock(EnvironmentDetail.class));

		String className = adapter.getClassName(event);
		
		Assert.assertNotNull(className);
		Assert.assertEquals("class", className);
	}
}
