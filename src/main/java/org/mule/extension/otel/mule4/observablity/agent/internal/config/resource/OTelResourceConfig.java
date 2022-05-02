package org.mule.extension.otel.mule4.observablity.agent.internal.config.resource;

import org.apache.commons.lang3.StringUtils;
import org.mule.extension.otel.mule4.observablity.agent.internal.util.KeyValuePair;
import org.mule.runtime.api.meta.ExpressionSupport;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.Expression;
import org.mule.runtime.extension.api.annotation.param.NullSafe;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.annotation.param.display.Summary;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

//----------------------------------------------------------------------------------
//	This class stores metadata about an OpenTelemetry Resource.  A resource is the
//	origin or the source of the telemetry data.
//----------------------------------------------------------------------------------
@Alias("resource")
@Summary("This class stores configuration details regarding the source of the telemetry data" +
		 "(signals) in the form of Attributes.")
public class OTelResourceConfig
{

	@Parameter
	@Summary("Logical (unique) name for this application (e.g., shoppingcart, productAPI, ...).")
	//@Expression(ExpressionSupport.NOT_SUPPORTED)
	private String serviceName;

	public String getServiceName()
	{
		return serviceName;
	}

	//------------------------------------------------------------------------------
	//	An optional list of user defined resource attributes in the form of 
	// 	<key,value> pairs. Attributes provide context to a distributed trace. 
	//	Attributes can give teams the raw data to find meaningful correlations, and 
	//	a clear view of what was involved when performance changes occur.
	//
	//	See the following link regarding best practices:
	//	https://opentelemetry.lightstep.com/core-concepts/attributes-and-labels/
	//------------------------------------------------------------------------------
	@Parameter
	@Summary("Optional attributes for this application defined as <key,value> pairs" +
			 "(e.g., service.namespace:com.mycompany, service.version:0.0.1, ...). " +
			 "Attributes provide context to a distributed trace which can help with " +
			 "correlations to proivde a clear view of what was involved when performance "+
			 "changes occur.")
	@Optional
	@NullSafe
	private List<Attribute> resourceAttributes;

	public List<Attribute> getResourceAttributes()
	{
		return resourceAttributes;
	}

	public Map<String, String> getProperties()
	{
		Map<String, String> config = new HashMap<>();
		
		config.put("otel.service.name", getServiceName());
		config.put("otel.resource.attributes", KeyValuePair.commaSeparatedList(getResourceAttributes()));
		
		return Collections.unmodifiableMap(config);
	}

	//------------------------------------------------------------------------------
	//	Override Object behavior
	//------------------------------------------------------------------------------
	@Override
	public String toString()
	{
		return StringUtils.join(getProperties());
	}
	
	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
		
		if (o == null || getClass() != o.getClass())
			return false;
		
		OTelResourceConfig that = (OTelResourceConfig) o;
		
		return Objects.equals(serviceName, that.serviceName) && 
			   Objects.equals(resourceAttributes, that.resourceAttributes);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(serviceName, resourceAttributes);
	}
}
