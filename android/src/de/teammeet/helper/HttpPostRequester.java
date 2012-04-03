/**
 *    Copyright 2012 Daniel Kreischer, Christopher Holm, Christopher Schwardt
 *
 *    This file is part of TeamMeet.
 *
 *    TeamMeet is free software: you can redistribute it and/or modify
 *    it under the terms of the GNU General Public License as published by
 *    the Free Software Foundation, either version 3 of the License, or
 *    (at your option) any later version.
 *
 *    TeamMeet is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *    GNU General Public License for more details.
 *
 *    You should have received a copy of the GNU General Public License
 *    along with TeamMeet.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

package de.teammeet.helper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.apache.http.HttpVersion;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.HttpHostConnectException;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpParams;
import org.apache.http.params.HttpProtocolParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class HttpPostRequester {

	private static final String	CLASS			= HttpPostRequester.class.getSimpleName();

	// TODO maybe use Http(s)URLConnection
	// TODO check if the cookies are saved over new instantiations
	// private ClientConnectionManager connectionManager = new
	// ThreadSafeClientConnManager(new DefaultedHttpParams(null, null),);
	private static HttpClient	mHttpClient		= null;
	// private HttpClient mHttpClient;
	private String				mUrl				= null;
	private List<NameValuePair>	mPostParameter	= null;

	public HttpPostRequester(final String url) {
		if (mHttpClient == null) {
			// sets up parameters
			final HttpParams params = new BasicHttpParams();
			HttpProtocolParams.setVersion(params, HttpVersion.HTTP_1_1);
			HttpProtocolParams.setContentCharset(params, "utf-8");
			params.setBooleanParameter("http.protocol.expect-continue", false);

			// registers schemes for both http and https
			final SchemeRegistry registry = new SchemeRegistry();
			registry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
			final SSLSocketFactory sslSocketFactory = SSLSocketFactory.getSocketFactory();
			sslSocketFactory.setHostnameVerifier(SSLSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
			registry.register(new Scheme("https", sslSocketFactory, 443));

			final ThreadSafeClientConnManager manager = new ThreadSafeClientConnManager(params, registry);
			mHttpClient = new DefaultHttpClient(manager, params);
		}

		this.mUrl = url;
		// mHttpClient = new DefaultHttpClient();
		mPostParameter = new ArrayList<NameValuePair>();
	}

	public HttpPostRequester(final String url, final Map<String, String> postParameterMap) {
		this(url);
		addPostParameterMap(postParameterMap);
	}

	public void addPostParameter(final String key, final String value) {
		mPostParameter.add(new BasicNameValuePair(key, value));
	}

	public void addPostParameterMap(final Map<String, String> map) {
		for (final String key : map.keySet()) {
			addPostParameter(key, map.get(key));
		}
	}

	public String request() throws ClientProtocolException, IOException, HttpHostConnectException {
		String content = null;
		String decodedContent = null;
		final HttpPost httppost = new HttpPost(mUrl);
		if (!mPostParameter.isEmpty()) {
			httppost.setEntity(new UrlEncodedFormEntity(mPostParameter));
		}
		final ResponseHandler<String> responseHandler = new BasicResponseHandler();
		content = mHttpClient.execute(httppost, responseHandler);
		// TODO: unquote the returned string
		decodedContent = URLDecoder.decode(content);
		/*
		 * HttpResponse response = mHttpClient.execute(httppost); HttpEntity
		 * entity = response.getEntity(); if (entity != null) { content =
		 * convertStreamToString(entity.getContent()); } else { throw new
		 * IOException("HTTP response doesn't contain entity!"); }
		 */
		return decodedContent;
	}

	@SuppressWarnings("unchecked")
	public Map requestJSON() {
		String content = null;
		Map jsonMap;
		try {
			content = request();
			if (content.startsWith("[]") || content.startsWith("{}")) {
				jsonMap = new Hashtable<String, String>();
			} else {
				jsonMap = convertJsonStringToMap(content);
			}
		} catch (final Exception e) {
			e.printStackTrace();
			jsonMap = new Hashtable<String, String>();
			String errorMessage = e.getMessage();
			if (content != null) {
				errorMessage += "\nContent was:" + content.toString();
			}
			jsonMap.put("error", errorMessage);
		}
		return jsonMap;
	}

	@SuppressWarnings("unchecked")
	private Map convertJsonStringToMap(final String content) throws JSONException {
		Map jsonMap = null;
		final JSONObject jsonObj = new JSONObject(content);
		if (jsonObj.has("error")) {
			jsonMap = new Hashtable<String, String>();
			jsonMap.put("error", jsonObj.get("error"));
		} else {
			jsonMap = recursiveJsonConversion(jsonObj);
		}
		return jsonMap;
	}

	/**
	 * This is really an evil hack to generically parse a JSON string into a
	 * map. The Caller needs to know the correct structure and the structure
	 * must be symmetric.
	 */
	@SuppressWarnings("unchecked")
	private Map recursiveJsonConversion(final JSONObject jsonObj) throws JSONException {
		Map jsonMap = new HashMap();
		final JSONArray jsonNamesArray = jsonObj.names();
		// JSONArray jsonArray = jsonObj.toJSONArray(jsonNamesArray);
		JSONObject curJsonObj;
		boolean first = true;
		boolean isString = false;
		String key;
		if (jsonNamesArray != null) {
			for (int i = 0; i < jsonNamesArray.length(); i++) {
				key = jsonNamesArray.getString(i);
				curJsonObj = jsonObj.optJSONObject(key);
				if (first) {
					if (curJsonObj != null) {
						jsonMap = new Hashtable<String, Map>();
					} else {
						jsonMap = new Hashtable<String, String>();
						isString = true;
					}
					first = false;
				}
				if (curJsonObj != null && !isString) {
					// curJsonObj = jsonObj.getJSONObject(key);
					jsonMap.put(key, recursiveJsonConversion(curJsonObj));
				} else if (curJsonObj == null && isString) {
					final String val = jsonObj.optString(key, "");
					jsonMap.put(key, val);
				} else {
					Log.e(CLASS, "json parse error: isString=" + isString + "curJsonObj" + curJsonObj);
					throw new JSONException("Unable to parse JSON string '" + jsonObj.toString() + "'!");
				}
			}
		} else {
			Log.e(CLASS, "json objects names() array is null.");
		}
		return jsonMap;
	}

	@SuppressWarnings("unused")
	private String convertStreamToString(final InputStream is) {
		/*
		 * To convert the InputStream to String we use the
		 * BufferedReader.readLine() method. We iterate until the BufferedReader
		 * return null which means there's no more data to read. Each line will
		 * appended to a StringBuilder and returned as String.
		 */
		final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		final StringBuilder sb = new StringBuilder();

		String line = null;
		try {
			while ((line = reader.readLine()) != null) {
				sb.append(line + "\n");
			}
		} catch (final IOException e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (final IOException e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public void clearCookies() {
		mHttpClient = null;
	}
}
