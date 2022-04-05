
import smbus, time, sys

class Chirp:
	def __init__(self, bus, address=0x20):
		self.bus = bus
		self.address = address
    
	def get_reg(self, reg):
		# read 2 bytes from register
		val = self.bus.read_word_data(self.address, reg)
		# return swapped bytes (they come in wrong order)
		return (val >> 8) + ((val & 0xFF) << 8)

	def reset(self):
		# To reset the sensor, write 6 to the device I2C address
		self.bus.write_byte(self.address, 6)

	def set_addr(self, new_addr):
		# To change the I2C address of the sensor, write a new address
		# (one byte [1..127]) to register 1; the new address will take effect after reset
		self.bus.write_byte_data(self.address, 1, new_addr)
		# second request is required since FW 0x26 to protect agains spurious address changes
		self.bus.write_byte_data(self.address, 1, new_addr)
		self.reset()
		self.address = new_addr

	def moist(self):
		# To read soil moisture, read 2 bytes from register 0
		return self.get_reg(0)

	def temp(self):
		# To read temperature, read 2 bytes from register 5
		return self.get_reg(5)

	def light(self):
		# To read light level, start measurement by writing 3 to the
		# device I2C address, wait for 3 seconds, read 2 bytes from register 4
		self.bus.write_byte(self.address, 3)
		time.sleep(1.5)
		return self.get_reg(4)

	def __repr__(self):
		return "<Chirp sensor on bus %d, addr %d>" % (self.bus_num, self.address)

if __name__ == "__main__":
	addr = 0x20
	#if len(sys.argv) == 2:
	#	if sys.argv[1].startswith("0x"):
	#		addr = int(sys.argv[1], 16)
	#	else:
	#		addr = int(sys.argv[1])
	chirp = Chirp(1, addr)
	
	minm = 253.0
	maxm = 540.0

	print chirp
	print "Moisture\tTemperature\tBrightness"
	while True:
		moist_raw = chirp.moist()
		moist_p = int((moist_raw-minm)/(maxm-minm) *100)
		print "raw: ",moist_raw,"\t \%",moist_p
		
		time.sleep(1)
