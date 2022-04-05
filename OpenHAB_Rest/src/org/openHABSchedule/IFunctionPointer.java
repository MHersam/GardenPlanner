package org.openHABSchedule;

import java.util.HashMap;

/**
 * @author Michael Hersam, Jannis Westermann, Philipp Wonner
 */

public interface IFunctionPointer {
	public boolean execute(HashMap<String, Object[]> schedule);
}
