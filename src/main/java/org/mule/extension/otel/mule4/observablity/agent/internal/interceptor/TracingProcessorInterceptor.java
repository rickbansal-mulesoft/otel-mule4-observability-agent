package org.mule.extension.otel.mule4.observablity.agent.internal.interceptor;

import org.mule.extension.http.api.request.builder.HttpRequesterRequestBuilder;
import org.mule.extension.otel.mule4.observablity.agent.internal.context.propagation.OTelContextPropagator;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.Constants;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.MultiMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

@Component
public class TracingProcessorInterceptor implements ProcessorInterceptor
{
	private static final Logger logger = LoggerFactory.getLogger(TracingProcessorInterceptor.class);

	@Override
	public void before(ComponentLocation location, Map<String, ProcessorParameterValue> parameters, InterceptionEvent event)
	{
		String traceId = event.getCorrelationId();
		
		if (TracingProcessorInterceptorFactory.isFirstProcessor(location))
		{
			try
			{
				event.addVariable(Constants.TRACE_CONTEXT_MAP_KEY, OTelContextPropagator.makeTraceContextMapFor(traceId));
				logger.info("created TRACE_CONTEXT_MAP_KEY variable");
			}
			catch (Exception e)
			{
				logger.info("unable to create TRACE_CONTEXT_MAP_KEY variable");
			}
		}
		else if (isHttpRequestProcessor(location))
		{			
			// ----------------------------------------------------------------------------------------
			// 	Attempting to insert WC3 Trace Context headers into the outgoing HTTP request but it
			//	doesn't seem to be working. The inserted headers display in the RequestBuilder's header
			//	list but they are not being honored in the outgoing HTTP Request.
			// ----------------------------------------------------------------------------------------
			
			/*
			Map<String, String> traceContextMap = TypedValue.unwrap(event.getVariables()
					                                                     .get(Constants.TRACE_CONTEXT_MAP_KEY));
			
			MultiMap<String, String> headers = new MultiMap<>();
			
			headers.put(Constants.TRACEPARENT, traceContextMap.get(Constants.TRACEPARENT));
			headers.put(Constants.TRACESTATE, traceContextMap.get(Constants.TRACESTATE));
			
			ProcessorParameterValue processorParameterValue = parameters.get("requestBuilder");
			
			HttpRequesterRequestBuilder httpRequestBuilder = (HttpRequesterRequestBuilder) processorParameterValue.resolveValue();
			
			httpRequestBuilder.setHeaders(headers);
			//event.addVariable(Constants.HTTP_REQUEST_BUILDER, httpRequestBuilder);
			
			logger.debug("before::Intercepted a HTTP Request processor");
			*/
		}	
	}

	@Override
	public CompletableFuture<InterceptionEvent> around(ComponentLocation location,
			                                           Map<String, ProcessorParameterValue> parameters, 
			                                           InterceptionEvent event, 
			                                           InterceptionAction action)
	{
		if (isLoggerProcessor(location))
		{			
			ProcessorParameterValue processorParameterValue = parameters.get("message");
			
			try
			{
				// ------------------------------------------------------------------------------------
				//	Extract and resolve the Logger output.  Insert the resolved output into the Event 
				//	Message.  This message will be picked up by the End Processor Notification handler, 
				//	extracted and then added as a Span Event.
				// ------------------------------------------------------------------------------------
				String message = (String) processorParameterValue.resolveValue().toString();
				event.addVariable(Constants.LOGGER_OUTPUT_KEY, message, DataType.JSON_STRING);
				
				/*
				TypedValue<String> tv = new TypedValue<>(message, DataType.JSON_STRING);
				Message eventMessage = Message.builder().payload(tv).build();
				event.message(eventMessage);
				*/
				
				logger.debug("around::Intercepted a logger message resolved: " + message);
			}
			catch (Exception e)
			{
				// do nothing for now
				logger.debug("around::Intercepted logger processor resolved with error: " + e.getMessage());
			}			
			logger.debug("around::Intercepted a logger processor");
		}
		
		/*
		if (isHttpRequestProcessor(location))
		{			
			Map<String, String> traceContextMap = TypedValue.unwrap(event.getVariables().get(ObservabilitySemantics.TRACE_CONTEXT_MAP_KEY));
			
			MultiMap<String, String> headers = new MultiMap<>();
			
			headers.put(ObservabilitySemantics.TRACEPARENT, traceContextMap.get(ObservabilitySemantics.TRACEPARENT));
			headers.put(ObservabilitySemantics.TRACESTATE, traceContextMap.get(ObservabilitySemantics.TRACESTATE));
			
			try
			{
				ProcessorParameterValue processorParameterValue = parameters.get("requestBuilder");
				
				HttpRequesterRequestBuilder httpRequestBuilder = (HttpRequesterRequestBuilder) processorParameterValue.resolveValue();
				
				httpRequestBuilder.setHeaders(headers);
				
				logger.debug("before::Intercepted a HTTP Request processor");
			}
			catch (Exception e)
			{
				// do nothing for now
				logger.debug("around::Intercepted http requester message resolved with error: " + e.getMessage());
			}
		}
		*/
		return action.proceed();
	}
		
	
	@Override
	public void after(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown)
	{
		if (isLoggerProcessor(location))
		{
			logger.debug("around::Intercepted a logger processor");
		}
	}

	private boolean isHttpRequestProcessor(ComponentLocation location)
	{
		return TracingProcessorInterceptorFactory.isProcessorType(location, Constants.HTTP_REQUESTER);
	}
	
	private boolean isLoggerProcessor(ComponentLocation location)
	{
		return TracingProcessorInterceptorFactory.isProcessorType(location, Constants.LOGGER);
	}	
}
