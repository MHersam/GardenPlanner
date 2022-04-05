package org.openHABRest;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.time.LocalDateTime;

import org.json.JSONArray;
import org.json.JSONObject;
import org.openHABSchedule.*;

import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * @author Michael Hersam, Jannis Westermann, Philipp Wonner
 */

public class openHABRest {
	
	private OkHttpClient 	client;
	
	private String 			configPath 	= "config.json";
	private JSONObject 		config;
	private String 			address;
	
	private JSONArray		itemConfig;
	private HashMap<String, JSONObject>		itemHM = new HashMap<String, JSONObject>();
	private boolean			justInTime = false;
	
	public openHABRest() {
		init();
	}
	
	public openHABRest(String configPath) {
		this.configPath = configPath;
		init();
	}
	
	private void init() {
		this.client = new OkHttpClient();
		
		String configString;
		//Load configuration file; default: config.json
		try {
			configString = new String(Files.readAllBytes(Paths.get(this.configPath)), StandardCharsets.UTF_8);
			config = new JSONObject(configString);
		} catch (IOException e) {
			System.err.println("Could not open configuration file!");
			e.printStackTrace();
		}
		
		this.address = config.optString("address", "http://localhost:8080/rest/");
		this.itemConfig = config.optJSONArray("items");

		//FEATURE: Just In Time item request?
		this.requestItems();
	}
	
	//TODO: request item history
	/**
	 * Request all openHAB items specified in the config file and store them in a hashmap by name and alias
	 */
	private void requestItems() {
		if (this.itemConfig == null) {
			System.err.println("No items to be requested.");
			System.exit(1);
		}
		
		for (int i = 0; i < this.itemConfig.length(); i++) {
			JSONObject conf = itemConfig.getJSONObject(i);
			JSONObject obj = this.doGetRequest("items/" + conf.getString("openHABName"));
			this.itemHM.put(obj.getString("name"), obj);
			if (conf.has("alias")) {
				this.itemHM.put(conf.getString("alias"), obj);
			}
		}
	}
	
	public void printItemConf() {
		System.out.println(this.itemConfig.toString());
	}
	public void printItems() {
		System.out.print(this.itemHM.toString());
	}
	
//-------------API-------------
	/**
	 * Sets the value of an item in openHAB.
	 * @param vars
	 * @return
	 */
	public void setItem(String itemName, Object value) {
		JSONObject item = itemHM.get(itemName);
		item.put("state", openHABRest.valueToString(item.getString("type"), value));		
		this.doPostRequest("items/" + item.getString("name"), openHABRest.valueToString(item.getString("type"), value));
	}
	
	/**
	 * Get the raw JSON object of an item
	 * @param name
	 * @return
	 */
	public JSONObject getRaw(String name) {
		if (!this.justInTime) {
			if (this.itemHM.containsKey(name)) {
				return this.itemHM.get(name);
			}
			System.err.println("Could not find item with name \"" + name + "\" in requested items.");
			System.exit(1);
			return null;
		} else {
			return this.doGetRequest("items/" + name);
		}
	}
	
	//TODO: handle undefined states: "NULL"
	//TODO: implement missing item types
	public Color getColor(String name) {
		String[] raw = this.getRaw(name).getString("state").split(",");
		return new Color(Byte.parseByte(raw[0]), Byte.parseByte(raw[1]), Byte.parseByte(raw[2]));
	}
	public boolean 	getContact(String name) {return this.getRaw(name).getString("state") == "OPEN";}
	//public Date 	getDate() {};
	public double	getDimmer(String name)	{return this.getRaw(name).getDouble("state");}
					//Group
	//public Image	getImage() {};
	public double[]	getLocation(String name) {
		String[] raw = this.getRaw(name).getString("state").split(",");
		return new double[]{Double.parseDouble(raw[0]), Double.parseDouble(raw[1]), Double.parseDouble(raw[2])};
	};
	public double 	getNumber(String name) {return this.getRaw(name).getDouble("state");}
	public double[]	getNumbers(String name) {
		String[] raw = this.getRaw(name).getString("state").split(",");
		double[] numbers = new double[raw.length];
		for (int i = 0; i < raw.length; i++) {numbers[i] = Double.parseDouble(raw[i]);};
		return numbers;
	};
					//Player
	public double	getRollershutter(String name) {return this.getRaw(name).getDouble("state");}
	public String	getString(String name) {return this.getRaw(name).getString("state");}
	public boolean	getSwitch(String name) {return this.getRaw(name).getString("state") == "ON";}

	
	/**
	 * Converts a value to a String, matching the item type.
	 * 
	 * @param type
	 * @param value
	 * @return Value as openHAB-compatible String
	 */
	public static String valueToString(String type, Object value) {
		switch(type) {
		case "String":
			if (value instanceof String) {
				return (String) value;
			}
			break;
		case "Switch":
			if (value instanceof Boolean) {
				return (boolean) value ? "ON" : "OFF";
			} else if (value instanceof Integer) {
				return (int) value > 0 ? "ON" : "OFF";
			}
			break;
		case "Number":
			if (value instanceof Double
				||	value instanceof Float
				||	value instanceof Integer) {
				return value.toString();
			}
			break;
		case "Contact":
			if (value instanceof Boolean) {
				return (boolean) value ? "Open" : "Closed";
			}
			break;
		default:
			System.err.println("Type / Value pair does not match or is not supported!\n"
								+ "Type: " + type
								+ "Value: " + value.toString()
								+ "Value-Type: " + value.getClass().toString());
			System.exit(1);
		}
		
		System.err.println("Value does not match the item Type: \n" + type + " : " + value.toString());
		System.exit(1);
		
		return null;
	}
	
//-------------REST-------------
	
