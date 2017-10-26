package com.sheffield.googleNewsDownloader;

import gsearch.internal.gson.GsonFactory;
import gsearch.internal.http.GzipRequestInterceptor;
import gsearch.internal.http.GzipResponseInterceptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.AllClientPNames;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;

import com.google.gson.Gson;
import com.sheffield.googleNewsDownloader.Response.ResponseData;
import com.sheffield.util.Util;

public class GoogleNewsDownloader {
	private HttpClient httpClient;
	private static final String NEWS_SEARCH_ENDPOINT = "http://ajax.googleapis.com/ajax/services/search/news";
	private static final String LOCAL_SEARCH_ENDPOINT = "http://ajax.googleapis.com/ajax/services/search/local";
	private static final String WEB_SEARCH_ENDPOINT = "http://ajax.googleapis.com/ajax/services/search/web";
	private static final String BOOK_SEARCH_ENDPOINT = "http://ajax.googleapis.com/ajax/services/search/books";
	private static final String IMAGE_SEARCH_ENDPOINT = "http://ajax.googleapis.com/ajax/services/search/images";
	private static final String VIDEO_SEARCH_ENDPOINT = "http://ajax.googleapis.com/ajax/services/search/videos";
	private static final String BLOG_SEARCH_ENDPOINT = "http://ajax.googleapis.com/ajax/services/search/blogs";
	
	private boolean compressionEnabled = false;
	private static String URL = "";
	private static String KEY = "";
	public static void main(String[] args) throws IOException {
		if (args.length < 3) {
			System.out.println("ERROR in argument list: java -jar googleNewsDownloader pathToSettingFile pathToSaveOutput");
		} else {
			String file = args[0];
			Vector<String> lines = Util.getFileContentAsVector(file);
			String toSaveFile = args[1];
			String aLangCode = lines.get(0);
					
			URL = lines.get(2);
			KEY = lines.get(1);
			
			collectDocuments(aLangCode, toSaveFile);			
		}

	}
	
	
	private static void collectDocuments(String aLangCode, String aPathToSave) throws IOException {
		GoogleNewsDownloader c = new GoogleNewsDownloader();
		NewsTopic topics[] = NewsTopic.values();
		Map<String, String> topicMaps = new HashMap<String, String>();
		topicMaps.put("h", "HEADLINES");
		topicMaps.put("w", "WORLD");
		topicMaps.put("n", "NATION");
		topicMaps.put("t", "SCIENCE_AND_TECHNOLOGY");
		topicMaps.put("el", "ELECTIONS");
		topicMaps.put("p", "POLITICS");
		topicMaps.put("e", "ENTERTAIMENT");
		topicMaps.put("s", "SPORTS");
		topicMaps.put("m", "HEALTH");
		topicMaps.put("po", "POPULAR");
		topicMaps.put("af", "AFRICA");
		topicMaps.put("pe", "RECOMMENDED");
		topicMaps.put("ir", "SPOTLIGHT");
		
		StringBuffer buffer = new StringBuffer();
//		buffer.append("url").append("\t");
//		buffer.append("languageCode").append("\t");
//		buffer.append("title").append("\t");
//		buffer.append("publishingDate").append("\n");

		Vector<String> found = new Vector<String>();
		for (int j = 0; j < topics.length; j++) {
			
			List<Result> results = null;
			try {
				results = c.searchNews(true,topics[j], aLangCode, aLangCode);							
			} catch (Exception e) {
				System.out.println("Error has accorred while searching " + e);
			}
			if (results == null) {
				continue;
			}						
			for (int i = 0; i < results.size(); i++) {
				Result result = results.get(i);
				if (found.contains(result.getUnescapedUrl().trim())) {
					//System.out.println("continuing");
					continue;
				}
				found.add(result.getUnescapedUrl().trim());
				buffer.append(result.getUnescapedUrl()).append("\t");
				buffer.append(aLangCode).append("\t");
				String title = result.getTitle();
				title = title.replaceAll("<b>", "");
				title = title.replaceAll("</b>", "");
				title = title.replaceAll("&#39;", "'");
				title = title.replaceAll("&quot;", "\"");
				buffer.append(title).append("\t");
				//buffer.append(topicMaps.get(topics[j].getCode())).append("\t");
				//buffer.append(result.getClusterUrl()).append("\t");
				buffer.append(result.getPublishedDate()).append("\n");
				//buffer.append(result.getPublisher()).append("\n");
				
			}
			Util.doSaveUTF(aPathToSave, buffer.toString());
		}					
	}
	
	
	
