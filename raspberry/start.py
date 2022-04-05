#start.py

'''
This is the GP_Server aplication. This manages the sensors and devices connectet to the Raspberry PI.

It handles the tcp-commands from OpenHAB. These commands ar given (mostly) as JSON 
and sends though TCP the latest measurements of the sensors in an given interval
'''

import time
import threading
from threading import Thread
import thread
import sys
import json
import display

import globals
from server import onStateChanged
import sensors
import devices
import fallback_system

from tcpcom import TCPServer




'''
Main Program Class
Handles the GPIO initialation and the main loop.
In the main loop the sensor data is measured and send throu the tcp server.
'''
class MainProgram(Thread):
    def __init__(self):
        print "Initialized MainProgramm"
        Thread.__init__(self)
        self.loadSettings()
        self.start()
        
    def run(self):
        try:
            #update all sensor data every X seconds, where x = globals.sensor_measurement_interval 
            while globals.MainProgram_run:
                sensors.measure_light() 
                temperature, humidity = sensors.getTempAirHum()
                water_isEmpty = sensors.isWaterEmpty()
                sensor_data = {
                  "temp_1": str(temperature),
                  "air_Hum": str(humidity),
                  "soil_Hum": str(sensors.getSoilHumidity()),
                  "light_h": str(sensors.light_light_hours),          
                  "light_wh_m2": str(sensors.light_wh_m2),
                  "light_kilo_luxh": str(sensors.light_kilo_luxh),
                  "light_akt_lux": str(sensors.light_akt_lux),
                  "is_water_empty": str(water_isEmpty)
                }
                #send sensor state
                air_hum_temp_sensor = "OK"
                if sensors.measure_failures_TempAirHum > 3:
                    air_hum_temp_sensor = "FAILED"
                soilhum_sensor = "OK"
                if sensors.measure_failures_SoilMoisture > 3:
                    soilhum_sensor = "FAILED"
                light_sensor = "OK"
                if sensors.measure_failures_Light > 3:
                    light_sensor = "FAILED"
                    
                sensor_state = {
                    "soilhum_sensor": soilhum_sensor,
                    "air_hum_temp_sensor": air_hum_temp_sensor,
                    "light_sensor": light_sensor
                }
                sensor_data.update(sensor_state)
                globals.server.sendMessage(json.dumps(sensor_data)+"\n")
                
                #Use Fallback System for now
                if (not globals.SIMULATE) and globals.use_fallback_system:
                    fallback_system.checkGarden()
                
                time.sleep(globals.sensor_measurement_interval )
        except KeyboardInterrupt:
            globals.server.disconnect()
            globals.server.terminate()
            globals.shutter.terminate()
            globals.MainProgram_run = False
            globals.Display_cmd_run = False
                
                
        #clean up at the end
        self.exitGPIO()
        self.saveSettings()
        globals.Display_cmd_run= False
        
    def loadSettings(self):
        try:
            with open('settings.cfg') as json_file:
                data = json.load(json_file)
                #set values from comming from OpenHAB
                if 'set_waterflow' in data:
                    devices.dev_waterflow = int(data['set_waterflow'])
                if 'set_total_steps' in data:
                    globals.shutter.setTotalSteps( int(data['set_total_steps']))
                if 'set_soilmoisture_max' in data:
                    sensors.soilmoisture_max = int(data['set_soilmoisture_max'])
        except:
            print "Could not load settings.cfg"
            
    def saveSettings(self):
        try:
            with open('settings.cfg','w') as json_file:
                data = {"set_waterflow" : str(devices.dev_waterflow),
                        "set_total_steps": str(globals.shutter.getTotalSteps()),
                        "set_soilmoisture_max": str(sensors.soilmoisture_max)
                }
                print data
                json.dump(data, json_file)
        except:
            print "Could not save settings.cfg"
    
    '''
    Cleans up the GPIO pins of the board
    '''
    def exitGPIO(self):
        if not globals.SIMULATE:
            globals.GPIO.cleanup()

if __name__ == "__main__":    
    port = 5005
    globals.display_cmd = display.Display_cmd()
    globals.server = TCPServer(port, stateChanged = onStateChanged) #delegates to server.onStateChanged
    
    MP = MainProgram()

    #globals.server.terminate()
    print "done"
