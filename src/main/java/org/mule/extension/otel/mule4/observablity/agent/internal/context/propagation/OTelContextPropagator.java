package org.mule.extension.otel.mule4.observablity.agent.internal.context.propagation;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import org.mule.extension.otel.mule4.observablity.agent.internal.connection.OtelSdkConnection;
import org.mule.extension.otel.mule4.observablity.agent.internal.notification.OTelMuleNotificationHandler;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.Constants;

import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapSetter;
import io.opentelemetry.context.propagation.TextMapPropagator;

import org.mule.runtime.api.connection.ConnectionException;

/**
 * 
 * Helper class to support trace Context propagation
 *
 * @see #extract(Object, TextMapGetter)
 * @see #inject(Object, TextMapSetter)
 * @see #makeTraceContextMapFor(String)
 */
public class OTelContextPropagator
{	
	private static TextMapPropagator getTextMapPropagator() throws ConnectionException
	{
		Supplier<ConnectionException> s = () -> new ConnectionException("Unable to get the TextMapPropagator. OTel SDK is not initialized. ");
		
		OtelSdkConnection osc = OtelSdkConnection.get().orElseThrow(s);
		
		return osc.getTextMapPropagator().get();
	}

	/**
	 * 
	 * Extracts data from the upstream carrier (e.g., from the incoming HTTP headers) and combines
	 * it with the current Context. The returned Context should contain the aggregated data.
	 * 
	 * @param <T>
	 * @param carrier - container which holds the propagation keys to be exracted.
	 * @param textMapGetter - invoked for each propagation key to be retrieved
	 * @return Context - Merged data from the carrier and the current Context
	 * @throws ConnectionException
	 */
	public static <T> Context extract(T carrier, TextMapGetter<T> textMapGetter) throws ConnectionException
	{
		return getTextMapPropagator().extract(Context.current(), carrier, textMapGetter);
	}

	/**
	 * Injects data from the current Context into the carrier (e.g., the outgoing HTTP headers)
	 * be used by downstream consumers.
	 * @param <T>
	 * @param carrier - container into which the current Context will be injected.
	 * @param textMapSetter - invoked for each propagation key to be injected.
	 * @throws ConnectionException
	 */
	public static <T> void inject(T carrier, TextMapSetter<T> textMapSetter) throws ConnectionException
	{
		getTextMapPropagator().inject(Context.current(), carrier, textMapSetter);
	}

	/**
	 * Get the Context for a particual trace and store the information into a Map.
	 * @param traceId
	 * @return a simple Map with the Context data for the trace with id <code>traceId</code>
	 * @throws ConnectionException
	 */
	public static Map<String, String> makeTraceContextMapFor(String traceId) throws ConnectionException
	{
		Context traceContext = OTelMuleNotificationHandler.getMuleSoftTraceStore().getTraceContextFor(traceId);
		
		Map<String, String> traceContextMap = new HashMap<>();
		
		traceContextMap.put(Constants.TRACE_TRANSACTION_ID, traceId);
		
		try (Scope scope = traceContext.makeCurrent())
		{
			/*
			 * Inject data from the current Context into a hash map
			 */
			inject(traceContextMap, new SimpleHashMapSetter());
		}
		
		return Collections.unmodifiableMap(traceContextMap);
	}
}
