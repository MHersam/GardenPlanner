import time
from scipy import interpolate
import matplotlib.pyplot as plt
import numpy as np

soil_hum = 95.0

def get_fake_temp(actTime):
	x = [  0,  5,  8,   15,   20,   24]
	y = [ 14, 14, 16,   26,   20,   14]
	f = interpolate.interp1d(x,y, kind='cubic')
	return f(actTime)
	
def get_fake_airhum(actTime):
	x = [  0,  5,  8,   15,   20,   24]
	y = [ 95, 85, 80,   43,   65,   80]
	f = interpolate.interp1d(x,y, kind='cubic')
	return f(actTime)
	
def get_fake_soilhum():
	global soil_hum
	return soil_hum
	
def get_fake_lux(actTime):
	x = [  0, 2,   5,    8,     10,     15,      20,   24]
	y = [  0, 0,   0, 6000,  30000, 100000,   25000,    0]
	f = interpolate.interp1d(x,y, kind='cubic')
	val = f(actTime)
	if val <= 0.0 :
		val = 0.0
	return val

'''
stepsize in seconds
'''	
def stepWorld(actTime, stepsize):
	global soil_hum
	temp = get_fake_temp(actTime)
	airhum = get_fake_airhum(actTime)
	discount = (temp*1.0)*(1.0-(airhum/100.0)) * (stepsize/3600.0)
	soil_hum = soil_hum - discount 
	if soil_hum < 0:
		soil_hum=0
		
def fake_watering():
	global soil_hum
	soil_hum = 99.0
	
def get_aktTime():
	hour =  time.localtime().tm_hour
	minutes =  time.localtime().tm_min
	akttime = (hour)+(minutes/60.0)
	return akttime
	
	
	

