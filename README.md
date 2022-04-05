# GP_OpenHABIntegration

This folder structure has to be used for OpenHab. <br>
Copy respectively replace the files in your OpenHab folder. At the first time delete all demo* files for a clean OpenHab. <br>
Maybe you have to change the default Sitemap to "gp": see https://docs.openhab.org/v2.1/tutorials/beginner/sitemap.html <br>

Start the Raspberryserver before OpenHab till the TCP-Socket problem is not solved. (seems to be a random problem on windows / no problems on linux)

To start on PC with simulated Raspberry Server:
  - in "./conf/items/gp.items", change comments: <br>
    /* tcp port for the communication with the Raspberry. E.g. used for commands or getting sensor info */ <br>
    String 	MyTCP_Rasp			"TCP_Rasp" 		{tcp=">[*:127.0.0.1:5005:default]"} <br>
    /*String 	MyTCP_Rasp			"TCP_Rasp" 		{tcp=">[*:192.168.8.150:5005:default]"} */ <br>

Needed Add-Ons and other stuff:
- JSONPath Transformation
  -> Install trough Paper UI: Add-Ons -> Transformations -> JSONPath Transformation
- RRD4j Persistence
  -> Install trough Paper UI: Add-Ons -> Persistence -> RRD4j Persistence
- TCP/UDP Binding
  -> Install trough Paper UI: Add-Ons -> Bindings -> TCP/UDP Binding

- Set your IP of the OpenHAB-Server at the following documents:
  -  in conf/html/textinput.html:<br>
      <script><br>
          var openHabianPI = "localhost"<br>
          function getParam(param)<br>
  -  in conf/sitemaps/gp.sitemap:<br>
      Frame label="Raspberry Settings:" {<br>
          /*Setup raspberry*/<br>
          Webview url="http://localhost:8080/static/textinput.html?item=rasp_waterflow&label=Rasp Waterflow ml/min" height=1<br>
          Webview url="http://localhost:8080/static/textinput.html?item=rasp_shutter_totalSteps&label=Rasp Shutter total_steps" height=1<br>
          Switch item=rasp_calibrate_soilmoisture_max label="Set current soilhumidity to 100%"<br>
        }
