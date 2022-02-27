package org.mule.extension.otel.mule4.observablity.agent.internal.util;

import org.mule.runtime.extension.api.annotation.param.Parameter;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

//----------------------------------------------------------------------------------
//	General class for storing a <key,value> pair as a Mule Parameter
//----------------------------------------------------------------------------------
public abstract class KeyValuePair
{

	@Parameter
	private String key;

	@Parameter
	private String value;

	public String getKey()
	{
		return key;
	}

	public String getValue()
	{
		return value;
	}
	
	//------------------------------------------------------------------------------
	// 	Helper method for converting a List<KeyValuePair> to a comma separated
	//  string with the following format:
	//  "key1=value1, key2=value2, key3=value3, ..."
	//------------------------------------------------------------------------------
	public static String commaSeparatedList(List<? extends KeyValuePair> pairs)
	{
		if (pairs == null)
			return "";
		
		return pairs.stream().map(KeyValuePair::toString).collect(Collectors.joining(","));
	}

	//------------------------------------------------------------------------------
	//	Override Object behavior
	//------------------------------------------------------------------------------
	@Override
	public boolean equals(Object o)
	{
		if (this == o)
			return true;
	
		if (o == null || getClass() != o.getClass())
			return false;
		
		KeyValuePair that = (KeyValuePair) o;
		
		return Objects.equals(key, that.key) && Objects.equals(value, that.value);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(key, value);
	}

	@Override
	public String toString()
	{
		return key + "=" + value;
	}
}
