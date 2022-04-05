package org.optaplanner.gardenplanner.domain;

import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.optaplanner.gardenplanner.io.WeatherForecastIO;
import org.optaplanner.gardenplanner.other.DateConverter;

/**
 * Fetches weather forecast data from a web service, objects represent an hourly
 * forecast
 *
 * API reference:
 * https://developer.climacell.co/v3/reference#data-layers-weather
 * https://developer.climacell.co/v3/reference#get-hourly
 * 
 * @author Michael Hersam, Jannis Westermann, Philipp Wonner
 */
public class WeatherForecast {

	private int hours;
	// unit: mm/h
	private Double[] precipitationAmount;
	// unit: %
	private Integer[] precipitationProbability;
	// possible values: none, rain, snow, ice pellets, freezing rain
	private String[] precipitationType;
	// unit: �C
	private Double[] temperature;
	// temperature of the dew point, unit: �C
	private Double[] dewPoint;
	// unit: m/s
	private Double[] windGustSpeed;
	// unit: %
	private Double[] cloudCoverPercentage;
	// relative humidity, unit: %
	private Double[] humidity;
	// unit: m/s
	private Double[] windSpeed;
	// unit: w/sqm
	private Double[] surfaceShortwaveRadiation;
	// time in hours, per hour, that sunlight hits the ground, therefore values
	// between 0 and 1
	private Double[] sunHours;
	// contains calendars which are set to current time + index times 1 hour
	// offsets
	private ArrayList<GregorianCalendar> timestamps;
	private ArrayList<GregorianCalendar> sunrise;
	private ArrayList<GregorianCalendar> sunset;
	private JSONArray apiResponse;
	private String lat, lon;
	// number of tries to reaching api
	private int numberOfTries = 0;

	/**
	 * 
	 * @param hours
	 *            length of the forecast (max 96 hours)
	 * @param lat
	 *            coordinate
	 * @param lon
	 *            coordinate
	 */

	public WeatherForecast(int hours, String lat, String lon) {
		super();
		this.lat = lat;
		this.lon = lon;
		initDependingOnHours(hours);
	}

	private void initDependingOnHours(int hours) {
		this.hours = hours;
		this.precipitationAmount = new Double[hours];
		this.precipitationProbability = new Integer[hours];
		this.precipitationType = new String[hours];
		this.temperature = new Double[hours];
		this.dewPoint = new Double[hours];
		this.windGustSpeed = new Double[hours];
		this.cloudCoverPercentage = new Double[hours];
		this.humidity = new Double[hours];
		this.windSpeed = new Double[hours];
		this.surfaceShortwaveRadiation = new Double[hours];
		this.sunHours = new Double[hours];
		this.sunrise = new ArrayList<>(hours);
		this.sunset = new ArrayList<>(hours);
		this.timestamps = new ArrayList<>(hours);
	}

