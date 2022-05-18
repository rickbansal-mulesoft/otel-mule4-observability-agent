package org.mule.extension.otel.mule4.observablity.agent.internal.util;

import java.time.Instant;
import java.util.Map;

import javax.xml.namespace.QName;

import org.mule.extension.otel.mule4.observablity.agent.internal.config.advanced.MuleComponent;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.advanced.SpanGenerationConfig;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.api.notification.EnrichedServerNotification;
import org.mule.runtime.api.notification.MessageProcessorNotification;
import org.mule.runtime.api.notification.PipelineMessageNotification;

/**
 * 
 *	A set of utilities (helper methods) to facilitate parsing of Notifications:
 *	<ul>
 *		<li> {@link #getInstantFrom(EnrichedServerNotification)} </li>
 *		<li> {@link #getSourceIdentifier(EnrichedServerNotification)} </li>
 *		<li> {@link #getDocName(EnrichedServerNotification)} </li>
 *		<li> {@link #getFlowId(EnrichedServerNotification)} </li>
 *		<li> {@link #getSpanName(EnrichedServerNotification)} </li>
 *		<li> {@link #getSpanId(EnrichedServerNotification)} </li>
 *		<li> {@link #getMuleSoftTraceId(EnrichedServerNotification)} </li>
 *		<li> {@link #getComponentId(EnrichedServerNotification)} </li>
 *		<li> {@link #getMessageAttributes(EnrichedServerNotification)} </li>
 *		<li> {@link #getMessageAttributes(Event)} </li>
 *		<li> {@link #getMessage(EnrichedServerNotification)} </li>
 *	</ul>
 *
 */
public class NotificationParserUtils
{
	// --------------------------------------------------------------------------------------------
	// Various public utility parsing helpers
	// --------------------------------------------------------------------------------------------
	/**
	 * 
	 * @param notification
	 * @return - an time Instant based on the timestamp in the notification
	 */
	public static Instant getInstantFrom(EnrichedServerNotification notification)
	{
		return Instant.ofEpochMilli(notification.getTimestamp());
	}
	
	/**
	 * 
	 * @param notification
	 * @return - the source/trigger related to this notification
	 */
	public static ComponentIdentifier getSourceIdentifier(EnrichedServerNotification notification)
	{
		ComponentIdentifier sourceIdentifier = null;
		
		if (notification.getEvent() != null && notification.getEvent().getContext().getOriginatingLocation() != null)
		{
			sourceIdentifier = notification.getEvent()
					                       .getContext()
					                       .getOriginatingLocation()
					                       .getComponentIdentifier()
					                       .getIdentifier();
		}
		return sourceIdentifier;
	}
	
	/**
	 * 
	 * @param notification
	 * @return - the user defined name for this component (flow or message processor)
	 */
	public static String getDocName(EnrichedServerNotification notification)
	{
		String docName = "";

		if (notification instanceof PipelineMessageNotification)
			docName = "name";
		else if (notification instanceof MessageProcessorNotification)
			docName = "doc:name";

		Map<String, String> annotations = getComponentAnnotation("{config}componentParameters", notification);
		
		return annotations.get(docName);
	}
	
	/**
	 * 
	 * @param notification
	 * @return the first part of the unique location of this component.  Should be the flow name.
	 * @see
	 * <a href=https://www.mulesoft.org/docs/site/4.3.0/apidocs/org/mule/runtime/api/component/location/ComponentLocation.html>
	 * 	Component Location
	 * </a>	
	 */
	public static String getFlowId(EnrichedServerNotification notification)
	{

		String flowId = "_" + notification.getComponent().getLocation().getRootContainerName();
		
		//
		// Check if this notification is related to a message processor in a sub-flow
		//
		String rcn = getComponentAnnotation("{http://www.mulesoft.org/schema/mule/parser-metadata}ROOT_CONTAINER_NAME", notification);
		
		if (rcn != null)
			flowId = "_" + rcn;
		
		//return "_" + notification.getComponent().getLocation().getRootContainerName();
		return flowId;
	}
	
	public static String getServerId(EnrichedServerNotification notification)
	{
		return notification.getServerId();
	}

	/**
	 * This should be unique for each component in a Mule application
	 * 
	 * @param notification
	 * @return - a unique name
	 */
	public static String getSpanName(EnrichedServerNotification notification)
	{
		return notification.getComponent().getIdentifier().getName() + "::" + getDocName(notification);
	}

	/**
	 * @param notification
	 * @return The unique absolute location of this component in the Mule application
	 */
	public static String getSpanId(EnrichedServerNotification notification)
	{
		return notification.getInfo().getComponent().getLocation().getLocation();
	}

	/**
	 * @param notification
	 * 	@return The Correlation ID in Mule Event as the OTel Trace ID - unique value
	 */
	public static String getMuleSoftTraceId(EnrichedServerNotification notification)
	{
		return notification.getEvent().getCorrelationId();
	}

	/**
	 * 
	 * @param notification
	 * @return The qualified name {@code <namespace:name>} of this component.  For example, {@code http:listener, http:requester,}...
	 */
	public static String getComponentId(EnrichedServerNotification notification)
	{
		ComponentIdentifier componentIdentifier = getComponentIdentifier(notification);

		return (componentIdentifier.getNamespace() + ":" + componentIdentifier.getName());
	}
	
	public static ComponentIdentifier getComponentIdentifier(EnrichedServerNotification notification)
	{
		return getComponentAnnotation("{config}componentIdentifier", notification);
	}
	
	public static MuleComponent getComponentAsMuleComponent(EnrichedServerNotification notification) 
	{
		MuleComponent muleComponent = new MuleComponent();
		ComponentIdentifier ci = getComponentIdentifier(notification);
	
		muleComponent.setNamespace(ci.getNamespace());
		muleComponent.setName(ci.getName());
		
		return muleComponent;
	}

	public static boolean skipParsing(EnrichedServerNotification notification, SpanGenerationConfig spanGenerationConfig)
	{
		return (Constants.AUTO_SKIP_LIST.contains(getComponentId(notification))
				|| !(spanGenerationConfig.getGenerateMessageProcessorsSpans())
				|| spanGenerationConfig.getBypassComponents().contains((MuleComponent)getComponentAsMuleComponent(notification)));
	}
	
	/**
	 * 
	 * @param <T>
	 * @param notification 
	 * @return - the attributes associated with this notification
	 */
	public static <T> T getMessageAttributes(EnrichedServerNotification notification)
	{
		return (T) notification.getEvent().getMessage().getAttributes().getValue();
	}
	
	/**
	 * 
	 * @param <T>
	 * @param event
	 * @return - the attributes associated with this event
	 */
	public static  <T> T getMessageAttributes(Event event)
	{
		return (T) event.getMessage().getAttributes().getValue();
	}
	
	/**
	 * 
	 * @param <T>
	 * @param notification
	 * @return - return the "payload" associated with this notification
	 */
	public static  <T> T getMessage(EnrichedServerNotification notification)
	{
		return (T) notification.getEvent().getMessage().getPayload().getValue();
	}
	
	public static <T> T getComponentAnnotation(String annotationName, EnrichedServerNotification notification)
	{
		return (T) notification.getComponent().getAnnotation(QName.valueOf(annotationName));
	}
}
