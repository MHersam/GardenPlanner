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
public class WaterIntensity {
	Integer intensity = 0;

	@PlanningId
	int id;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	@ValueRangeProvider(id = "intensityRange")
	public CountableValueRange<Integer> getDelayRange() {
		return ValueRangeFactory.createIntValueRange(0, 3);
	}

	@PlanningVariable(valueRangeProviderRefs = { "intensityRange" })
	public Integer getIntensity() {
		return intensity;
	}

	public void setIntensity(Integer intensity) {
		this.intensity = intensity;
	}

}
