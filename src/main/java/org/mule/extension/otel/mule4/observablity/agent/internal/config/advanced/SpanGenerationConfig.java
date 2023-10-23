package org.mule.extension.otel.mule4.observablity.agent.internal.config.advanced;

import java.util.List;

import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.DisplayName;
import org.mule.runtime.extension.api.annotation.param.display.Placement;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

//----------------------------------------------------------------------------------
//This class stores details on span generation for a trace
//----------------------------------------------------------------------------------
//@ExclusiveOptionals
public class SpanGenerationConfig
{

	@Parameter()
    @Placement(order = 10, tab = "OTLP Trace Exporter")
	@DisplayName(value = "Generate Message Processor Spans")
	@Summary("Generate Message Processor spans and them to the trace")
	@Optional (defaultValue = "true")
	@Expression(ExpressionSupport.NOT_SUPPORTED)
	private boolean generateMessageProcessorsSpans;

	@Parameter
    @Placement(order = 20, tab = "OTLP Trace Exporter")
	@DisplayName("Message Processor Span Bypass")
	@Optional
	@NullSafe
    @Expression(ExpressionSupport.NOT_SUPPORTED)
	@Summary("List of Message Processors to bypass when generating Message Processor spans.")
	private List<MuleComponent> bypassComponents;
	
	
	public boolean getGenerateMessageProcessorsSpans()
	{
		return this.generateMessageProcessorsSpans;
	}
	
	public List<MuleComponent> getBypassComponents()
	{
		return this.bypassComponents;
	}
}