	public GoogleNewsDownloader()
	{
		this(new DefaultHttpClient());
	}
	
	public GoogleNewsDownloader(HttpClient hClient)
	{
		this.httpClient = hClient;
	

		//
		//  this user agent string has been crafted this way
		//  so that Google's service will return gzip compressed responses
		//  when Accept-Encoding: gzip is present in the request.
		//
		
		setUserAgent("Mozilla/5.0 (Java) Gecko/20081007 gsearch-java-client");
		
		setConnectionTimeout( 10 * 1000 );
		setSocketTimeout(25 * 1000);
		
		
	}
	
	public void setUserAgent(String ua)
	{
		this.httpClient.getParams().setParameter(AllClientPNames.USER_AGENT, ua);
	}
	
	public void setConnectionTimeout(int milliseconds)
	{
		httpClient.getParams().setIntParameter(AllClientPNames.CONNECTION_TIMEOUT, milliseconds);
	}
	
	public void setSocketTimeout(int milliseconds)
	{
		httpClient.getParams().setIntParameter(AllClientPNames.SO_TIMEOUT, milliseconds);
	}
	
	
	
	protected Response sendSearchRequest(boolean isDecodeQuery, String url, Map<String, String> params)
	{
		
		if (params.get("v") == null)
		{
			params.put("v", "1.0");
		}
		
		String json = null;
		try {
			json = sendHttpRequest(isDecodeQuery, "GET", url, params);
		} catch (IOException anException) {
			// TODO Auto-generated catch block
			anException.printStackTrace();
		}
		
		Response r = fromJson(json);
		
		r.setJson(json);
		
		return r;
	}
	
	
	protected Response fromJson(String json)
	{
		Gson gson = GsonFactory.createGson();
		
		Response r = gson.fromJson(json, Response.class);
		
		return r;
	}
	
	protected String sendHttpRequest(boolean isEndecodeQuery, String httpMethod, String aUrl, Map<String, String> params) throws IOException
	{
		//HttpClient c = getHttpClient();
		
		//HttpUriRequest request = null;
		
		StringBuilder builder = new StringBuilder();
		
		if ("GET".equalsIgnoreCase(httpMethod))
		{


			String queryString = buildQueryString(isEndecodeQuery, params);
			
			aUrl = aUrl + queryString;
			
			URL url = new URL(aUrl);
    		URLConnection connection = url.openConnection();
    		connection.addRequestProperty("Referer", URL);
    		String line;
    		
    		BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), "UTF-8"));
    		while((line = reader.readLine()) != null) {
    			builder.append(line);
    		}

//			try {
//				request = new HttpGet(url);
//				request.setHeader("Referer", "http://ir.shef.ac.uk/");
//
//			} catch (URISyntaxException anException) {
//				anException.printStackTrace();
//			}
//			
		}
		else
		{
			throw new RuntimeException("unsupported method: " + httpMethod);
		}
//	
//		HttpResponse response = null;
//		HttpEntity entity = null;
//		
//		try
//		{
//			response = c.execute(request);
//			
//			int statusCode = response.getStatusLine().getStatusCode();
//			
//			if (statusCode != HttpStatus.SC_OK)
//			{
//				throw new RuntimeException("unexpected HTTP response status code = " + statusCode);
//			}
//			
//			entity = response.getEntity();
			
			return builder.toString();//EntityUtils.toString(entity);
		}
//		catch (Exception ex)
//		{
//			throw new RuntimeException(ex);
//		}
//		finally
//		{
//			// todo : 
//		}
		