	/**
	 * fetches data from a weather API and sets the respective fields of the
	 * object with this data. retries a set amount of times, if api not
	 * reachable
	 * 
	 * @return true if request was successful, false if something went wrong
	 */
	public boolean retrieveCurrentWeatherForecast() {
		int skip = 0;
		try {
			numberOfTries++;
			apiResponse = WeatherForecastIO.readJsonFromUrl("https://api.climacell.co/v3/weather/forecast/hourly?lat="
					+ lat + "&lon=" + lon
					+ "&unit_system=si&start_time=now&fields=precipitation%2Cprecipitation_probability%2Cprecipitation_type%2Ctemp%2Cwind_gust%2Ccloud_cover%2Csunrise%2Csunset%2Chumidity%2Cwind_speed%2Csurface_shortwave_radiation%2Cdewpoint&apikey=zRBm5TNZ6lfyREudRluOG9mTHyyusz18");
			WeatherForecastIO.writeLatestForecast(apiResponse);
		} catch (JSONException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			// unable to connect to web service
			System.err.println("Web service unavailable");
			if (numberOfTries <= 3) {
				System.out.println("retrying...");
				try {
					Thread.sleep(5000);
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				return retrieveCurrentWeatherForecast();
			}
			try {
				// try reading last forecast from file
				apiResponse = WeatherForecastIO.readLastForecast();
				skip = determineNumberOfPastHours();
				initDependingOnHours(hours - skip);
			} catch (IOException e2) {
				e2.printStackTrace();
				return false;
			}
		}

		setVariablesWithJSONData(skip);
		return true;
	}

	/**
	 * when reading an old forecast from file, some of the entries may be in the
	 * past. we only want entries that are in the future
	 * 
	 * @return number of entries, that are in the past
	 */
	private int determineNumberOfPastHours() {
		int pastHours = 0;
		GregorianCalendar current = new GregorianCalendar();
		for (int i = 0; i < 96; i++) {
			JSONObject hour = new JSONObject(apiResponse.get(i).toString());
			JSONObject time = new JSONObject(hour.get("observation_time").toString());
			GregorianCalendar next = DateConverter.convertUnixToCalendar(time.getString("value"));
			if (current.before(next)) {
				// include last full hour in the plan
				pastHours -= 1;
				break;
			} else {
				pastHours++;
			}
		}
		return Math.max(0, pastHours);
	}

	/**
	 * writes the weather data from apiResponse to variables
	 * 
	 * @param offset
	 *            number of hours that should be skipped because they are in the
	 *            past
	 */
	private void setVariablesWithJSONData(int offset) {
		for (int i = 0; i < hours; i++) {
			JSONObject hour = new JSONObject(apiResponse.get(i + offset).toString());
			JSONObject precipitationAmount = new JSONObject(hour.get("precipitation").toString());
			this.precipitationAmount[i] = precipitationAmount.getDouble("value");
			JSONObject precipitationProbability = new JSONObject(hour.get("precipitation_probability").toString());
			this.precipitationProbability[i] = precipitationProbability.getInt("value");
			JSONObject precipitationType = new JSONObject(hour.get("precipitation_type").toString());
			this.precipitationType[i] = precipitationType.getString("value");
			JSONObject temperature = new JSONObject(hour.get("temp").toString());
			this.temperature[i] = temperature.getDouble("value");
			JSONObject dewPoint = new JSONObject(hour.get("dewpoint").toString());
			this.dewPoint[i] = dewPoint.getDouble("value");
			JSONObject windGusts = new JSONObject(hour.get("wind_gust").toString());
			this.windGustSpeed[i] = windGusts.getDouble("value");
			JSONObject cloudCover = new JSONObject(hour.get("cloud_cover").toString());
			this.cloudCoverPercentage[i] = cloudCover.getDouble("value");
			JSONObject humidity = new JSONObject(hour.get("humidity").toString());
			this.humidity[i] = humidity.getDouble("value");
			JSONObject windSpeed = new JSONObject(hour.get("wind_speed").toString());
			this.windSpeed[i] = windSpeed.getDouble("value");
			JSONObject surfaceShortwaveRadiation = new JSONObject(hour.get("surface_shortwave_radiation").toString());
			this.surfaceShortwaveRadiation[i] = surfaceShortwaveRadiation.getDouble("value");
			JSONObject sunrise = new JSONObject(hour.get("sunrise").toString());
			this.sunrise.add(DateConverter.convertUnixToCalendar(sunrise.getString("value")));
			JSONObject sunset = new JSONObject(hour.get("sunset").toString());
			this.sunset.add(DateConverter.convertUnixToCalendar(sunset.getString("value")));
			JSONObject time = new JSONObject(hour.get("observation_time").toString());
			this.timestamps.add(DateConverter.convertUnixToCalendar(time.getString("value")));
			if (this.timestamps.get(i).after(this.sunrise.get(i))
					&& this.timestamps.get(i).before(this.sunset.get(i))) {
				this.sunHours[i] = (100.0 - this.cloudCoverPercentage[i]) / 100;
			} else {
				this.sunHours[i] = 0.0;
			}
		}
	}

	public int getHours() {
		return hours;
	}
	public double getLatitude(){
		return Double.parseDouble(lat);
	}
	public double getLongitude(){
		return Double.parseDouble(lon);
	}

	public Double[] getPrecipitationAmount() {
		return precipitationAmount;
	}

	public Integer[] getPrecipitationProbability() {
		return precipitationProbability;
	}

	public String[] getPrecipitationType() {
		return precipitationType;
	}

	public Double[] getTemperature() {
		return temperature;
	}
	public Double[] getDewPoint() {
		return dewPoint;
	}

	public Double[] getWindGustSpeed() {
		return windGustSpeed;
	}

	public Double[] getSunHours() {
		return sunHours;
	}

	public Double[] getCloudCoverPercentage() {
		return cloudCoverPercentage;
	}
	
	public Double[] getHumidity() {
		return humidity;
	}

	public Double[] getWindSpeed() {
		return windSpeed;
	}

	public Double[] getSurfaceShortwaveRadiation() {
		return surfaceShortwaveRadiation;
	}

	public ArrayList<GregorianCalendar> getSunrise() {
		return sunrise;
	}

	public ArrayList<GregorianCalendar> getSunset() {
		return sunset;
	}

	public ArrayList<GregorianCalendar> getTimestamps() {
		return timestamps;
	}
}
