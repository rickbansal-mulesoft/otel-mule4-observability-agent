package org.mule.extension.otel.mule4.observablity.agent.internal.notification.parser.service.provider;

import java.util.Map;

import org.mule.extension.otel.mule4.observablity.agent.internal.context.propagation.AnypointMQMessageAttributesGetter;
import org.mule.extension.otel.mule4.observablity.agent.internal.context.propagation.OTelContextPropagator;
import org.mule.extension.otel.mule4.observablity.agent.internal.store.config.MuleConnectorConfigStore;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.Constants;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.NotificationParserUtils;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.notification.EnrichedServerNotification;
import org.mule.runtime.api.notification.PipelineMessageNotification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.context.Context;

import com.mulesoft.extension.mq.api.attributes.AnypointMQMessageAttributes;

public class AnypointMQSubscriberParser extends BaseNotificationParser
{
    private static Logger logger = LoggerFactory.getLogger(AnypointMQSubscriberParser.class);

    // --------------------------------------------------------------------------------------------
    // Verify if this Parser can handle this notification
    // -------------------------------------------------------------------------------------------- 
    @Override
    public boolean canParse(EnrichedServerNotification notification)
    {
        ComponentIdentifier sourceIdentifier = NotificationParserUtils.getSourceIdentifier(notification);
        String sourceComponent = sourceIdentifier.getNamespace() + ":" + sourceIdentifier.getName();
        int action = Integer.parseInt(notification.getAction().getIdentifier());
        
        // ----------------------------------------------------------------------------------------
        // Only parse MQ Listener notifications which are a source/trigger to the start of a flow
        // ----------------------------------------------------------------------------------------
        if ( sourceComponent.equalsIgnoreCase(Constants.ANYPOINT_MQ_SUBSCRIBER)  && 
             action == PipelineMessageNotification.PROCESS_START )
        {
            return true;
        }
        else
        {
            return false;
        }
    }
    
    // --------------------------------------------------------------------------------------------
    // Pipeline Start Notification Parsing Handler
    // --------------------------------------------------------------------------------------------
    @Override
    public SpanBuilder startPipelineNotification(EnrichedServerNotification notification,
                                                 MuleConnectorConfigStore muleConnectorConfigStore, 
                                                 SpanBuilder spanBuilder)
    {
        super.startPipelineNotification(notification, muleConnectorConfigStore, spanBuilder);
        
        try
        {
            spanBuilder.setSpanKind(SpanKind.SERVER);
            spanBuilder = addMQSubscriberAttributesToSpan(notification, muleConnectorConfigStore, spanBuilder);

            AnypointMQMessageAttributes anypointMQMessageAttributes = NotificationParserUtils.getMessageAttributes(notification);

            // ------------------------------------------------------------------------------------
            //  Copy over any WC3 Trace Headers from the incoming MQ message into the current
            //  trace context
            // ------------------------------------------------------------------------------------
            Context context = OTelContextPropagator.extract(anypointMQMessageAttributes, new AnypointMQMessageAttributesGetter());
            spanBuilder.setParent(context); 
        }
        catch (Exception e)
        {
            logger.debug(e.getMessage());
        }
        
        return spanBuilder;
    }

    // --------------------------------------------------------------------------------------------
    // Message Processor Start Notification Parsing Handler
    // -------------------------------------------------------------------------------------------- 
    
    @Override
    public SpanBuilder startProcessorNotification(EnrichedServerNotification notification,
                                                  MuleConnectorConfigStore muleConnectorConfigStore, 
                                                  SpanBuilder spanBuilder)
    {
        super.startProcessorNotification(notification, muleConnectorConfigStore, spanBuilder);
                
        return addMQSubscriberConfigAttributesToSpan(notification, muleConnectorConfigStore, spanBuilder);
    }
    
    
    // --------------------------------------------------------------------------------------------
    // Annotate the span with various MQ Subscriber attributes
    // --------------------------------------------------------------------------------------------
    private SpanBuilder addMQSubscriberAttributesToSpan(EnrichedServerNotification notification,
                                                        MuleConnectorConfigStore muleConnectorConfigStore,
                                                        SpanBuilder spanBuilder)
    {
        AnypointMQMessageAttributes anypointMQMessageAttributes = NotificationParserUtils.getMessageAttributes(notification);

        try 
        {
            spanBuilder.setAttribute("subscriber.message.contentType", anypointMQMessageAttributes.getContentType());            
            spanBuilder.setAttribute("subscriber.destination", anypointMQMessageAttributes.getDestination());
            spanBuilder.setAttribute("subscriber.messageId", anypointMQMessageAttributes.getMessageId());            
        }
        catch (Exception e)
        {
            logger.debug(e.getMessage());
        }

        return spanBuilder;
    }

    // --------------------------------------------------------------------------------------------
    // Annotate the span with various MQ Subscriber configuration attributes
    // --------------------------------------------------------------------------------------------
    
    private SpanBuilder addMQSubscriberConfigAttributesToSpan(EnrichedServerNotification notification,
                                                              MuleConnectorConfigStore muleConnectorConfigStore,
                                                              SpanBuilder spanBuilder)
    {
        Map<String, String> anypointMQAttributes = NotificationParserUtils.getComponentAnnotation("{config}componentParameters", notification);
        String configRef = anypointMQAttributes.get("config-ref");
    
        MuleConnectorConfigStore.AnypointMQConfig anypointMQConfig = muleConnectorConfigStore.getConfig(configRef);
        
        try 
        {
            spanBuilder.setAttribute("subscriber.scheme", anypointMQConfig.getProtocol());
            spanBuilder.setAttribute("subscriber.host", anypointMQConfig.getHost());
            spanBuilder.setAttribute("subscriber.port", anypointMQConfig.getPort());
            spanBuilder.setAttribute("subscriber.path", anypointMQConfig.getPath());     

            spanBuilder.setAttribute("subscriber.clientId", anypointMQConfig.getClientId());                    
        }
        catch (Exception e)
        {
            logger.debug(e.getMessage());
        }

        return spanBuilder;
    }    
}
