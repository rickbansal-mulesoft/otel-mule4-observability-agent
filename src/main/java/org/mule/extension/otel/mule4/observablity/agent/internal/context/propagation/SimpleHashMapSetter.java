package org.mule.extension.otel.mule4.observablity.agent.internal.context.propagation;

import java.util.Map;

import io.opentelemetry.context.propagation.TextMapSetter;

public class SimpleHashMapSetter implements TextMapSetter<Map<String, String>>
{

	@Override
	public void set(Map<String, String> carrier, String key, String value)
	{
		if (carrier != null)
			carrier.put(key, value);		
	}
}
