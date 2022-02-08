package org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter;

import java.util.Arrays;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;


//----------------------------------------------------------------------------------
//	This class stores all of the transports supported by the OpenTelemetry Protocol
//----------------------------------------------------------------------------------
public enum OtlpExporterTransportProtocolType
{
	GRPC 			("grpc"), 
	HTTP_PROTOBUF 	("http/protobuf"), 
	HTTP_JSON 		("http/json");

	private final String protocol;
	private static Map<String, OtlpExporterTransportProtocolType> transportProtocols;

	//------------------------------------------------------------------------------
	//	Static block to initialize the transportProtocols map.  This block is only
	//	executed once when the class is loaded into memory.
	//------------------------------------------------------------------------------
	static
	{
		transportProtocols = Arrays.stream(OtlpExporterTransportProtocolType.values())
							 .collect(Collectors
						     .toMap(OtlpExporterTransportProtocolType::getProtocolType, Function.identity()));
	}

	//------------------------------------------------------------------------------
	//	Default Constructor
	//------------------------------------------------------------------------------
	OtlpExporterTransportProtocolType(String value)
	{
		this.protocol = value;
	}

	/**
	 * 
	 * @return the transport protocol
	 */
	public String getProtocolType()
	{
		return protocol;
	}

	OtlpExporterTransportProtocolType fromValue(String value)
	{
		return transportProtocols.get(value);
	}
}