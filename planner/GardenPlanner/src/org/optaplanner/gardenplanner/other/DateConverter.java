package org.optaplanner.gardenplanner.other;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.TimeZone;

/**
 * Helper class for converting date strings to java objects
 * 
 * @author Michael Hersam, Jannis Westermann, Philipp Wonner
 *
 */
public class DateConverter {
	/**
	 * 
	 * @param dateString
	 *            format: "yyyy-mm-ddTHH:MM:SSSZ" or "yyyy-mm-dd HH:MM:SSS"
	 * @return GregorianCalendar object with time set to moment of string (in GMT) in local time zone
	 */
	public static GregorianCalendar convertUnixToCalendar(String dateString) {
		GregorianCalendar calendar = new GregorianCalendar();
		dateString = dateString.replace('T', ' ').replace('Z', Character.MIN_VALUE);
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
		sdf.setTimeZone(TimeZone.getTimeZone("GMT"));		
		try {
			calendar.setTime(sdf.parse(dateString));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return calendar;
	}
}
