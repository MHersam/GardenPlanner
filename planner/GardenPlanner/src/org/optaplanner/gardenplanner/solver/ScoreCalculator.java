package org.optaplanner.gardenplanner.solver;

import java.util.ArrayList;
import java.util.GregorianCalendar;

import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.core.impl.score.director.easy.EasyScoreCalculator;
import org.optaplanner.gardenplanner.domain.Shutter;
import org.optaplanner.gardenplanner.domain.WaterIntensity;
import org.optaplanner.gardenplanner.domain.WeatherForecast;
import org.optaplanner.gardenplanner.domain.GardenScheduleSolution;

/**
 * Used during the planning process to score solutions
 * 
 * @author Michael Hersam, Jannis Westermann, Philipp Wonner
 */
public class ScoreCalculator implements EasyScoreCalculator<GardenScheduleSolution> {

	@Override
	public HardSoftScore calculateScore(GardenScheduleSolution waterschedule) {
		int hard, soft;
		hard = 0;
		soft = 0;

		// out of desired soil moisture range penalty
		for (Double moisture : waterschedule.getSoilMoisture()) {
			if (moisture < 40 || moisture > 60)
				soft -= 10;
			if (moisture < 30 || moisture > 70)
				hard -= 10;
		}

		// penalty for irrigation, (rain should be used to water plants if
		// possible)
		for (WaterIntensity waterIntensity : waterschedule.getWaterIntensity()) {
			soft -= waterIntensity.getIntensity() * 10;
		}

		// penalty for using artificial light
		for (Integer light : waterschedule.getUvLampPlan()) {
			soft -= 5 * light;
		}

		/*
		 * Penalize watering between sunrise and sunset when cloud cover lower
		 * than 80% for the next 2 hours
		 */
		WeatherForecast weather = waterschedule.getWeather();
		for (int i = 0; i < waterschedule.getWaterIntensity().size(); i++) {
			// if watering is planned for this hour
			if (waterschedule.getWaterIntensity().get(i).getIntensity() != 0) {
				// if time between sunrise and sunset
				if (weather.getTimestamps().get(i).after(weather.getSunrise().get(i))
						&& weather.getTimestamps().get(i).before(weather.getSunset().get(i))) {
					// if not cloudy
					if (weather.getCloudCoverPercentage()[i] < 90) {
						hard -= 20;
					}
					// avoiding out of bounds
					if (weather.getHours() == i + 1) {
						continue;
					}
					if (weather.getCloudCoverPercentage()[i + 1] < 90) {
						hard -= 10;
					}
				}
			}
		}

		// discourage watering, when soil is warm
		for (int i = 0; i < waterschedule.getWaterIntensity().size(); i++) {
			if (waterschedule.getWaterIntensity().get(i).getIntensity() != 0) {
				double temp = weather.getTemperature()[i];
				if (temp >= 20) {
					soft -= 5;
				}
				if (temp >= 25) {
					soft -= 5;
				}
				if (temp >= 30) {
					hard -= 10;
				}
			}

		}

		// encourage closing shutters to protect plants from bad weather
		for (int i = 0; i < waterschedule.getShutter().size(); i++) {
			Integer shutterState = waterschedule.getShutter().get(i).getShutterState();
			// react to ice pellets
			if (shutterState == 1 && weather.getPrecipitationType()[i].equals("ice pellets")) {
				hard -= weather.getPrecipitationProbability()[i];
			}
			// react to strong wind gusts
			if (shutterState == 1 && weather.getWindGustSpeed()[i] > 14) {
				soft -= 10;
			}
			if (shutterState == 1 && weather.getWindGustSpeed()[i] > 19) {
				hard -= 10;
			}
		}

		for (Shutter state : waterschedule.getShutter()) {
			if (state.getShutterState() == 0) {
				soft -= 1;
			}
		}

		// keep sun hours between min and max values
		Double[] sunBalanceHourly = waterschedule.getLightBalance();
		ArrayList<Double> sunBalanceDailyList = new ArrayList<>();
		for (int i = 0; i < weather.getHours(); i++) {
			if (weather.getTimestamps().get(i).get(GregorianCalendar.HOUR_OF_DAY) == weather.getSunrise().get(i)
					.get(GregorianCalendar.HOUR_OF_DAY) && i > 0) {
				sunBalanceDailyList.add(sunBalanceHourly[i - 1]);
			} else if (i == weather.getHours() - 1) {
				sunBalanceDailyList.add(sunBalanceHourly[i]);
			}
		}
		for (Double balance : sunBalanceDailyList) {
			if (balance > (waterschedule.getPlantMaxSunHours() - waterschedule.getPlantMinSunHours())) {
				hard -= Math.rint(
						(balance - (waterschedule.getPlantMaxSunHours() - waterschedule.getPlantMinSunHours())) * 10);
			}
		}

		return HardSoftScore.of(hard, soft);
	}

}
