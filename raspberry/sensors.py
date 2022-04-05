#sensors.py

import globals
import time
from random import randint
if globals.SIMULATE:
	import sensor_fake


'''
This Modul handles the Raspberry sensors and provides the data
'''

#counters if sensor measurement fails. Should be handeled if sensors are defect
measure_failures_TempAirHum = 100
measure_failures_Light = 100
measure_failures_SoilMoisture = 100

#smoothed sensordata: newval = 0.7 x actval + 0.3 * oldval. may change 0.7/0.3
smooth_factor = 0.4
smoothed_humidity = -100.0
smoothed_temperature = -100.0
smoothed_soilhumidity = -100.0
water_latest_measurement = 0

#soil moisture callibration
soilmoisture_min = 253.0
soilmoisture_max = 520.0	#maybe use 490 instead, seems to depend on soil / tomato fresh watered had 390
soilmoisture_smoothed_cur =-100

#Light values
light_wh_m2 = 0.0               #watthours per squermeter
light_akt_lux = 0.0             #lux at latest measurment
light_kilo_luxh = 0.0           #kilo lux hours
light_light_hours = 0.0         #lighthours, counts if watt/m2 > 120
light_latest_measurement = 0
light_last_day = time.localtime().tm_yday   #for checking if day has changed

'''
Measures temperature and humidity of the Air
Returns:
    Temperature and Humidity
'''
def getTempAirHum():
    global smooth_factor
    global smoothed_humidity
    global smoothed_temperature
    global measure_failures_TempAirHum
    humidity = None
    temperature = None
    try:
        if globals.SIMULATE:
            humidity = sensor_fake.get_fake_airhum(sensor_fake.get_aktTime())
            temperature = sensor_fake.get_fake_temp(sensor_fake.get_aktTime())
        else:
            humidity, temperature = globals.Ada_DHT.read_retry(globals.tempAirHum_sensor, globals.tempAirHum_gpio)
        if humidity > 100:
            humidity = 100
        if smoothed_humidity == -100.0:
            smoothed_humidity = humidity
        else:
            smoothed_humidity = (smooth_factor*humidity) + ((1-smooth_factor)*smoothed_humidity)
        if smoothed_temperature == -100.0:
            smoothed_temperature = temperature
        else:
            smoothed_temperature = (smooth_factor*temperature) + ((1-smooth_factor)*smoothed_temperature)
        measure_failures_TempAirHum = 0
        return smoothed_temperature, smoothed_humidity
    except:
	measure_failures_TempAirHum = measure_failures_TempAirHum +1
        globals.display_cmd.print_displ(  "SENSORS: Some issues in getTempAirHum(): hum:"+str(humidity)+" tmp:"+str(temperature)+" :: Fails "+str(measure_failures_TempAirHum))
        return smoothed_temperature, smoothed_humidity
        
def getSoilHumidity():
	global smoothed_soilhumidity
	global water_latest_measurement
	global smooth_factor
	global measure_failures_SoilMoisture
	if globals.SIMULATE:
		time_diff = 0.001
		if water_latest_measurement != 0:
			time_diff = time.time() - water_latest_measurement
		sensor_fake.stepWorld(sensor_fake.get_aktTime(), time_diff)
		soilhumidity = sensor_fake.get_fake_soilhum()
		if smoothed_soilhumidity == -100.0:
			smoothed_soilhumidity = soilhumidity
		else:
			smoothed_soilhumidity = (smooth_factor*soilhumidity) + ((1-smooth_factor)*smoothed_soilhumidity)
		water_latest_measurement = time.time()
		measure_failures_SoilMoisture = 0
		return soilhumidity
	else:
		global soilmoisture_min
		global soilmoisture_max 
		global soilmoisture_smoothed_cur
		minm = soilmoisture_min
		maxm = soilmoisture_max 
		moist_raw = 0.0
		try:
		    moist_raw = globals.moisture_sensor.moist()
		    measure_failures_SoilMoisture = 0
		except:
		    measure_failures_SoilMoisture = measure_failures_SoilMoisture +1
		    globals.display_cmd.print_displ(  "SENSORS: Some issues in getSoilHumidity(): soilm:"+str(moist_raw)+" smoothed:"+str(smoothed_soilhumidity)+" :: Fails "+str(measure_failures_SoilMoisture ))
		    return smoothed_soilhumidity 
		    
		moist_p = (moist_raw-minm)/(maxm-minm) *100
		#print "moisture: ",moist_raw, "\tpoz "+"{:4.2f}".format(moist_p) +"\tsmoothed: "+"{:4.2f}".format(smoothed_soilhumidity) #for calibration
		if moist_p < 0:
		    moist_p = 0
		if moist_p > 100:
		    moist_p = 100
		    
		if smoothed_soilhumidity == -100.0:
			smoothed_soilhumidity = moist_p
		else:
			smoothed_soilhumidity = (smooth_factor*moist_p) + ((1-smooth_factor)*smoothed_soilhumidity)

		#smoothed raw value for calibration	
		if soilmoisture_smoothed_cur == -100.0:
			soilmoisture_smoothed_cur = moist_raw
		else:
			soilmoisture_smoothed_cur= (smooth_factor*moist_raw ) + ((1-smooth_factor)*soilmoisture_smoothed_cur)
			
		water_latest_measurement = time.time()
		return smoothed_soilhumidity
		
