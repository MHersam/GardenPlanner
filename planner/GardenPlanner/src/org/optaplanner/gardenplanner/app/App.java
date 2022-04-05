package org.optaplanner.gardenplanner.app;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

import org.apache.commons.io.FileUtils;
import org.optaplanner.core.api.solver.Solver;
import org.optaplanner.core.api.solver.SolverFactory;
import org.optaplanner.gardenplanner.domain.Evapotranspiration;
import org.optaplanner.gardenplanner.domain.GardenScheduleSolution;
import org.optaplanner.gardenplanner.domain.WeatherForecast;
import org.optaplanner.gardenplanner.io.GardenScheduleIO;
import org.optaplanner.gardenplanner.io.TemporaryDataIO;

import org.openHABRest.*;
import org.openHABSchedule.*;

/**
 * @author Michael Hersam, Jannis Westermann, Philipp Wonner
 */
public class App implements IFunctionPointer {
	
	private openHABRest openHAB;
	private openHABSchedule openHABSchedule;
	
	//TODO: retrieve these values from openHAB
	private String lat = "48.783333";
	private String lon = "9.18333";
	// number of hours the schedule should cover
	private int hours = 96;
	// height of the location above sea level in meters
	private double locationHeight = 300;
	// predicted or sensed soil moisture at runtime
	private double initialMoisture = 50;
	// min/max number of sun hours per day that are healthy for the type of
	// plant
	private double plantMaxSunHours = 8.0;
	private double plantMinSunHours = 4.0;
	// heating should be turned on if air temperature under this value
	private double plantHeatingThreshold = 24.0;
	// in other words how many artificial light hours are needed to equal
	// one hour of sun light
	private double sunToArtificialLightRatio = 2.0;
	
	public static void main(String[] args) {

    	App app = new App();
    	app.openHAB = new openHABRest();
    	app.openHABSchedule = new openHABSchedule(app.openHAB);
		
		// create directory for generated files, if it does not exist already
		new File(System.getProperty("user.dir") + File.separator + "res").mkdir();

		String[] items = {"water_amount",
							"water_pump",
							"light",
							"heating",
							"shutter"};
		app.openHABSchedule.startScheduleUpdates(items, app, 60, app.hours);

	}

	@Override
	public boolean execute(HashMap<String, Object[]> schedule) {
		
		WeatherForecast weather = new WeatherForecast(this.hours, this.lat, this.lon);
		Boolean success = weather.retrieveCurrentWeatherForecast();		
		if (!success) {
			// TODO:alternative routine without forecast
			return false;
		}
		// Set the current weather values to the local sensor data
		weather.getTemperature()[0] = this.openHAB.getNumber("temperature");
		weather.getHumidity()[0] = this.openHAB.getNumber("air_humidity");
		weather.getSunHours()[0] = this.openHAB.getNumber("sun_hours");

		Double[] ETc = Evapotranspiration.calculateETc(1.0, this.locationHeight, this.hours, weather);
		
		this.initialMoisture = this.openHAB.getNumber("soil_humidity");


		SolverFactory<GardenScheduleSolution> solverFactory = SolverFactory
				.createFromXmlResource("resources/WateringSolverConfig.xml");
		Solver<GardenScheduleSolution> solver = solverFactory.buildSolver();
		GardenScheduleSolution unsolvedProblem = new GardenScheduleSolution(weather, ETc, this.initialMoisture, this.plantMinSunHours,
				this.plantMaxSunHours, this.plantHeatingThreshold, this.sunToArtificialLightRatio);
		GardenScheduleSolution solvedProblem = solver.solve(unsolvedProblem);

		try {
			GardenScheduleIO.writePlan(solvedProblem);
			TemporaryDataIO.writeTempData(solvedProblem);
		} catch (IOException e) {
			System.err.println("Failed to write data to disk!");
			//e.printStackTrace();
			//ignore for now
		}
		
		//return schedule to openHAB interface
		for (int i = 0; i < this.hours; i++) {
			schedule.get("water_amount")[i] = solvedProblem.getWaterIntensity().get(i).getIntensity();
			schedule.get("water_pump")[i] = solvedProblem.getWaterIntensity().get(i).getIntensity() > 0 ? 1 : 0;
			schedule.get("shutter")[i] 		= solvedProblem.getShutter().get(i).getShutterState();
		}
		schedule.put("light", solvedProblem.getUvLampPlan());
		schedule.put("heating", solvedProblem.getHeatingPlan());
		
		
		// DEBUGGING
		// ...
		System.out.println("Calculated: ");
		System.out.print("planned water intensity: ");
		for (int i = 0; i < solvedProblem.getWaterIntensity().size(); i++) {
			System.out.print(solvedProblem.getWaterIntensity().get(i).getIntensity() + ", ");
		}
		System.out.print("\n");
		System.out.print("shutter states: ");
		for (int i = 0; i < solvedProblem.getShutter().size(); i++) {
			System.out.print(solvedProblem.getShutter().get(i).getShutterState() + ", ");
		}
		System.out.print("\n");
		System.out.println("natural sun time per hour: " + Arrays.toString(solvedProblem.getWeather().getSunHours()));
		System.out.println("predicted soil moisture: " + Arrays.toString(solvedProblem.getSoilMoisture()));
		System.out.println("ETc: " + Arrays.toString(ETc));
		double sum = 0;
		for (int i = 0; i < ETc.length; i++) {
			sum += ETc[i];
		}
		System.out.println("ETc sum over " + weather.getHours() + " hours: " + sum + "mm");
		System.out.println("Weather data:");
		System.out.println("precipitation amount: " + Arrays.toString(weather.getPrecipitationAmount()));
		System.out.println("precipitation probability: " + Arrays.toString(weather.getPrecipitationProbability()));
		System.out.println("precipitation type: " + Arrays.toString(weather.getPrecipitationType()));
		System.out.println("temperature: " + Arrays.toString(weather.getTemperature()));
		System.out.println("cloud cover percentage: " + Arrays.toString(weather.getCloudCoverPercentage()));
		System.out.println("humidity: " + Arrays.toString(weather.getHumidity()));
		System.out.println("wind speed: " + Arrays.toString(weather.getWindSpeed()));
		System.out.println("surface shortwave radiation: " + Arrays.toString(weather.getSurfaceShortwaveRadiation()));

		// archive all generated files for later investigations
		File target = new File(System.getProperty("user.dir") + File.separator + "res_archive");
		target.mkdir();
		target = new File(System.getProperty("user.dir") + File.separator + "res_archive" + File.separator
				+ solvedProblem.getWeather().getTimestamps().get(0).getTime().getTime());
		target.mkdir();
		try {
			FileUtils.copyDirectory(new File(System.getProperty("user.dir") + File.separator + "res"), target);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// DEBUGGING END
		
		return true;
	}

}
