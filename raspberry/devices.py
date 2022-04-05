#devices.py

'''
This Modul handles the raspberry devices like water pump, shutters, etc...
'''

import globals

import time
import threading
import json

if globals.SIMULATE:
	import sensor_fake

GPIO_state_isWatering = False
GPIO_state_isLighting = False
GPIO_state_isHeating = False
GPIO_state_isShutter_open = True

#Watering Block, maby use a thread object to abord the thread if watering should stop maunally (in function watering(waterduration))
#waterflow ml/min 
dev_waterflow = 120
def controlWaterpump(ml):
    global GPIO_state_isWatering
    global dev_waterflow
    if ml != "STOP" and not GPIO_state_isWatering:
        #calculated waterduration in seconds
        waterduration = (float(ml)/dev_waterflow)*60
        waterthread = threading.Thread(target=watering, args=(waterduration,))
        waterthread.start()
    else:
        GPIO_stopWatering()
    
def watering(waterduration):
    globals.display_cmd.print_displ(  "DEVICES: Starte Bewaesserung : Time (s)"+str(waterduration))
    GPIO_startWatering()
    time.sleep(waterduration)
    GPIO_stopWatering()
        
def controlLight(cmd):
    global GPIO_state_isLighting
    if cmd == "START" and not GPIO_state_isLighting:
        GPIO_startLight()
    if cmd == "STOP" and GPIO_state_isLighting:
        GPIO_stopLight()
        
def controlHeating(cmd):
    global GPIO_state_isHeating
    if cmd == "START" and not GPIO_state_isHeating:
        GPIO_startHeating()
    if cmd == "STOP" and GPIO_state_isHeating:
        GPIO_stopHeating()
        
def controlShutter(cmd):
    global GPIO_state_isShutter_open
    #TODO
    if cmd == "OPEN":
        globals.display_cmd.print_displ(  "DEVICES::GPIO: Oeffne Shutter")
        GPIO_state_isShutter_open = True
        if not globals.SIMULATE:
            globals.shutter.open()
    if cmd == "CLOSE":
        globals.display_cmd.print_displ(  "DEVICES::GPIO: Schliesse Shutter")
        GPIO_state_isShutter_open = False
        if not globals.SIMULATE:
            globals.shutter.close()
    
        
        
#---------------- GPIO ------------------------
def GPIO_startWatering():
    global GPIO_state_isWatering
    if globals.SIMULATE:
    #Case Simulate an PC:
        globals.display_cmd.print_displ(  "DEVICES::GPIO: Starte Bewaesserung")
        if not GPIO_state_isWatering:
            GPIO_state_isWatering = True
            sensor_fake.fake_watering()
    else:
    #Case Working on Rasp:
        if not GPIO_state_isWatering:
            globals.display_cmd.print_displ(  "DEVICES::GPIO: Starte Bewaesserung")
            globals.GPIO.output(globals.rel_water_gpio, globals.GPIO.LOW)
            GPIO_state_isWatering = True
        
    
        
def GPIO_stopWatering():
    global GPIO_state_isWatering
    if globals.SIMULATE:
    #Case Simulate an PC:
        if GPIO_state_isWatering:
            globals.display_cmd.print_displ("DEVICES:GPIO: Stoppe Bewaesserung")
            GPIO_state_isWatering = False
            device_data = {
              "dev_water": "OFF"
            }
            globals.server.sendMessage(json.dumps(device_data))
    else:
    #Case Working on Rasp:
        #TODO
        if GPIO_state_isWatering:
            globals.display_cmd.print_displ( "DEVICES:GPIO: Stoppe Bewaesserung")
            globals.GPIO.output(globals.rel_water_gpio, globals.GPIO.HIGH)
            GPIO_state_isWatering = False
            device_data = {
              "dev_water": "OFF"
            }
            globals.server.sendMessage(json.dumps(device_data))
            

def GPIO_startLight():
    global GPIO_state_isLighting
    if globals.SIMULATE:
        GPIO_state_isLighting = True
        globals.display_cmd.print_displ( "DEVICES:GPIO: Starte kuenstliche Beleuchtung")
    else:
        GPIO_state_isLighting = True
        globals.GPIO.output(globals.rel_light_gpio, globals.GPIO.LOW)
        globals.display_cmd.print_displ( "DEVICES:GPIO: Starte kuenstliche Beleuchtung" )
    
def GPIO_stopLight():
    global GPIO_state_isLighting
    if globals.SIMULATE:
        GPIO_state_isLighting = False
        globals.display_cmd.print_displ(  "DEVICES:GPIO: Stoppe kuenstliche Beleuchtung")
    else:
        GPIO_state_isLighting = False
        globals.GPIO.output(globals.rel_light_gpio, globals.GPIO.HIGH)
        globals.display_cmd.print_displ(  "DEVICES:GPIO: Stoppe kuenstliche Beleuchtung")

    
def GPIO_startHeating():
    global GPIO_state_isHeating
    if globals.SIMULATE:
        GPIO_state_isHeating = True
        globals.display_cmd.print_displ(  "DEVICES:GPIO: Starte Heizung")
    else:
        GPIO_state_isHeating = True
        globals.GPIO.output(globals.rel_heating_gpio, globals.GPIO.LOW)
        globals.display_cmd.print_displ(  "DEVICES:GPIO: Starte Heizung")

    
def GPIO_stopHeating():
    global GPIO_state_isHeating
    if globals.SIMULATE:
        GPIO_state_isHeating = False
        globals.display_cmd.print_displ( "DEVICES:GPIO: Stoppe Heizung")
    else:
        GPIO_state_isHeating = False
        globals.GPIO.output(globals.rel_heating_gpio, globals.GPIO.HIGH)
        globals.display_cmd.print_displ(  "DEVICES:GPIO: Stoppe Heizung")

        
    
    
    
