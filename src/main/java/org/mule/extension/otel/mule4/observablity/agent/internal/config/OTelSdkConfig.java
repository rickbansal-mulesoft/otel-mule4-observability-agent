package org.mule.extension.otel.mule4.observablity.agent.internal.config;

import org.mule.extension.otel.mule4.observablity.agent.internal.config.advanced.CustomAttributesConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.advanced.SpanGenerationConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter.OtlpExporterConfig;
import org.mule.extension.otel.mule4.observablity.agent.internal.config.resource.OTelResourceConfig;
import org.mule.runtime.core.api.config.MuleConfiguration;
import org.mule.runtime.core.api.el.ExpressionManager;

//----------------------------------------------------------------------------------
//	This class is a helper/wrapper class which stores all of the high level config
//	objects needed to configure the OpenTelemetry SDK.
//----------------------------------------------------------------------------------
/**
 * 
 * Configuration details for the OpenTelemetry SDK
 *
 */
public class OTelSdkConfig
{
	private final OTelResourceConfig       resourceConfig;
	private final OtlpExporterConfig       traceExporterConfig;
	private final OtlpExporterConfig       metricExporterConfig;
	private final MuleConfiguration	       muleConfiguration;
	private final SpanGenerationConfig     spanGenerationConfig;
	private final ExpressionManager        expressionManager;
	private final CustomAttributesConfig   customAttributesConfig;

	public OTelSdkConfig(OTelResourceConfig r, OtlpExporterConfig t, OtlpExporterConfig mt, 
	                     MuleConfiguration m, SpanGenerationConfig s, ExpressionManager e,
	                     CustomAttributesConfig cac)
	{
		this.resourceConfig = r;
		this.traceExporterConfig = t;
	    this.metricExporterConfig = mt;
		this.muleConfiguration = m;
		this.spanGenerationConfig = s;
		this.expressionManager = e;
		this.customAttributesConfig = cac;
	}

    //------------------------------------------------------------------------------
    //  Helper Methods
    //------------------------------------------------------------------------------	
	public OTelResourceConfig getResourceConfig()
	{
		return resourceConfig;
	}

	public OtlpExporterConfig getTraceExporterConfig()
	{
		return traceExporterConfig;
	}
	
    public OtlpExporterConfig getMetricExporterConfig()
    {
        return metricExporterConfig;
    }
	
	public MuleConfiguration getMuleConfiguration()
	{
		return muleConfiguration;
	}
	
	public SpanGenerationConfig getSpanGenerationConfig()
	{
		return spanGenerationConfig;
	}
	
	public ExpressionManager getExpressionManager()
	{
	    return expressionManager;
	}
	
	public CustomAttributesConfig getCustomAttributesConfig()
	{
	    return customAttributesConfig;
	}
}