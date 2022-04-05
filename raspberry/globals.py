#global variables

#set this false to stop the program
MainProgram_run = True
Display_cmd_run = True
#tcp server object
server = None
#display
display_cmd = None
displaySetCMD = False	#set this True for Displaying data in cmd; False is in Window
#SIMULATE = False if working on Raspberry / True if working on PC
import sys
#Use Fallback System
use_fallback_system = True

#check cmd arguments
SIMULATE = True
for arg in sys.argv:
	if (arg == "SIMULATE=False") or (arg == "SIMULATE=false"):
		print "!!!Run on Gardensytem, do not simulate!!!"
		SIMULATE = False

        
#check if some Threat is working on the GPIO
gpio_isfree = True
#Interval time for sensor measurments (seconds)
sensor_measurement_interval = 10

if not SIMULATE:
	#GPIO Pins
	rel_water_gpio   = 17
	rel_light_gpio   = 27
	water_fill_gpio	 = 26
	rel_heating_gpio = 22
	tempAirHum_gpio  = 4
	stepper_direction_gpio 	= 21
	stepper_step_gpio		= 20
	stepper_sleep_gpio		= 16
	
	#Import of the GPIO Lib
	print "Imported GPIO"
	import RPi.GPIO as GPIO
	
	#SetupGPIO
	GPIO.setmode(GPIO.BCM)
	GPIO.setup(rel_water_gpio, GPIO.OUT)
	GPIO.output(rel_water_gpio, GPIO.HIGH)
	GPIO.setup(water_fill_gpio, GPIO.IN)
	GPIO.setup(rel_light_gpio, GPIO.OUT)
	GPIO.output(rel_light_gpio, GPIO.HIGH)
	GPIO.setup(rel_heating_gpio, GPIO.OUT)
	GPIO.output(rel_heating_gpio, GPIO.HIGH)
	
	GPIO.setup(stepper_direction_gpio, GPIO.OUT)
	GPIO.setup(stepper_step_gpio, GPIO.OUT)
	GPIO.setup(stepper_sleep_gpio, GPIO.OUT)
	
	print "Imported smbus"
	import smbus
	bus = smbus.SMBus(1)  #i2c buss
	
	#Import of the Adafruit Temperature and Humidity Lib
	import Adafruit_DHT as Ada_DHT
	print "SENSORS: Imported DHT22"
	tempAirHum_sensor = Ada_DHT.DHT22
	
	#Import Lightsensor BH1750
	from light_sensor_class_bh1750 import BH1750
	print "SENSORS: Imported BH1750"	
	light_sensor = BH1750(bus)
	light_sensor.set_sensitivity(31)
	
	#Import Chirp-Moisture Sensor
	from i2cMoisture import Chirp
	moisture_sensor = Chirp(bus, 0x20)
	
	#Import Shutterstepper
	from stepper import shutter_stepper
	shutter = shutter_stepper(stepper_direction_gpio, stepper_step_gpio, stepper_sleep_gpio, GPIO)
	
