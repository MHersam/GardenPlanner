#fallback_system.py
import sensors
import devices
import globals

import time
'''
Fallback Sytem:
Use this if CSP-Planer is not aviable (for eg. Internet/Weatherforecast is not working use this Fallback System)

Manages the Garden in an unintelligent way without forecasts
'''

watering_minimum_moisture = 60
watering_latest_watering_timestamp = 0.0
watering_wait_time_after_watering = 60*10
watering_wateramount = 200

def checkGarden():
	#globals
	global watering_minimum_moisture
	global watering_latest_watering_timestamp
	global watering_wait_time_after_watering
	global watering_wateramount
	
	current_time = time.time()
	
	#Check and do: Watering
	#First check if enough time has past since the last watering that watering could reach the sensor
	if (watering_latest_watering_timestamp == 0.0) or (watering_latest_watering_timestamp + watering_wait_time_after_watering < current_time):
		#check if sesor is working and soil humidity is under minimum
		if (sensors.smoothed_soilhumidity < watering_minimum_moisture) and (sensors.measure_failures_SoilMoisture < 3) and (sensors.isWaterEmpty() == 0):
			#Water the Garden with the given amount and set new watering time stamp
			devices.controlWaterpump(watering_wateramount)
			watering_latest_watering_timestamp = time.time()
			globals.display_cmd.print_displ("Fallback System: Watered the Garden")
