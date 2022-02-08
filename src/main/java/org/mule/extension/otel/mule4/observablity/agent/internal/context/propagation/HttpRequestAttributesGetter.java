package org.mule.extension.otel.mule4.observablity.agent.internal.context.propagation;

import org.mule.extension.http.api.HttpRequestAttributes;

import io.opentelemetry.context.propagation.TextMapGetter;

public class HttpRequestAttributesGetter implements TextMapGetter<HttpRequestAttributes>
{

	@Override
	public Iterable<String> keys(HttpRequestAttributes carrier)
	{
		return carrier.getHeaders().keySet();
	}

	@Override
	public String get(HttpRequestAttributes carrier, String key)
	{
		return carrier.getHeaders().get(key);
	}

}
