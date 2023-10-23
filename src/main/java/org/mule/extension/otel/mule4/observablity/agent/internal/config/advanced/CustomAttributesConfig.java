package org.mule.extension.otel.mule4.observablity.agent.internal.config.advanced;

import java.util.List;

import org.mule.extension.otel.mule4.observablity.agent.internal.config.resource.Attribute;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.Constants;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.api.notification.EnrichedServerNotification;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.opentelemetry.api.trace.SpanBuilder;

//----------------------------------------------------------------------------------
// This class stores user defined custom attributes
//----------------------------------------------------------------------------------
public class CustomAttributesConfig
{
    @Parameter()
    @Placement(order = 30, tab = "OTLP Trace Exporter")
    @DisplayName(value = "Send Custom Attributes for Flow Spans")
    @Summary("Generate and send custom attributes when a flow starts")
    @Optional (defaultValue = "false")
    @Expression(ExpressionSupport.NOT_SUPPORTED)
    private boolean sendCustomAttributesPerFlow;
    
    @Parameter()
    @Placement(order = 40, tab = "OTLP Trace Exporter")
    @DisplayName(value = "Send Custom Attributes for Message Proccesor Spans")
    @Summary("Generate and send custom attributes when a message proccesor starts")
    @Optional (defaultValue = "false")
    @Expression(ExpressionSupport.NOT_SUPPORTED)
    private boolean sendCustomAttributesPerProcessor;
    
    @Parameter
    @Placement(order = 50, tab = "OTLP Trace Exporter")
    @DisplayName("User Defined Custom Attributes")
    @Optional
    @NullSafe
    @Expression(ExpressionSupport.NOT_SUPPORTED)
    @Summary("List of user defined custom attributes in name-value pairs.")
    private List<Attribute> customAttributes;
    
    private static Logger logger = LoggerFactory.getLogger(CustomAttributesConfig.class);

    public boolean getSendCustomAttributesPerFlow()
    {
        return this.sendCustomAttributesPerFlow;
    }
    
    public boolean getSendCustomAttributesPerProcessor()
    {
        return this.sendCustomAttributesPerProcessor;
    }
    
    public List<Attribute> getCustomAttributes()
    {
        return this.customAttributes;
    }
    
    public void setAttributes(SpanBuilder spanBuilder, ExpressionManager em, EnrichedServerNotification n) 
    {
        try
        {
            customAttributes.forEach((a) -> spanBuilder.setAttribute(Constants.CUSTOM_ATTRIBUTE + a.getKey(em, n), a.getValue(em, n)));  
        }
        catch (Exception e)
        {
            logger.debug(e.getMessage());
        }
    }    
}