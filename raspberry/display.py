#display.py

'''
Displayes status of the GP_Server.
Change view from window to cmd by setting globals.displaySetCMD to 'True'

Displayes latest sensor data and the states of the devices like heating, waterpump and lights
'''

import sensors
import devices
import globals
import devices
import time
import os
import threading
from threading import Thread
import thread
import datetime
import Tkinter as tk


class Display_cmd(Thread):
	def __init__(self):
		Thread.__init__(self)
		#holds all messages. 
		self.msgs=[]
		#If logging is True, all prints of the Display are logged into File. Set this to 'False' if logging is not needed
		self.logging= True
		self.start()
    
	def run(self):
		try:
			refresh_counter = 0
			#Display Data in CMD
			if globals.displaySetCMD:
				while globals.Display_cmd_run:
					refresh_counter = (refresh_counter+1) % 2			#update every 2 seconds
					if refresh_counter == 0:
						text_water = "off"
						if devices.GPIO_state_isWatering :
							text_water = " on"
						text_light = "off"
						if devices.GPIO_state_isLighting :
							text_light = " on"
						text_heating = "off"
						if devices.GPIO_state_isHeating :
							text_heating = " on"
						text_shutter = "off"
						if devices.GPIO_state_isShutter_open :
							text_shutter = " on"

						
						os.system('cls' if os.name == 'nt' else 'clear')
						print "#######################################################"
						print "#Devices:| water | light | heating | shutter |        #"
						print "#        | ", text_water," | ", text_light," |  ", text_heating,"  |  ", text_shutter,"  |        #"
						print "#######################################################"
						t_temp= addSpaces("{:4.1f}".format(sensors.smoothed_temperature),6,False)
						t_ahum= addSpaces("{:4.1f}".format(sensors.smoothed_humidity),6,False)
						t_shum= addSpaces("{:4.2f}".format(sensors.smoothed_soilhumidity),6,False)
						print "#Sensors: temp:",t_temp,"  ahum:", t_ahum," shum:", t_shum,"  #"
						
						t_lux = addSpaces("{:6.1f}".format(sensors.light_akt_lux),8,True)
						t_kluxh = addSpaces("{:7.3f}".format(sensors.light_kilo_luxh),11,True)
						print "#         lux  :   ", t_lux,"   kLUXh:  ",t_kluxh," #"
						
						t_wh_m2 = addSpaces("{:7.3f}".format(sensors.light_wh_m2),11,True)
						t_lighth = addSpaces("{:2.6f}".format(sensors.light_light_hours),9,True)
						print "#         wh/m2:", t_wh_m2,"   SoStunden:",t_lighth," #"
						print "#######################################################"
						
						#print msgs
						for i in range(len(self.msgs)):
							if i>8:							#show last 8 msgs
								break
							print self.msgs[len(self.msgs)-(i+1)]
						
					time.sleep(1)
			#Display data in Window
			else:
				self.showGUI()
					
		#Handle Exceptions (On KeyboardInterrupt close program)		
		except KeyboardInterrupt:
			globals.shutter.terminate()
			globals.server.disconnect()
			globals.server.terminate()
			globals.MainProgram_run = False
			globals.Display_cmd_run = False
			
	'''
	Loggs and displays messages
	'''		
	def print_displ(self, string):
		time_string = datetime.datetime.now().strftime('[%Y-%m-%d %H:%M:%S] ')+string
		self.msgs.append(time_string)
		if self.logging:
			with open('log.txt', 'a') as the_file:
				the_file.write(time_string+'\n')
		
		
	'''
	Shows Gui instead of cmd output
	'''
	def showGUI(self):
		def update_devices(label):
			def updateLab():
				text_water = "off"
				if devices.GPIO_state_isWatering :
					text_water = " on"
				text_light = "off"
				if devices.GPIO_state_isLighting :
					text_light = " on"
				text_heating = "off"
				if devices.GPIO_state_isHeating :
					text_heating = " on"
				text_shutter = "close"
				if devices.GPIO_state_isShutter_open :
					text_shutter = "open"
				txt = "Devices:\n"
				txt = txt+"Water:\t"+text_water+"\t\t"
				txt = txt+"Light:\t"+text_light+"\n"
				txt = txt+"Heating:\t"+text_heating+"\t\t"
				txt = txt+"Shutter:\t"+text_shutter
				label.config(text=txt, justify=tk.LEFT)
				label.after(2000, updateLab)
			updateLab()
			
		def update_sensors(label):
			def updateLab():
				t_temp= addSpaces("{:4.1f}".format(sensors.smoothed_temperature),6,False)
				t_ahum= addSpaces("{:4.1f}".format(sensors.smoothed_humidity),6,False)
				t_shum= addSpaces("{:4.2f}".format(sensors.smoothed_soilhumidity),6,False)
				#check water tank
				t_water_empty = "empty"
				if (sensors.isWaterEmpty() == 0):
					t_water_empty = "full"
				txt =  "Sensors:\ntemp:"+t_temp+"\tahum:"+t_ahum+"\tshum:"+t_shum+"\tWaterFill:"+t_water_empty+"\n"
				
				t_lux = addSpaces("{:6.1f}".format(sensors.light_akt_lux),8,True)
				t_kluxh = addSpaces("{:7.3f}".format(sensors.light_kilo_luxh),11,True)
				txt = txt+ "lux:\t"+ t_lux+"\t\tkLUXh:\t\t"+t_kluxh+"\n"
				
				t_wh_m2 = addSpaces("{:7.3f}".format(sensors.light_wh_m2),11,True)
				t_lighth = addSpaces("{:2.6f}".format(sensors.light_light_hours),9,True)
				txt = txt+ "wh/m2:\t"+ t_wh_m2+"  \tSoStunden:\t"+t_lighth+"\n"
				shutter_totalsteps = "SIM"
				if not globals.SIMULATE:
					shutter_totalsteps = str(globals.shutter.getTotalSteps())
				txt = txt + "waterflow: "+str(devices.dev_waterflow) +" ShutterTS: "+shutter_totalsteps +" SM_max: "+str(sensors.soilmoisture_max)
				label.config(text=txt, justify=tk.LEFT)
				label.after(2000, updateLab)
			updateLab()
			
		def update_msgs(label):
			def updateLab():
				#print msgs
				txt = "\nMSG:\n"
				for i in range(len(self.msgs)):
					if i>10:							#show last 10 msgs
						break
					txt = txt + self.msgs[len(self.msgs)-(i+1)]+"\n"
				label.config(text=txt, justify=tk.LEFT)
				label.after(2000, updateLab)
			updateLab()
			
		root = tk.Tk()
		root.title("Status GardenPlaner Server")
		
		label_devices = tk.Label(root)
		label_devices.grid(row=0, sticky=tk.W)
		update_devices(label_devices)
		
		label_sensors = tk.Label(root)
		label_sensors.grid(row=1, sticky=tk.W)
		update_sensors(label_sensors)
		
		label_msgs = tk.Label(root)
		label_msgs.grid(row=2, sticky=tk.W)
		update_msgs(label_msgs)
		
		but_stop = tk.Button(root, text='Stop', width=25, command=root.destroy)
		but_stop.grid(row=3, sticky=tk.W)
		root.mainloop()
		#if this code is reached, window is closed -> Close program
		if not globals.SIMULATE:
			globals.shutter.terminate()
		globals.server.disconnect()
		globals.server.terminate()
		globals.MainProgram_run = False
		globals.Display_cmd_run = False
		

			
'''
Adds to an string the number of Spaces that the total lenghts fits "length"
Input:
	string:		original string
	length:		lengrh to fit
	befor:		boolean, add spaces befor or after the string
returns string with the given length
'''
def addSpaces(string, length, befor):
	o_len = len(string)
	if o_len > length:
		return string
	
	diff_len = length - o_len
	if befor:
		string = (" " * diff_len)+ string
	else:
		string = string+( " "*diff_len)
	return string
	
			
			
			
		
