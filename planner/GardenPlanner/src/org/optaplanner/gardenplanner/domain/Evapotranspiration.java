package org.optaplanner.gardenplanner.domain;

import java.util.GregorianCalendar;

/**
 * Class to predict hourly evapotranspiration in mm using the FAO
 * Penman-Monteith equation and weather data
 *
 * Reference: Explanations: http://www.fao.org/3/x0490e/x0490e01.htm#preface
 * Equations:
 * https://www.researchgate.net/publication/237412886_Penman-Monteith_hourly_Reference_Evapotranspiration_Equations_for_Estimating_ETos_and_ETrs_with_Hourly_Weather_Data
 * https://doi.org/10.1016/j.biosystemseng.2014.10.011 (2.1)
 * 
 * @author Michael Hersam, Jannis Westermann, Philipp Wonner
 */
public class Evapotranspiration {
	/**
	 * predicts evapotranspiration rates using the FAO Penman-Monteith equation
	 * and weather data
	 * 
	 * @param kc
	 *            crop coefficient
	 * @param locationHeight
	 *            height of the location above sea level in meters
	 * @param hours
	 *            number of hours that should be predicted
	 * @param weather
	 *            current weather forecast
	 * 
	 * @return predicted evapotranspiration rate for every hour in mm
	 */
	public static Double[] calculateETc(double kc, double locationHeight, int hours, WeatherForecast weather) {
		// Reference crop evapotranspiration
		Double[] ET0 = new Double[hours];
		// Crop evapotranspiration under standard conditions
		Double[] ETc = new Double[hours];
		// air temperature in °C
		Double[] Thr = weather.getTemperature();
		// wind speed in m/s in 2m height
		Double[] U2 = weather.getWindSpeed();
		// the psychrometric constant
		double gamma = 0.054;
		// The albedo
		double alpha = 0.23;
		int dayOfYear = weather.getTimestamps().get(hours / 2).get(GregorianCalendar.DAY_OF_YEAR);
		// Latitude in radians
		double phi = Math.toRadians(weather.getLatitude());
		// Declination of the sun above the celestial equator in radians
		double smallDelta = 0.409 * Math.sin(((2 * Math.PI) / 365) * dayOfYear - 1.39);
		for (int i = 0; i < hours; i++) {
			// saturation vapour pressure at the air temperature Thr
			double e0 = 0.6108 * Math.exp((17.27 * Thr[i]) / (Thr[i] + 237.3));
			// actual average hourly vapour pressure (kPa)
			double ea = 0.6108 * Math.exp((17.27 * weather.getDewPoint()[i]) / (weather.getDewPoint()[i] + 237.3));
			// the slope of the saturation vapour pressure curve at Thr
			double delta = (4098 * (0.6108 * Math.exp((17.27 * Thr[i]) / (Thr[i] + 237.3))))
					/ (Math.pow((Thr[i] + 237.3), 2));

			// the net shortwave radiation
			double Rns = (1 - alpha) * weather.getSurfaceShortwaveRadiation()[i] * 0.0036;

			// hours of offset of local time zone and UTC, no DST
			int offset = weather.getTimestamps().get(i).get(GregorianCalendar.ZONE_OFFSET) / (1000 * 60 * 60);

			// Lm = station longitude in degrees, Lz = longitude of the local
			// time meridian
			double Lz, Lm;
			if (offset > 0) {
				Lz = 180 + 15 * offset;
				Lm = weather.getLongitude() + 180;
			} else {
				Lz = 15 * Math.abs(offset);
				Lm = weather.getLongitude();
			}

			// Local standard time in hours
			double t = (weather.getTimestamps().get(i).get(GregorianCalendar.HOUR_OF_DAY)
					+ weather.getTimestamps().get(i).get(GregorianCalendar.MINUTE) / 60.0 + 0.5) % 24;
			double b = (2 * Math.PI * (dayOfYear - 81)) / 364;
			// Solar time correction for wobble in Earth’s rotation
			double Sc = 0.1645 * Math.sin(2 * b) - 0.1255 * Math.cos(b) - 0.025 * Math.sin(b);
			// hour angle in radians
			double omega = (Math.PI / 12) * (((t - 0.5) + (Lz - Lm) / 15) - 12 + Sc);
			// hour angle 0.5 hours before omega in radians
			double omega1 = omega - Math.PI / 24;
			// hour angle 0.5 hours after omega in radians
			double omega2 = omega + Math.PI / 24;
			// Extraterrestrial radiation
			double Ra = (12 / Math.PI) * 60 * 0.082 * (1 + 0.033 * Math.cos((2 * Math.PI / 365) * dayOfYear))
					* ((omega2 - omega1) * Math.sin(phi) * Math.sin(smallDelta)
							+ Math.cos(phi) * Math.cos(smallDelta) * (Math.sin(omega2) - Math.sin(omega1)));

			// the clear-sky solar radiation
			double Rso = (0.75 * 0.00002 * locationHeight) * Ra;

			// solar altitude in degrees
			double beta = (180 / Math.PI) * Math.asin(
					Math.sin(phi) * Math.sin(smallDelta) + Math.cos(phi) * Math.cos(smallDelta) * Math.cos(omega));

			// cloudiness function of Rs and Rso
			double f;
			if (beta < 17.2) {
				f = -0.35;
			} else {
				f = (1.35 * (Math.max(0.3, Math.min(1.0, (weather.getSurfaceShortwaveRadiation()[i] * 0.0036) / Rso)))
						- 0.35);
			}
			// net longwave radiation
			double Rnl = 0.000000000204 * (Math.pow(Thr[i] + 273.16, 4)) * (0.34 - 0.14 * Math.sqrt(ea)) * f;
			// net radiation at the crop surface
			double Rn = Rns - Rnl;

			// soil heat flux density
			double Ghr;
			if (weather.getTimestamps().get(i).after(weather.getSunrise().get(i))
					&& weather.getTimestamps().get(i).before(weather.getSunset().get(i))) {
				// during daylight periods
				Ghr = 0.1 * Rn;
			} else {
				// during nighttime periods
				Ghr = 0.5 * Rn;
			}

			ET0[i] = (0.408 * delta * (Rn - Ghr)
					+ gamma * (37 / (weather.getTemperature()[i] + 273.16) * U2[i] * (e0 - ea)))
					/ (delta + gamma * (1 + 0.34 * U2[i]));
			ETc[i] = ET0[i] * kc;
		}
		return ETc;
	}
}
