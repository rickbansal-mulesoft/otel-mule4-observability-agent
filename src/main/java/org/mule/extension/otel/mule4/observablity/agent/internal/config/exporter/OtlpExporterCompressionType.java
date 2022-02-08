package org.mule.extension.otel.mule4.observablity.agent.internal.config.exporter;

//----------------------------------------------------------------------------------
//	This class stores all of the compression types supported by the OpenTelemetry 
//	Protocol
//----------------------------------------------------------------------------------
public enum OtlpExporterCompressionType
{
	NONE 	("none"), 
	GZIP 	("gzip");

	private final String compression;

	//------------------------------------------------------------------------------
	//	Default Constructor
	//------------------------------------------------------------------------------
	OtlpExporterCompressionType(String value)
	{
		this.compression = value;
	}

	/**
	 * 
	 * @return the compression type
	 */
	public String getCompressionType()
	{
		return compression;
	}
}