def calibrateSoilHumidityMax():
    if not globals.SIMULATE:
	global soilmoisture_max
	global soilmoisture_smoothed_cur
	soilmoisture_max = int(round(soilmoisture_smoothed_cur))
        
def getLightHours():
	global light_light_hours
	if globals.SIMULATE:
		return light_light_hours
	else:
		return light_light_hours
        
'''
Measures Light over time.
accumulates lighthoures, watt houres per meter^2

Info:
ab 120w/m^2 eine Sonnenstunde
in Deutschland Sommer u. Sonne 1000w/m^2
ca 100000 lux = 1000w/m^2 bzw (wohl besser) 1000 lx = 8,0 W/m^2 => umrechnungsfaktor 0.008
'''
def measure_light():   
    lux_w_m2_factor = 8         #1 kilolux = 8 w/m^2
    light_filter_factor = 2.75  #white plastic filter gets from 110.000lx to 40.000lx -> factor is about 2.75
    global light_wh_m2
    global light_akt_lux
    global light_light_hours
    global light_latest_measurement
    global light_kilo_luxh
    global light_last_day
    global measure_failures_Light
    
    #get lux from sensor or fake sensor
    try:
        if not globals.SIMULATE:
            light_akt_lux = globals.light_sensor.measure_high_res() * light_filter_factor 
        else:
            light_akt_lux = sensor_fake.get_fake_lux(sensor_fake.get_aktTime())
            
        light_w_m2 = (light_akt_lux/1000.0) * lux_w_m2_factor
             
        if light_latest_measurement != 0:
            time_diff = time.time() - light_latest_measurement
            light_kilo_luxh = light_kilo_luxh + ((light_akt_lux / 1000.0)*(time_diff/3600.0))
            
            light_wh_m2 = light_kilo_luxh * lux_w_m2_factor 
            
            #accumulate lighthours.
            #Eine Sonnenstunde wird ab 120w/m2 gezaehlt => ab 15.000 lux (nach obriger Formel)
            if light_w_m2 > 120 :
                light_light_hours = light_light_hours + (time_diff/3600.0)
            
        light_latest_measurement = time.time()
	measure_failures_Light = 0
    except:
	measure_failures_Light = measure_failures_Light +1
        globals.display_cmd.print_displ(  "SENSORS: Some issues in measure_light() :: Fails:"+ str(measure_failures_Light))
        
	
    #check if day has changed, reset all light values
    today = time.localtime().tm_yday
    if (today != light_last_day):
	    light_wh_m2 = 0.0               
	    light_akt_lux = 0.0           
	    light_kilo_luxh = 0.0          
	    light_light_hours = 0.0         
	    light_latest_measurement = 0
	    globals.display_cmd.print_displ(  "SENSORS: Day has changed")
	    light_last_day = today
		
'''
Checks is Water is empty
Returns	0 if tank is full (not empty)
	1 if tank is empty
'''
def isWaterEmpty():
    if not globals.SIMULATE:
	isEmpty = 1 - globals.GPIO.input(globals.water_fill_gpio)
	return isEmpty
    
    else:
	return 0

    
    
    

