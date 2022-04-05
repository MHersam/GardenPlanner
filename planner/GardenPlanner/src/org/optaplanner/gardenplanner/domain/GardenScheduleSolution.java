package org.optaplanner.gardenplanner.domain;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.optaplanner.core.api.domain.solution.PlanningEntityCollectionProperty;
import org.optaplanner.core.api.domain.solution.PlanningScore;
import org.optaplanner.core.api.domain.solution.PlanningSolution;
import org.optaplanner.core.api.score.buildin.hardsoft.HardSoftScore;
import org.optaplanner.gardenplanner.io.TemporaryDataIO;
import org.optaplanner.gardenplanner.io.GardenScheduleIO;

/**
 * @author Michael Hersam, Jannis Westermann, Philipp Wonner
 */
@PlanningSolution
public class GardenScheduleSolution {
	private List<WaterIntensity> waterIntensityList;
	private List<Shutter> 	shutterList;

	private double 			initialMoisture;
	private Double[] 		soilMoisture;
	private Double[] 		evapotranspiration;
	private HardSoftScore 	score;
	private Double[] 		precipitation;
	private WeatherForecast weather;
	private Integer[] 		uvLampPlan;
	private Integer[] 		heatingPlan;
	private Double[] 		lightBalance;
	private int 			hours;
	double 					sunToArtificialLightRatio;
	double 					artificialLightHours;

	// min/max number of sun hours per day that are healthy for the type of
	// plant
	private double plantMinSunHours, plantMaxSunHours;

	public GardenScheduleSolution() {
		super();
	}

	public GardenScheduleSolution(WeatherForecast weather, Double[] ETc, double initialMoisture, double plantMinSunHours,
			double plantMaxSunHours, double plantHeatingThreshold, double sunToArtificialLightRatio) {
		super();
		this.weather = weather;
		this.initialMoisture = initialMoisture;
		this.plantMaxSunHours = plantMaxSunHours;
		this.plantMinSunHours = plantMinSunHours;
		this.precipitation = weather.getPrecipitationAmount();
		this.hours = weather.getHours();
		this.evapotranspiration = ETc;
		this.sunToArtificialLightRatio = sunToArtificialLightRatio;
		soilMoisture = new Double[hours];
		waterIntensityList = new ArrayList<WaterIntensity>(hours);
		shutterList = new ArrayList<Shutter>(hours);
		uvLampPlan = new Integer[hours];
		
		for (int i = 0; i < hours; i++) {
			waterIntensityList.add(new WaterIntensity());
			waterIntensityList.get(i).setId(i);
			shutterList.add(new Shutter());
			shutterList.get(i).setId(hours + i);
			uvLampPlan[i] = 0;
		}
		this.heatingPlan = calculateHeatingPlan(plantHeatingThreshold);
	}
	

	private Double[] calculateSoilMoisture() {
		Double[] soilMoisture = new Double[hours];

		soilMoisture[0] = initialMoisture;
		soilMoisture[0] += 5 * waterIntensityList.get(0).getIntensity() - evapotranspiration[0];
		for (int i = 1; i < hours; i++) {
			soilMoisture[i] = soilMoisture[i - 1] - evapotranspiration[i];
			soilMoisture[i] += precipitation[i] * shutterList.get(i).getShutterState();
			soilMoisture[i] += 5 * waterIntensityList.get(i).getIntensity();

			// value range for moisture between 0 and 100
			if (soilMoisture[i] < 0) {
				soilMoisture[i] = 0d;
			}
			if (soilMoisture[i] > 100) {
				soilMoisture[i] = 100d;
			}
		}
		return soilMoisture;
	}

