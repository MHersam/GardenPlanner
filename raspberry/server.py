#server.py

'''
Handles TCP inputs and delegates the commands.
'''

import time
import sys
import globals
import devices
import sensors

from tcpcom import TCPServer
from random import randint
import json

'''
onStateChanged is triggered if an TCP-MSG arrives on the socket.
This method deligates the commands from OpenHAB to the connected devices
'''
def onStateChanged(state, msg):
    if state == TCPServer.MESSAGE:
        globals.display_cmd.print_displ( "TcpServer: Received msg:"+ str(msg))
        
        #------------ Handle GardenPlaner inputs (gP) -----------------------
        
        #JSON auslesen falls es ein JSON ist
        try:
            JSONparsedMSG = json.loads(msg)
            
            #check for watering command
            if 'startWatering' in JSONparsedMSG:
                devices.controlWaterpump(JSONparsedMSG['startWatering'])
            #check for light command
            if 'startLight' in JSONparsedMSG:
                devices.controlLight(JSONparsedMSG['startLight'])
            #check for heating command
            if 'startHeating' in JSONparsedMSG:
                devices.controlHeating(JSONparsedMSG['startHeating'])
            #check for heating command
            if 'changeSutter' in JSONparsedMSG:
                devices.controlShutter(JSONparsedMSG['changeSutter'])
            
            #set values from comming from OpenHAB
            if 'set_waterflow' in JSONparsedMSG:
                devices.dev_waterflow = int(JSONparsedMSG['set_waterflow'])
            if 'set_total_steps' in JSONparsedMSG:
                globals.shutter.setTotalSteps( int(JSONparsedMSG['set_total_steps']))
            if 'set_soilmoisture_max' in JSONparsedMSG:
                sensors.calibrateSoilHumidityMax()
            
        except:
            pass
        
        
        #------------ Handle Testgound inputs (tG) --------------------------
        if msg == "TCP_SWITCH 1":
            data = '{"tg_tcp_SW_rec":"ON"}'
            globals.server.sendMessage(data)
            globals.display_cmd.print_displ( 'TcpServer: Sende: '+ str(data))
        if msg == "TCP_SWITCH 0":
            data = '{"tg_tcp_SW_rec":"OFF"}'
            globals.server.sendMessage(data)
            globals.display_cmd.print_displ( 'TcpServer: Sende: '+ str(data))
        
            
        #Handle EXIT command
        if msg == "Server_Exit":
            globals.display_cmd.print_displ( "TcpServer: Exit by TCP Command")
            globals.MainProgram_run = False
            globals.Display_cmd_run = False
            globals.shutter.terminate()
            globals.server.terminate()
    else:
        globals.display_cmd.print_displ( "TcpServer: Some unknown state from TCP:  "+str(state)+" :: "+str(msg))
        
