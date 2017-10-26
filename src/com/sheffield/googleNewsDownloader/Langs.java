package com.sheffield.googleNewsDownloader;

public enum Langs
{
	ENGLISH("en"),
	GERMAN("de"),
	LATVIAN("lv"),
	GREEK("el"),
	ROMANIAN("ro"),
	SLOVENIAN("sl"),
	CROATIAN("hr"),
	LITHUANIAN("lt"),
	ESTONIAN("et");
	
	private String code;
	
	Langs(String code)
	{
		this.code = code;
	}
	
	public String getCode()
	{
		return this.code;
	}
}
