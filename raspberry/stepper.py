import time
import RPi.GPIO as GPIO
import threading


'''
Handles shutter
Expects that GPIO is set and pin-setup is done
'''
class shutter_stepper(threading.Thread):
	def __init__(self, gpio_dir, gpio_step, gpio_sleep, GPIO):
		threading.Thread.__init__(self)
		self.GPIO = GPIO
		self.gpio_direction = gpio_dir
		self.gpio_step = gpio_step
		self.gpio_sleep = gpio_sleep
		self.working = False
		self.opening = False
		self.closing = False
		self.opened = True
		self.closed = False
		self.total_steps = 1540 #account for limited movement due to rusting bearings; old:200*10
		self.position = 0
		self.running  =True
		self.start()
		
	def setTotalSteps(self, num):
		self.total_steps = num
		
	def getTotalSteps(self):
		return self.total_steps
	
	def run(self):	
		while self.running:
			time.sleep(0.01)
			#if working... and check if work is to do an the second AND part
			if self.working and (self.opening or self.closing):
				#print "opening: ",self.opening, "\t\tclosing ", self.closing, "  position ", self.position
				#wake stepper driver, because we are working now
				self.GPIO.output(self.gpio_sleep, self.GPIO.HIGH)
				
				#handle closing
				if self.closing and (not self.opening):
					if self.position < self.total_steps:
						self.GPIO.output(self.gpio_direction, self.GPIO.HIGH) #Swap HIGH/LOW if opening
						self.GPIO.output(self.gpio_step, self.GPIO.HIGH)
						self.GPIO.output(self.gpio_step, self.GPIO.LOW)
						self.position = self.position +1					
					else:
						self.working = False
						self.closing = False
						self.opening = False
						self.opened = False
						self.closed = True
						
				#handle opening
				if self.opening and (not self.closing):
					if self.position > 0:
						self.GPIO.output(self.gpio_direction, self.GPIO.LOW) #Swap HIGH/LOW if opening
						self.GPIO.output(self.gpio_step, self.GPIO.HIGH)
						self.GPIO.output(self.gpio_step, self.GPIO.LOW)		
						self.position = self.position -1				
					else:
						self.working = False
						self.closing = False
						self.opening = False
						self.opened = True
						self.closed = False
						
						
				if self.closing and self.opening:
					print "STEPPER: can't open and close at the same time, stop working now"
					self.working = False
			else:
				#send stepper driver sleeping
				self.GPIO.output(self.gpio_sleep, self.GPIO.LOW)
				if self.working:
					#seems working is set, but nothing to do(opening/closing)
					working = False
					print "STEPPER: working but nothing to do"
				
				
	def close(self):
		if not self.closed:
			self.working = True
			self.closing = True
			self.opening = False
			self.opened = False
			self.closed = False
			
	def open(self):
		if not self.opened:
			self.working = True
			self.closing = False
			self.opening = True
			self.opened = False
			self.closed = False
			
	def terminate(self):
		self.running =False
			
	
	
	

"""
gpio_stepper_direction 	= 21
gpio_stepper_step		= 20
gpio_stepper_sleep		= 16


GPIO.setmode(GPIO.BCM)
GPIO.setup(gpio_stepper_direction, GPIO.OUT)
GPIO.setup(gpio_stepper_step, GPIO.OUT)
GPIO.setup(gpio_stepper_sleep, GPIO.OUT)


print "initialize"
shutter = shutter_stepper(gpio_stepper_direction, gpio_stepper_step, gpio_stepper_sleep, GPIO)
print "doing smth"
time.sleep(5)

print "closing"
shutter.close()
time.sleep(1)

print "opening"
shutter.open()
time.sleep(10)

shutter.terminate()
time.sleep(1)

	
GPIO.cleanup()
"""

