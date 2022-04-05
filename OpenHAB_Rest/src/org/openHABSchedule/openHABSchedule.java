package org.openHABSchedule;

import org.openHABRest.*;

import org.json.JSONArray;
import org.json.JSONObject;

//import java.io.File;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashMap;

/**
 * @author Michael Hersam, Jannis Westermann, Philipp Wonner
 */

public class openHABSchedule {
	private openHABRest openHAB;
	//private File file;
	
	
	public openHABSchedule(openHABRest o) {
		this.openHAB = o;
		//this.file = Util.urlToFile(Util.getLocation(openHABSchedule.class));
	}
	
	
	/**
	 * Execute the scheduler function in regular interval
	 * 
	 * @param items		Name of openHAB items
	 * @param Scheduler 
	 * @param interval	Update interval in minutes
	 * @param steps		# of time steps in the schedule
	 */
	public void startScheduleUpdates(String[] items, IFunctionPointer scheduler, int interval, int steps) {
		
		int intervalMilliSeconds = interval * 60000;
		HashMap<String, Object[]> schedule = new HashMap<String, Object[]>();
		for (String item : items) {
			schedule.put(item, new Object[steps]);
		}
		int timeIndex = 0;
		
		while (true) {
			boolean ok = scheduler.execute(schedule);
			
			if (ok) {
				timeIndex = 0;
			}
			
			// If schedule steps are left
			if (timeIndex < schedule.get(items[0]).length) {
				
				for (String item : items) {
					this.openHAB.setItem(item, schedule.get(item)[timeIndex]);
				}
				
			}
			//else go to sleep
			try {
				Thread.sleep(intervalMilliSeconds);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
}
