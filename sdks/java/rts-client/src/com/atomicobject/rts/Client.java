package com.atomicobject.rts;
import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

public class Client {
	
	BufferedReader input;
	OutputStreamWriter out;
	//Tile gameMap [][] = new Tile();
	//Initialized object for holding updates from server
	LinkedBlockingQueue<Map<String, Object>> updates;
	//Holds a list of units
	Map<Long, Unit> units;
	
    // Field to store the initial game information
    private JSONObject gameInfo;
//    int mapWidth = ((Long) gameInfo.get("map_width")).intValue();
//    int mapHeight = ((Long) gameInfo.get("map_height")).intValue();
//    JSONObject[][] mapArray = new JSONObject[2 * mapWidth + 1][2 * mapHeight + 1];
    
	public Client(Socket socket) {
		updates = new LinkedBlockingQueue<Map<String, Object>>();
		units = new HashMap<Long, Unit>();
		try {
			input = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			out = new OutputStreamWriter(socket.getOutputStream());
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void start() {
		System.out.println("Starting client threads ...");
		new Thread(() -> readUpdatesFromServer()).start();
		new Thread(() -> runClientLoop()).start();
	}
	
	//Takes in map update from server then appends it to the end of the persistant updates map
	public void readUpdatesFromServer() {
		String nextLine;
		try {
			while ((nextLine = input.readLine()) != null) {
				@SuppressWarnings("unchecked")
				Map<String, Object> update = (Map<String, Object>) JSONValue.parse(nextLine.trim());
				
                // Store gameInfo if it hasn't been set yet
                if (gameInfo == null) {
                    gameInfo = new JSONObject(update);
                    System.out.println("Game info received: " + gameInfo.toJSONString());
                }
				
				updates.add(update);
			}
		} catch (IOException e) {
			// exit thread
		}		
	}

	public void runClientLoop() {
		System.out.println("Starting client update/command processing ...");
		try {
			while (true) {
				//While the server is still running, do these two processes one at a time in loop
				processUpdateFromServer();
				respondWithCommands();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		closeStreams();
	}
	
	//Takes in most recent update from server
	private void processUpdateFromServer() throws InterruptedException {
		//Instantiates update as the most recent version of the map
		Map<String, Object> update = updates.take();
		if (update != null) {
			
			System.out.println("Processing update: " + update);
			@SuppressWarnings("unchecked")
			
			Collection<JSONObject> tileUpdates = (Collection<JSONObject>) update.get("tile_updates");
			tileUpdate(tileUpdates);
			Collection<JSONObject> unitUpdates = (Collection<JSONObject>) update.get("unit_updates");
			addUnitUpdate(unitUpdates);
		}
	}
	
	//Adds a new unit to the units list upon unit creation
	private void tileUpdate(Collection<JSONObject> tileUpdates) {

		tileUpdates.forEach((tile) -> {
			
		});
	}
	
	//Adds a new unit to the units list upon unit creation
	private void addUnitUpdate(Collection<JSONObject> unitUpdates) {
		unitUpdates.forEach((unitUpdate) -> {
			Long id = (Long) unitUpdate.get("id");
			String type = (String) unitUpdate.get("type");
			if (!type.equals("base")) {
				units.put(id, new Unit(unitUpdate));
			}
		});
	}

	private void respondWithCommands() throws IOException {
		if (units.size() == 0) return;
		
		JSONArray commands = buildCommandList();		
		sendCommandListToServer(commands);
	}

	@SuppressWarnings("unchecked")
	private JSONArray buildCommandList() {

		//Gets an array of ids from the list of units
		Long[] unitIds = units.keySet().toArray(new Long[units.size()]);
		
		JSONArray commands = new JSONArray();
		
		for(Long unitId : unitIds) {
			Unit unit = units.get(unitId);
			JSONObject Command = new JSONObject();	
			if(unit.status.equals("idle")) {
			switch (unit.type) {
				case "tank":
					Command = handleTank(unit);
					break;
				case "scout":
					Command = handleScout(unit);
					break;
				case "worker":
					Command = handleWorker(unit);
					break;
				case "base":
					Command = handleBase(unit);
					break;
			}
			}
			commands.add(Command);
		}

		return commands;
	}

	@SuppressWarnings("unchecked")
	private void sendCommandListToServer(JSONArray commands) throws IOException {
		JSONObject container = new JSONObject();
		container.put("commands", commands);
		System.out.println("Sending commands: " + container.toJSONString());
		out.write(container.toJSONString());
		out.write("\n");
		out.flush();
	}

	private void closeStreams() {
		closeQuietly(input);
		closeQuietly(out);
	}

	private void closeQuietly(Closeable stream) {
		try {
			stream.close();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}
	
	private JSONObject handleTank(Unit unit) {
		JSONObject command = new JSONObject();
		//Instantiates possible directions
		String[] directions = {"N","E","S","W"};
		String direction = directions[(int) Math.floor(Math.random() * 4)];
		
		command.put("command", "MOVE");
		command.put("dir", direction);
		command.put("unit", unit.id);
		
		return command;
	}
	private JSONObject handleScout(Unit unit) {
		JSONObject command = new JSONObject();
		//Instantiates possible directions
		String[] directions = {"N","E","S","W"};
		String direction = directions[(int) Math.floor(Math.random() * 4)];
		
		command.put("command", "MOVE");
		command.put("dir", direction);
		command.put("unit", unit.id);
		
		return command;
	}
	private JSONObject handleWorker(Unit unit) {
		JSONObject command = new JSONObject();	
		//Instantiates possible directions
		String[] directions = {"N","E","S","W"};
		String direction = directions[(int) Math.floor(Math.random() * 4)];
	
		
		command.put("command", "MOVE");
		command.put("dir", direction);
		command.put("unit", unit.id);
		
		return command;
	}
	private JSONObject handleBase(Unit unit) {
		JSONObject command = new JSONObject();
		command = null;
		return command;
	}
}
