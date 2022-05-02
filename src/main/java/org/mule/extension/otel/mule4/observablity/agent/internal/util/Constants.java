package org.mule.extension.otel.mule4.observablity.agent.internal.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 * Repository for Module level Constants
 *
 */
public class Constants
{
	public static final String PROCESSOR_INTERCEPTOR_ENABLE 	= "mule.otel.processor.interceptor.enable";
	public static final String TRACE_TRANSACTION_ID 			= "TRACE_TRANSACTION_ID";
	public static final String TRACE_CONTEXT_MAP_KEY 			= "OTEL_TRACE_CONTEXT";	
	public static final String LOGGER_OUTPUT_KEY 			    = "__LOGGER_OUTPUT_KEY__";	
	
	public static final String HTTP_REQUEST_BUILDER 			= "_HTTP_REQUEST_BUILDER";	

	public static final String FLOW								= "flow";

	public static final String DB_SELECT						= "db:select";
	public static final String DB_MATCHER						= "^db:.*$";

	public static final String HTTP_LISTENER 					= "http:listener";	
	public static final String HTTP_REQUESTER					= "http:request";

	public static final String LOGGER							= "mule:logger";
	public static final String FLOW_REFERENCE					= "mule:flow-ref";
	public static final String TRY_SCOPE					    = "mule:try";
	public static final String FOREACH_SCOPE					= "mule:foreach";
	
	public static final String FLOW_NAME_ATTRIBUTE				= "flow.name";
	public static final String SERVER_ID_ATTRIBUTE				= "server.id";
	public static final String DOC_NAME_ATTRIBUTE				= "doc.name";
	public static final String START_DATETIME_ATTRIBUTE			= "start.datetime";
	public static final String END_DATETIME_ATTRIBUTE			= "end.datetime";
	
	public static final String INSTRUMENTATION_VERSION_DEFAULT 	= "0.0.1";
	public static final String INSTRUMENTATION_NAME 			= "org.mulesoft.extension.otel.mule4.observability.agent";
	
	public static final String TRACEPARENT 						= "traceparent";
	public static final String TRACESTATE 						= "tracestate";
	
	public static final String HTTP_REQUESTER_URI_NS 			= "http://www.mulesoft.org/schema/mule/http";
	public static final String DB_URI_NS 						= "http://www.mulesoft.org/schema/mule/db";	

	public static final String AGENT_ARTIFACT_ID 				= "otel-mule4-observability-agent";	
	
	public static final List<String> AUTO_SKIP_LIST				= new ArrayList<String>(Arrays.asList(FLOW_REFERENCE, 
			                                                                                          TRY_SCOPE, 
			                                                                                          FOREACH_SCOPE));
}
