package org.mule.extension.otel.mule4.observablity.agent.internal.interceptor;

import org.mule.extension.otel.mule4.observablity.agent.internal.util.Constants;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 	This class is an implementation of the MuleSoft ProcessorInterceptorFactory which can intercept processors 
 * 	at various stages of the processor lifecycle. To intercept processors, two things must be done:
 * 	<ol>
 * 		<li> Instantiation of this class must be done during the Mule Runtime boot up process. </li> </p>
 * 		<ul>
 * 			<li> Add an entry in the <code>src/main/resources/META-INF/services/org/mule/config/registry-bootstrap.properties</code> file </li>
 * 		</ul>
 * 		<li> The <code>mule.otel.interceptor.processor.enable</code> system property must be set to <code>true</code> (default). </li>
 *	</ol>
 * 	For now only the following processors are intercepted: </p>
 *	<ul>
 *		<li>The first processor of a flow or a sub-flow:</li> </p>
 *		<ul>
 *			<li> That is, after <code>PipelineMessageNotification.PROCESS_START</code>
 *				 event but before the <code>MessageProcessorNotification.MESSAGE_PROCESSOR_PRE_INVOKE</code> event. 
 *			</li>
 *		</ul>
 *      <li>An HTTP Request processor (regardless of location).</li>
 *      <li>A Logger processor (regardless of location).</li>
 *	</ul>
 * @see 
 * <a href=https://docs.mulesoft.com/mule-runtime/3.9/bootstrapping-the-registry>
 * Bootstrapping the Mule Registry
 * </a>
 */
@Component
public class TracingProcessorInterceptorFactory implements ProcessorInterceptorFactory
{
	private static Logger logger = LoggerFactory.getLogger(TracingProcessorInterceptorFactory.class);

	private  boolean interceptorEnabled;
	
	public TracingProcessorInterceptorFactory()
	{
		logger.info("TracingProcessorInterceptorFactory created");
	}
	
	@Override
	public ProcessorInterceptor get()
	{
		return new TracingProcessorInterceptor();
	}

	/**
	 *
	 * @param location
	 * @{@link ComponentLocation}
	 * @return
	 */
	@Override
	public boolean intercept(ComponentLocation location)
	{
		interceptorEnabled = Boolean.parseBoolean(System.getProperty(Constants.PROCESSOR_INTERCEPTOR_ENABLE, "false"));
		
		if (!interceptorEnabled)
			return false;
		
		if (isInterceptable(location))
		{
			logger.debug("Processor at location: " + location.getLocation() + " intercepted");
			return true;
		}
		return false;
	}
	
	private boolean isInterceptable(ComponentLocation location)
	{
		boolean is = false;
		
		if (isFirstProcessor(location) 
			|| isProcessorType(location, Constants.HTTP_REQUESTER)
			|| isProcessorType(location, Constants.LOGGER))
		{
			is = true;
		}
		return is;
	}
	
	public static boolean isFirstProcessor(ComponentLocation location)
	{
		return (location.getLocation().endsWith("/0")) ? true : false;
	}
	
	public static boolean isProcessorType(ComponentLocation location, String processorType)
	{
		ComponentIdentifier componentIdentifier = location.getComponentIdentifier().getIdentifier();
		String sourceComponent = componentIdentifier.getNamespace() + ":" + componentIdentifier.getName();
		
		return (sourceComponent.matches("^"+ processorType + "$")) ? true : false;
	}
}