	private Double[] calculateLightBalance() {
		Double[] balance = new Double[hours];
		ArrayList<String> data = null;
		try {
			data = TemporaryDataIO.readTempData();
			balance[0] = Double.parseDouble(data.get(2 + GardenScheduleIO.elapsedHoursSinceLastPlan() + Integer.parseInt(data.get(1))));
		} catch (Exception e) {
			balance[0] = -plantMinSunHours;
		}

		for (int i = 1; i < hours; i++) {
			if (weather.getTimestamps().get(i).get(GregorianCalendar.HOUR_OF_DAY) == weather.getSunrise().get(i)
					.get(GregorianCalendar.HOUR_OF_DAY)) {
				balance[i] = -plantMinSunHours;
			} else {
				balance[i] = balance[i - 1] + weather.getSunHours()[i] * shutterList.get(i).getShutterState()
						+ (1 / sunToArtificialLightRatio) * uvLampPlan[i];
			}
		}

		return balance;
	}

	private Integer[] calculatUVLampPlan() {
		lightBalance = calculateLightBalance();
		int indexOfFirstAfterSunset = 0;
		int sunsetHour = weather.getSunset().get(hours/2).get(GregorianCalendar.HOUR_OF_DAY);
		int sunriseHour = weather.getSunrise().get(hours/2).get(GregorianCalendar.HOUR_OF_DAY);

		for (int i = 0; i < hours; i++) {
			int currentHourOfDay = weather.getTimestamps().get(i).get(GregorianCalendar.HOUR_OF_DAY);
			if (currentHourOfDay >= sunsetHour || currentHourOfDay < sunriseHour) {
				break;
			} else {
				indexOfFirstAfterSunset++;
			}
		}

		// plan artificial lighting between sunset and sunrise
		for (int i = indexOfFirstAfterSunset; i < hours; i++) {
			int currentHourOfDay = weather.getTimestamps().get(i).get(GregorianCalendar.HOUR_OF_DAY);
			if (lightBalance[i] < 0 && (currentHourOfDay > sunsetHour || currentHourOfDay < sunriseHour)) {
				uvLampPlan[i] = 1;
				lightBalance = calculateLightBalance();
			} else {
				uvLampPlan[i] = 0;
			}
		}

		return uvLampPlan;
	}

	private Integer[] calculateHeatingPlan(double plantHeatingThreshold) {
		Integer[] plan = new Integer[hours];
		for (int i = 0; i < plan.length; i++) {
			if (weather.getTemperature()[i] < plantHeatingThreshold) {
				plan[i] = 1;
			} else {
				plan[i] = 0;
			}
		}
		return plan;
	}

	@PlanningEntityCollectionProperty
	public List<WaterIntensity> getWaterIntensity() {
		return waterIntensityList;
	}

	public void setWaterIntensity(List<WaterIntensity> waterIntensity) {
		this.waterIntensityList = waterIntensity;
	}
	@PlanningEntityCollectionProperty
	public List<Shutter> getShutter() {
		return shutterList;
	}

	public void setShutterList(List<Shutter> shutterList) {
		this.shutterList = shutterList;
	}

	@PlanningScore
	public HardSoftScore getScore() {
		return score;
	}

	public void setScore(HardSoftScore score) {
		this.score = score;
	}

	public Double[] getSoilMoisture() {
		this.soilMoisture = calculateSoilMoisture();
		return soilMoisture;
	}

	public Integer[] getUvLampPlan() {
		this.uvLampPlan = calculatUVLampPlan();
		return uvLampPlan;
	}

	public Double[] getLightBalance() {
		this.lightBalance = calculateLightBalance();
		return lightBalance;
	}

	public Integer[] getHeatingPlan() {
		return heatingPlan;
	}

	public void setSoilMoisture(Double[] soilMoisture) {
		this.soilMoisture = soilMoisture;
	}

	public WeatherForecast getWeather() {
		return weather;
	}

	public double getPlantMinSunHours() {
		return plantMinSunHours;
	}

	public double getPlantMaxSunHours() {
		return plantMaxSunHours;
	}

	public int getHours() {
		return hours;
	}

	public Double[] getEvapotranspiration() {
		return evapotranspiration;
	}

	public double getArtificialLightHours() {
		return artificialLightHours;
	}
}
