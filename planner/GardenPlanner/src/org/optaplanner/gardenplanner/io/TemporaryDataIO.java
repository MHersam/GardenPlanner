package org.optaplanner.gardenplanner.io;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.optaplanner.gardenplanner.domain.GardenScheduleSolution;

/**
 * Class saves data needed in later program runs, loads this data if necessary.
 * examples are sun hours that already happened for this day, predicted soil
 * moisture for the initial moisture value
 * 
 * @author Michael Hersam, Jannis Westermann, Philipp Wonner
 *
 */
public class TemporaryDataIO {
	static String PATH = System.getProperty("user.dir") + File.separator + "res" + File.separator + "data.tmp";

	public static ArrayList<String> readTempData() throws IOException {
		BufferedReader in = new BufferedReader(new InputStreamReader(new FileInputStream(PATH), "UTF8"));
		ArrayList<String> dataList = new ArrayList<>();
		while (in.ready()) {
			dataList.add(in.readLine());
		}
		in.close();
		return dataList;
	}

	public static void writeTempData(GardenScheduleSolution schedule) throws IOException {
		Double[] lightBalance = schedule.getLightBalance();
		File f = new File(PATH);

		if (f.exists()) {

		}
		f.createNewFile();
		BufferedWriter out = new BufferedWriter(new FileWriter(PATH));
		int hours = schedule.getHours();
		out.write(schedule.getWeather().getTimestamps().get(0).getTime().toString() + "\n");
		out.write(hours + "\n");
		for (int i = 0; i < hours; i++) {
			// remove manipulations for use as initial moisture value in later
			// program runs
			out.write(String.valueOf(schedule.getSoilMoisture()[i] + schedule.getEvapotranspiration()[i]
					- schedule.getWeather().getPrecipitationAmount()[i]
							* schedule.getShutter().get(i).getShutterState())
					+ "\n");
		}
		for (int i = 0; i < hours; i++) {
			out.write(String.valueOf(lightBalance[i]) + "\n");
		}
		out.close();

	}
}
