package org.optaplanner.gardenplanner.domain;

import org.optaplanner.core.api.domain.entity.PlanningEntity;
import org.optaplanner.core.api.domain.lookup.PlanningId;
import org.optaplanner.core.api.domain.valuerange.CountableValueRange;
import org.optaplanner.core.api.domain.valuerange.ValueRangeFactory;
import org.optaplanner.core.api.domain.valuerange.ValueRangeProvider;
import org.optaplanner.core.api.domain.variable.PlanningVariable;

/**
 * @author Michael Hersam, Jannis Westermann, Philipp Wonner
 */
@PlanningEntity
public class Shutter {
	// 0 = shutters closed, 1 = shutters open
	Integer shutterState = 1;

	@PlanningId
	int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@ValueRangeProvider(id = "shutterRange")
	public CountableValueRange<Integer> getShutterRange() {
		return ValueRangeFactory.createIntValueRange(0, 1);
	}

	@PlanningVariable(valueRangeProviderRefs = { "shutterRange" })
	public Integer getShutterState() {
		return shutterState;
	}

	public void setShutterState(Integer shutter) {
		this.shutterState = shutter;
	}
}