//	}
	
	private String buildQueryString(boolean isDecodeQuery, Map<String, String> params)
	{
		StringBuffer query = new StringBuffer();
		
		if (params.size() > 0)
		{
			query.append("?");
			
			for (String key : params.keySet())
			{
				query.append(key);
				query.append("=");
				if (isDecodeQuery) {
					query.append(encodeParameter(params.get(key)));
				} else {
					query.append(params.get(key));					
				}
				query.append("&");
			}
			
			if (query.charAt(query.length() - 1) == '&')
			{
				query.deleteCharAt(query.length() - 1);
			}
		}			
		
		return query.toString();
	}

	protected String decode(String s)
	{
		try
		{
			return URLDecoder.decode(s, "UTF-8");
		} 
		catch (UnsupportedEncodingException ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	protected String encodeParameter(String s)
	{
		try
		{
			return URLEncoder.encode(s, "UTF-8");
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}

	protected HttpClient getHttpClient()
	{
		
		if (this.httpClient instanceof DefaultHttpClient)
		{
			DefaultHttpClient defaultClient = (DefaultHttpClient) httpClient;
			
			defaultClient.removeRequestInterceptorByClass(GzipRequestInterceptor.class);
			defaultClient.removeResponseInterceptorByClass(GzipResponseInterceptor.class);
			
			if (this.isCompressionEnabled())
			{
				defaultClient.addRequestInterceptor(GzipRequestInterceptor.getInstance());
				defaultClient.addResponseInterceptor(GzipResponseInterceptor.getInstance());
			}
			
		}

		return this.httpClient;
	}
	
	/**
	 * 
	 * @param location  use "city, state" (example: "Miami, FL") or zip code  ("97202") or country ("Singapore")
	 * 
	 * @return
	 * 
	 */
	public List<Result> searchNewsByLocation(boolean isDecodeQuery,String location)
	{
		return searchNews(isDecodeQuery,null, location, null, null, null);
	}
	
	/**
	 * 
	 * @param query  may be null
	 * @param location  use "city, state" (example: "Miami, FL") or zip code  ("97202") or country ("Singapore")
	 * @param topic may be null
	 * 
	 * @return
	 * 
	 */
	public List<Result> searchNews(boolean isDecodeQuery, String query, String location, String aQueryLang, String aResultLang, NewsTopic topic)
	{
		List<Result> results = null;
		Map<String, String> params = new LinkedHashMap<String, String>();
		Map<Integer, String> times = new HashMap<Integer, String>(); 
		times.put(1, "a");//as_qdr, as_drrb=q
		times.put(2,"h");
		times.put(3,"d");
		times.put(4,"w");

		for (int i = 0; i < 5; i++) {
			if ( (query != null) && (query.trim().length() > 0) )
			{
				params.put("q", query);
			}
			
			if (location != null)
			{
				params.put("geo", location);
			}

			if (aQueryLang != null)
			{
				params.put("hl", aQueryLang);
			}

			if (aResultLang != null)
			{
				params.put("lr", "lang_" + aResultLang);
			}

			if (topic != null)
			{
				params.put("topic", topic.getCode());
			}
			params.put("key", KEY);
			params.put("as_scoring", "r");
			params.put("rsz", "8");
			
			if (i > 0) {
				params.put("as_qdr",times.get(i));
				params.put("as_drrb","q");
				
			}
			Response r = sendNewsSearchRequest(isDecodeQuery, params);
			if (r != null) {
				ResponseData data = r.getResponseData();
				if (data != null) {
					List<Result> list = data.getResults();		
					if (results == null) {
						results = list;
					} else {
						results.addAll(list);
					}
				}
			}
		}

		return results;
	}


	protected Response sendNewsSearchRequest(boolean isDecodeQuery, Map<String, String> params)
	{
		return sendSearchRequest(isDecodeQuery, NEWS_SEARCH_ENDPOINT, params);
	}
	
	protected Response sendLocalSearchRequest(boolean isDecodeQuery,Map<String, String> params)
	{
		return sendSearchRequest(isDecodeQuery, LOCAL_SEARCH_ENDPOINT, params);
	}
	
	protected Response sendWebSearchRequest(boolean isDecodeQuery,Map<String, String> params)
	{
		return sendSearchRequest(isDecodeQuery,WEB_SEARCH_ENDPOINT, params);
	}

	protected Response sendBookSearchRequest(boolean isDecodeQuery,Map<String, String> params)
	{
		return sendSearchRequest(isDecodeQuery,BOOK_SEARCH_ENDPOINT, params);
	}

	protected Response sendImageSearchRequest(boolean isDecodeQuery,Map<String, String> params)
	{
		return sendSearchRequest(isDecodeQuery, IMAGE_SEARCH_ENDPOINT, params);
	}

	public boolean isCompressionEnabled()
	{
		return compressionEnabled;
	}

	public void setCompressionEnabled(boolean b)
	{
		this.compressionEnabled = b;
	}

	
	
	/**
	 * 
	 * 
	 *   send HTTP GET
	 *   
	 *   This method can be used to retrieve images  (JPEG, PNG, GIF)
	 *   or any other file type
	 *   
	 *   @return byte array
	 *  
	 */
	public byte[] getBytesFromUrl(String url)
	{
		try
		{
			HttpGet get = new HttpGet(url);
			
			HttpResponse response = this.getHttpClient().execute(get);
			
			HttpEntity entity = response.getEntity();
			
			if (entity == null)
			{
				throw new RuntimeException("response body was empty");
			}
			
			return EntityUtils.toByteArray(entity);
		}
		catch (RuntimeException ex)
		{
			throw ex;
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}
	
	/**
	 * 
	 * send HTTP GET
	 * 
	 */
	public String get(String url) 
	{
		try
		{
			HttpGet get = new HttpGet(url);
			
			HttpResponse response = this.getHttpClient().execute(get);
			
			HttpEntity entity = response.getEntity();
			
			if (entity == null)
			{
				throw new RuntimeException("response body was empty");
			}
			
			return EntityUtils.toString(entity);
		}
		catch (RuntimeException ex)
		{
			throw ex;
		}
		catch (Exception ex)
		{
			throw new RuntimeException(ex);
		}
	}

	public List<Result> searchNews(boolean isDecodeQuery,NewsTopic topic)
	{
		return searchNews(isDecodeQuery,null, null, null, null, topic);
	}

	public List<Result> searchNews(boolean isDecodeQuery,NewsTopic topic, String aQueryLang, String aResultLang)
	{
		return searchNews(isDecodeQuery,null, null, aQueryLang, aResultLang, topic);
	}

	public List<Result> searchNewsLocal(boolean isDecodeQuery,NewsTopic topic, String aQueryLang, String aResultLang, String aLocation)
	{
		return searchNews(isDecodeQuery,null, aLocation, aQueryLang, aResultLang, topic);
	}

	
	public List<Result> searchWeb(boolean isDecodeQuery,String query)
	{
		Map<String, String> params = new LinkedHashMap<String, String>();
		
		params.put("q", query);
		
		params.put("key", "ABQIAAAA1UHmftlU1fBs1ZNG-Xj9rxQF72hysTxY9RoN4abhHEpH7i0oqxRLtIOfbwhm2g4zElBLRBM-0t797g");
		params.put("userip", "143.167.10.89");
		params.put("as_scoring", "r");
		params.put("rsz", "8");

		Response r = sendWebSearchRequest(isDecodeQuery,params);
		if (r != null) {
			ResponseData data = r.getResponseData();
			if (data != null) {
				return data.getResults();
			}
		}
		return null;
	}
	

	public List<Result> searchBooks(boolean isDecodeQuery,String query)
	{
		Map<String, String> params = new LinkedHashMap<String, String>();
		
		params.put("q", query);
		
		Response r = sendBookSearchRequest(isDecodeQuery,params);
		
		return r.getResponseData().getResults();
	}
	

	public List<Result> searchImages(boolean isDecodeQuery,String query)
	{
		Map<String, String> params = new LinkedHashMap<String, String>();
		
		params.put("q", query);
		
		Response r = sendImageSearchRequest(isDecodeQuery,params);
		
		return r.getResponseData().getResults();
	}
	

	public List<Result> searchLocal(boolean isDecodeQuery,double lat, double lon, String query)
	{
		Map<String, String> params = new LinkedHashMap<String, String>();
		
		params.put("sll", lat + "," + lon);
		params.put("mrt", "localonly");
		
		if (query != null)
		{
			params.put("q", query);
		}
		
		Response r = sendLocalSearchRequest(isDecodeQuery,params);
		
		return r.getResponseData().getResults();
		
	}
	
	public List<Result> searchVideos(boolean isDecodeQuery,String query, OrderBy order)
	{
		Map<String, String> params = new LinkedHashMap<String, String>();
		
		params.put("q", query);
		
		if (order == null)
		{
			order = OrderBy.RELEVANCE;
		}
		
		params.put("orderBy", order.getValue());
		
		
		Response r = sendVideoSearchRequest(isDecodeQuery,params);
		
		return r.getResponseData().getResults();
	}

	protected Response sendVideoSearchRequest(boolean isDecodeQuery,Map<String, String> params)
	{
		return sendSearchRequest(isDecodeQuery,VIDEO_SEARCH_ENDPOINT, params);
	}

	public List<Result> searchBlogs(boolean isDecodeQuery,String query)
	{
		Map<String, String> params = new LinkedHashMap<String, String>();
		
		params.put("q", query);
		
		Response r = sendBlogSearchRequest(isDecodeQuery,params);
		
		return r.getResponseData().getResults();
	}

	protected Response sendBlogSearchRequest(boolean isDecodeQuery,Map<String, String> params)
	{
		return sendSearchRequest(isDecodeQuery,BLOG_SEARCH_ENDPOINT, params);
	}
	
}