	//TODO: Handle 404 item not found responses
	//TODO: Improve flexibility
	
    public JSONObject doGetRequest(String item) {
        Request request = new Request.Builder()
                                .url(this.address + item)
                                .build();
        Response response;
        try {
        	response = this.client.newCall(request).execute();
        	
        	String responseBody = response.body().string();
        	if (!responseBody.isBlank() && !responseBody.isEmpty()){
	        	JSONObject response_json = new JSONObject(responseBody);
	        	//TODO: optionally continue with warning
	        	if (response_json.has("error")) {
	        		System.err.println("Received error from POST request:");
	        		System.err.println(response_json.get("error"));
	        		System.exit(1);
	        	}
	            return response_json;
        	} else {
	        	return null;
	        }
        } catch (Exception e) {
        	System.err.println("Failed to connect to openHAB at address " + this.address + "\nwhile requesting \"" + item + "\": \n" + e.toString());
        	System.exit(1);
        }
        return null;
    }
    
    public JSONObject doPostRequest(String item, String body) {
    	RequestBody formBody = RequestBody.create(MediaType.parse("text/plain"), body);
    	
        Request request = new Request.Builder()
                                .url(this.address + item)
                                .post(formBody)
                                .build();
        System.out.println(request.toString());
        Response response;
        try {
        	response = this.client.newCall(request).execute();
        	
 			String responseBody = response.body().string();
        	if (!responseBody.isBlank() && !responseBody.isEmpty()){
	        	JSONObject response_json = new JSONObject(responseBody);
	        	//TODO: optionally continue with warning
	        	if (response_json.has("error")) {
	        		System.err.println("Received error from POST request:");
	        		System.err.println(response_json.get("error"));
	        		System.exit(1);
	        	}
	            return response_json;
        	} else {
	        	return null;
	        }

        } catch (IOException e) {
        	System.err.println("Failed to connect to openHAB at address " + this.address + "\nwhile requesting \"" + item + "\": \n" + e.toString());
        	e.printStackTrace();
        	System.exit(1);
        }
        return null;
    }
    
    public JSONObject doPutRequest(String item, String body) {
    	System.out.println(item);
    	System.out.println(body);
    	RequestBody formBody = RequestBody.create(MediaType.parse("application/json"), body);
    	
        Request request = new Request.Builder()
                                .url(this.address + item)
                                .post(formBody)
                                .build();
    	
        Response response;
        try {
        	response = this.client.newCall(request).execute();

        	String responseBody = response.body().string();
        	if (!responseBody.isBlank() && !responseBody.isEmpty()){
	        	JSONObject response_json = new JSONObject(responseBody);
	        	//TODO: optionally continue with warning
	        	if (response_json.has("error")) {
	        		System.err.println("Received error from POST request:");
	        		System.err.println(response_json.get("error"));
	        		System.exit(1);
	        	}
	            return response_json;
        	} else {
	        	return null;
	        }
        } catch (Exception e) {
        	System.err.println("Failed to connect to openHAB at address " + this.address + "\nwhile requesting \"" + item + "\": \n" + e.toString());
        	System.exit(1);
        }
        return null;
    }
    
    public JSONObject doDeleteRequest(String item) {
        Request request = new Request.Builder()
                                .url(this.address + item)
                                .delete()
                                .build();
        Response response;
        try {
        	response = this.client.newCall(request).execute();

        	String responseBody = response.body().string();
        	if (!responseBody.isBlank() && !responseBody.isEmpty()){
	        	JSONObject response_json = new JSONObject(responseBody);
	        	//TODO: optionally continue with warning
	        	if (response_json.has("error")) {
	        		System.err.println("Received error from POST request:");
	        		System.err.println(response_json.get("error"));
	        		System.exit(1);
	        	}
	            return response_json;
        	} else {
	        	return null;
	        }
        } catch (IOException e) {
        	System.err.println("Failed to connect to openHAB at address " + this.address + "\nwhile requesting \"" + item + "\": \n" + e.toString());
        	e.printStackTrace();
        	System.exit(1);
        }
        return null;
    }
  
    
//-------------MAIN-------------
    //Keep main for fast testing
    /*
    public static void main(String[] args) {
    	openHABRest o = new openHABRest();
    	openHABSchedule s = new openHABSchedule(o);
    	o.setItem("gp_dev_water", 0);
    	return;
    }
    */
}