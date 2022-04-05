package org.optaplanner.gardenplanner.io;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.optaplanner.gardenplanner.domain.GardenScheduleSolution;

/**
 * Helper class to save and retrieve calculated WateringSchedules to/from CSV
 * files
 * 
 * @author Michael Hersam, Jannis Westermann, Philipp Wonner
 */
public class GardenScheduleIO {
	private final static String PATH = System.getProperty("user.dir") + File.separator + "res" + File.separator
			+ "latest_plan.csv";

	/**
	 * writes the timestamps and intensity to
	 * ../Opta_Watering/src/resources/latest_plan.csv
	 * 
	 * @param solution
	 *            the solved solution
	 * @throws IOException
	 */
	public static void writePlan(GardenScheduleSolution solution) throws IOException {
		ArrayList<String> rows = new ArrayList<>();
		for (int i = 0; i < solution.getWaterIntensity().size(); i++) {
			rows.add(solution.getWeather().getTimestamps().get(i).getTime().toString() + ","
					+ String.valueOf(solution.getWaterIntensity().get(i).getIntensity()) + ","
					+ String.valueOf(solution.getShutter().get(i).getShutterState()) + ","
					+ solution.getUvLampPlan()[i] + "," + solution.getHeatingPlan()[i]);
		}
		FileWriter csvWriter = new FileWriter(PATH);
		csvWriter.append("Time");
		csvWriter.append(",");
		csvWriter.append("Watering intensity");
		csvWriter.append(",");
		csvWriter.append("Shutters");
		csvWriter.append(",");
		csvWriter.append("UV-Lamps");
		csvWriter.append(",");
		csvWriter.append("Heating");
		csvWriter.append("\n");

		for (String rowData : rows) {
			csvWriter.append(String.join(",", rowData));
			csvWriter.append("\n");
		}

		csvWriter.flush();
		csvWriter.close();
	}

	/**
	 * reads last plan that was calculated
	 * 
	 * @return HashMap with following keys: "Time", "Intensity", "Shutters",
	 *         "Lamps", "Heating". Values are all ArrayLists with Integers
	 *         except for "Time", which is a ArrayList with GregorianCalendar
	 *         objects
	 * @throws Exception
	 */
	public static HashMap<String, List<?>> readPlan() throws IOException {
		List<List<String>> records = new ArrayList<>();
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(PATH));
			String line;
			// skip header line
			br.readLine();

			while ((line = br.readLine()) != null) {
				String[] values = line.split(",");
				records.add(Arrays.asList(values));
			}
			br.close();
		} catch (IOException e) {
			throw e;
		}
		HashMap<String, List<?>> plan = new HashMap<>();

		ArrayList<GregorianCalendar> time = new ArrayList<>();
		ArrayList<Integer> intensity = new ArrayList<>();
		ArrayList<Integer> shutters = new ArrayList<>();
		ArrayList<Integer> lamps = new ArrayList<>();
		ArrayList<Integer> heating = new ArrayList<>();

		for (int i = 0; i < records.size(); i++) {
			GregorianCalendar cal = new GregorianCalendar();
			SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
			try {
				cal.setTime(sdf.parse(records.get(i).get(0)));
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			time.add(cal);
			intensity.add(Integer.parseInt(records.get(i).get(1)));
			shutters.add(Integer.parseInt(records.get(i).get(2)));
			lamps.add(Integer.parseInt(records.get(i).get(3)));
			heating.add(Integer.parseInt(records.get(i).get(4)));
		}
		plan.put("Time", time);
		plan.put("Intensity", intensity);
		plan.put("Shutters", shutters);
		plan.put("Lamps", lamps);
		plan.put("Heating", heating);
		return plan;
	}
	/**
	 * calculates the elapsed time between calculation of the last plan and now
	 * @return number of elapsed hours
	 * @throws Exception 
	 */
	public static int elapsedHoursSinceLastPlan() throws Exception{
		HashMap<String, List<?>> lastPlan = null;
		try {
			lastPlan = readPlan();
		} catch (IOException e) {
			throw e;
		}
		long currentTimeMillis = new GregorianCalendar().getTimeInMillis();
		GregorianCalendar lastPlanCal = (GregorianCalendar) lastPlan.get("Time").get(0);
		long lastPlanTimeMillis = lastPlanCal.getTimeInMillis();
		long secondsBetween = (currentTimeMillis - lastPlanTimeMillis) / 1000;
		int elapsedHours =  (int) (secondsBetween / 3600);
		if(elapsedHours >= lastPlan.get("Time").size()){
			throw new Exception("Last Plan is too old to be used");
		}
		return elapsedHours;
	}
}
