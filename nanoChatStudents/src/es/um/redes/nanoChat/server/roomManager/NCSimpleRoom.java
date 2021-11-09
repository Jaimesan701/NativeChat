package es.um.redes.nanoChat.server.roomManager;

import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;

import es.um.redes.nanoChat.messageFV.NCMessage;
import es.um.redes.nanoChat.messageFV.NCQueryMessage;
import es.um.redes.nanoChat.messageFV.NCSendMessage;

public class NCSimpleRoom extends NCRoomManager {

	private HashMap<String, Socket> socketUsuarios;
	private NCRoomDescription descriptor;

	public NCSimpleRoom() {
		socketUsuarios = new HashMap<>();
		descriptor = new NCRoomDescription(roomName, new LinkedList<>(), 0);
	}

	// registra un nuevo usuario en el servidor
	@Override
	public boolean registerUser(String u, Socket s) {
		// TODO Auto-generated method stub
		boolean añadido = descriptor.members.add(u);
		socketUsuarios.put(u, s);

		return añadido;
	}

	// manda un mensaje a todos los usarios de la sala
	@Override
	public void broadcastMessage(String u, String message) throws IOException {
		DataOutputStream dos;
		for (String user : socketUsuarios.keySet()) {
			if (!user.equals(u)) {
				Socket socket = socketUsuarios.get(user);
				NCSendMessage messageToSend = (NCSendMessage) NCMessage.makeNCSendMessage(NCMessage.OP_SEND, message,
						u);
				String rawMessage = messageToSend.toEncodedString();
				dos = new DataOutputStream(socket.getOutputStream());
				dos.writeUTF(rawMessage);
			}
			
			descriptor.timeLastMessage = new Date().getTime();
		}

	}

	// elimina a un usuario de la sala
	@Override
	public void removeUser(String u) {
		// TODO Auto-generated method stub

		socketUsuarios.remove(u);

		descriptor.members.remove(u);

	}

	// establece el nombre de sala
	@Override
	public void setRoomName(String roomName) {
		// TODO Auto-generated method stub
		this.roomName = roomName;
		descriptor.roomName = roomName;

	}

	// obtiene el descriptor de la sala de chat
	@Override
	public NCRoomDescription getDescription() {
		return descriptor;
	}

	// obtiene el numero de usuarios en la sala
	@Override
	public int usersInRoom() {
		// TODO Auto-generated method stub
		return descriptor.members.size();
	}

	// manda un mensaje privado a un usuario de la sala
	@Override
	public int sendPrivate(String mensaje, String emisor, String receiver) {
		DataOutputStream dos = null;
		if(!descriptor.members.contains(receiver))
			return 1;
		else if(emisor.equals(receiver))
			return 2;
		else {
			for (String user : socketUsuarios.keySet()) {
				if (!user.equals(emisor) && user.equals(receiver)) {
					Socket socket = socketUsuarios.get(user);
					NCSendMessage messageToSend = (NCSendMessage) NCMessage.makeNCSendMessage(NCMessage.OP_SEND, mensaje,
							emisor);
					String rawMessage = messageToSend.toEncodedString();
					try {
						dos = new DataOutputStream(socket.getOutputStream());
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					try {
						dos.writeUTF(rawMessage);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
			descriptor.timeLastMessage = new Date().getTime();;
			return 0;
		}
	}

	@Override
	public void sendExitMessage(String u) {
		String mensajeParaSala = "El usuario " + u + " ha salido de la sala";
		for (String user : socketUsuarios.keySet()) {
			if (!user.equals(u)) {
				Socket socket = socketUsuarios.get(user);
				NCQueryMessage messageToSend = (NCQueryMessage) NCMessage.makeNCQueryMessage(NCMessage.OP_EXIT_OR_ENTER,
						mensajeParaSala);
				String rawMessage = messageToSend.toEncodedString();
				DataOutputStream dos = null;
				try {
					dos = new DataOutputStream(socket.getOutputStream());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					dos.writeUTF(rawMessage);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	@Override
	public void sendEnterMessage(String u) {
		String mensajeParaSala = "El usuario " + u + " ha entrado en la sala";
		for (String user : socketUsuarios.keySet()) {
			if (!user.equals(u)) {
				Socket socket = socketUsuarios.get(user);
				NCQueryMessage messageToSend = (NCQueryMessage) NCMessage.makeNCQueryMessage(NCMessage.OP_EXIT_OR_ENTER,
						mensajeParaSala);
				String rawMessage = messageToSend.toEncodedString();
				DataOutputStream dos = null;
				try {
					dos = new DataOutputStream(socket.getOutputStream());
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				try {
					dos.writeUTF(rawMessage);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

}
