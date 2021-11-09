package es.um.redes.nanoChat.server;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;

import java.net.Socket;
import java.util.*;

/**
 * Esta clase contiene el estado general del servidor (sin la lógica relacionada con cada sala particular)
 */
class NCServerManager {

	// Primera habitación del servidor
	final static byte INITIAL_ROOM = 'A';
	final static String ROOM_PREFIX = "Room";
	// Siguiente habitación que se creará
	byte nextRoom;
	// Usuarios registrados en el servidor
	private Set<String> users = new HashSet<String>();
	// Habitaciones actuales asociadas a sus correspondientes RoomManagers
	private Map<String,NCRoomManager> rooms = new HashMap<String,NCRoomManager>();

	NCServerManager() {
		nextRoom = INITIAL_ROOM;
	}
	
	
	// MéDONE para registrar un RoomManager
	public boolean registerRoomManager(NCRoomManager rm) {
		//DONE Dar soporte para que pueda haber más de una sala en el servidor
		if(rooms.keySet().size() < 26) {
			String roomName = ROOM_PREFIX + (char) nextRoom;
			rooms.put(roomName, rm);
			rm.setRoomName(roomName);
			nextRoom++;
			return true;
		}else
			return false;
	
	}

	// Devuelve la descripción de las salas existentes
	public synchronized List<NCRoomDescription> getRoomList() {
		//DONE Pregunta a cada RoomManager cuál es la descripción actual de su sala
		//DONE Añade la información al ArrayList
		ArrayList<NCRoomDescription> roomsDescriptions = new ArrayList<NCRoomDescription>();
		for (String s : rooms.keySet()){
			roomsDescriptions.add(rooms.get(s).getDescription());
		}
		return roomsDescriptions;
	}

		public synchronized void showRooms() {
			for (String s : rooms.keySet()){
				System.out.println(s);
			}
		}
		
	// Intenta registrar al usuario en el servidor.
	public synchronized boolean addUser(String user) {
		boolean userAdded = users.add(user);
		return userAdded;
	}

	// Elimina al usuario del servidor
	public synchronized void removeUser(String user) {
		//DONE Elimina al usuario del servidor
		users.remove(user);
	}

	// Un usuario solicita acceso para entrar a una sala y registrar su conexión en ella
	public synchronized NCRoomManager enterRoom(String u, String room, Socket s) {
		//DONE Verificamos si la sala existe
		boolean existeSala = rooms.containsKey(room);
		
		//DONE Decidimos qué hacer si la sala no existe (devolver error O crear la sala)
		if(!existeSala)
			return null;
		NCRoomManager descriptor = rooms.get(room);
		descriptor.registerUser(u, s);
		return rooms.get(room);
		//DONE Si la sala existe y si es aceptado en la sala entonces devolvemos el RoomManager de la sala
	}

	// Un usuario deja la sala en la que estaba
	public synchronized void leaveRoom(String u, String room) {
		//DONE Verificamos si la sala existe
		//DONE Si la sala existe sacamos al usuario de la sala
		//DONE Decidir qué hacer si la sala se queda vacía
		if(rooms.containsKey(room))
			rooms.get(room).removeUser(u);
	}
	
	// Un usuario deja la sala en la que estaba
	public synchronized NCRoomManager renameRoom(String room, String newName) {
		NCRoomManager aux = null;
		if(rooms.containsKey(room))
			aux = rooms.get(room);
		rooms.remove(room);
		aux.setRoomName(newName);
		rooms.put(newName, aux);
		return aux;
	}
}
