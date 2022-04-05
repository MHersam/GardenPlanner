package org.optaplanner.gardenplanner.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;

import org.json.JSONArray;
import org.json.JSONException;

/**
 * @author Michael Hersam, Jannis Westermann, Philipp Wonner
 */
public class WeatherForecastIO {

	private final static String PATH = System.getProperty("user.dir") + File.separator + "res" + File.separator + "last_weather_forecast.json";

	/**
	 * 
	 * @return api response saved in
	 *         ../Opta_Watering/src/resources/last_weather_forecast.json as
	 *         JSONArray
	 * @throws IOException
	 */
	public static JSONArray readLastForecast() throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(PATH), "UTF8"));
		String content = "";
		while (in.ready()) {
			content += in.readLine();
		}
		JSONArray json = new JSONArray(content);
		in.close();
		return json;
	}

	/**
	 * saves the passed json in
	 * ../Opta_Watering/src/resources/last_weather_forecast.json for potential
	 * later use
	 * 
	 * @param response
	 *            got from calling the weather API
	 * @throws IOException
	 */
	public static void writeLatestForecast(JSONArray response) throws IOException {
		File f = new File(PATH);
		f.createNewFile();
		BufferedWriter out = new BufferedWriter(new FileWriter(PATH));
		out.write(response.toString());
		out.close();

	}
	
	public static JSONArray readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONArray json = new JSONArray(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}
}
