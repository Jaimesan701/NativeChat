package es.um.redes.nanoChat.server;

import es.um.redes.nanoChat.messageFV.*;



import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import es.um.redes.nanoChat.server.roomManager.NCRoomManager;
import es.um.redes.nanoChat.server.roomManager.NCSimpleRoom;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.ArrayList;

/**
 * A new thread runs for each connected client
 */
public class NCServerThread extends Thread {
	
	private Socket socket = null; // manager global compartido entre los Threads
	private NCServerManager serverManager = null;
	//Input and Output Streams
	private DataInputStream dis;
	private DataOutputStream dos;
	//Usuario actual al que atiende este Thread
	String user;
	//RoomManager actual (dependerá de la sala a la que entre el usuario)
	NCRoomManager roomManager;
	//Sala actual
	String currentRoom;

	//Inicialización de la sala
	public NCServerThread(NCServerManager manager, Socket socket) throws IOException {
		super("NCServerThread");
		this.socket = socket;
		this.serverManager = manager;
	}

	//Main loop
	public void run() {
		try {
			//Se obtienen los streams a partir del Socket
			dis = new DataInputStream(socket.getInputStream());
			dos = new DataOutputStream(socket.getOutputStream());
			//En primer lugar hay que recibir y verificar el nick
			receiveAndVerifyNickname();
			System.out.println("Registro hecho usuario: "+ user);
			//Mientras que la conexión esté activa entonces...
			while (true) {
				//DONE Obtenemos el mensaje que llega y analizamos su código de operación
				NCMessage message = NCMessage.readMessageFromSocket(dis);
				switch (message.getOpcode()) {
				//DONE 1) si se nos pide la lista de salas se envía llamando a sendRoomList();
					case NCMessage.OP_GET_ROOMLIST:
						System.out.println("Procesando Roomlist from client : " + user);
						this.sendRoomList();
						break;
				//DONE 2) Si se nos pide entrar en la sala entonces obtenemos el RoomManager de la sala,
				//DONE 2) notificamos al usuario que ha sido aceptado y procesamos mensajes con processRoomMessages()
				//DONE 2) Si el usuario no es aceptado en la sala entonces se le notifica al cliente
					case NCMessage.OP_ENTER_ROOM:
						System.out.println("Procesando EnterRoom from client : " + user);
						NCRoomManager room = serverManager.enterRoom(user, ((NCQueryMessage)message).getName(), socket);
						if (room == null) {
							NCMessage response = (NCOneFieldMessage) NCMessage.makeNCOneFieldMessage(NCMessage.OP_ENTER_DENIED);
							String rawResponse = ((NCOneFieldMessage)response).toEncodedString();
							dos.writeUTF(rawResponse);
						}else {
							NCMessage response = (NCOneFieldMessage) NCMessage.makeNCOneFieldMessage(NCMessage.OP_ENTER_OK);
							String rawResponse = ((NCOneFieldMessage)response).toEncodedString();
							dos.writeUTF(rawResponse);
							currentRoom = room.getDescription().roomName;
							roomManager = room;
							roomManager.sendEnterMessage(user);
							processRoomMessages();
						}
							
						break;
					case NCMessage.OP_CREATE:
						System.out.println("Procesando Create from client : " + user);
						boolean resultado  = serverManager.registerRoomManager(new NCSimpleRoom());
						if(!resultado) {
							NCOneFieldMessage m = (NCOneFieldMessage) NCMessage.makeNCOneFieldMessage(NCMessage.OP_CREATE_FAIL);
							dos.writeUTF(m.toEncodedString());
						}else {
							NCOneFieldMessage m = (NCOneFieldMessage) NCMessage.makeNCOneFieldMessage(NCMessage.OP_CREATE);
							dos.writeUTF(m.toEncodedString());
						}
							
						break;
				}
			}
		} catch (Exception e) {
			//If an error occurs with the communications the user is removed from all the managers and the connection is closed
			System.out.println("* User "+ user + " disconnected.");
			//e.printStackTrace();
			serverManager.leaveRoom(user, currentRoom);
			serverManager.removeUser(user);
		}
		finally {
			if (!socket.isClosed())
				try {
					socket.close();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
		}
	}

	// Obtenemos el nick y solicitamos al ServerManager que verifique si está duplicado
	private void receiveAndVerifyNickname() throws IOException {

		//DONE Entramos en un bucle hasta comprobar que alguno de los nicks proporcionados no está duplicado
		//DONE Extraer el nick del mensaje
		//DONE Validar el nick utilizando el ServerManager - addUser()
		//DONE Contestar al cliente con el resultado (éxito o duplicado)
		boolean userOK = false;
		while(!userOK) {
			//La lógica de nuestro programa nos obliga a que haya un nick registrado antes de proseguir
			NCQueryMessage roomMessage = (NCQueryMessage) NCMessage.readMessageFromSocket(dis);
			String nick = roomMessage.getName();
			userOK = serverManager.addUser(nick);

			if(userOK) {
				NCOneFieldMessage nickOkMessage = (NCOneFieldMessage) NCMessage.makeNCOneFieldMessage(NCMessage.OP_NICK_OK);
				String rawResponseOk = nickOkMessage.toEncodedString();
				user = nick;
				dos.writeUTF(rawResponseOk);
				
			}
			else {
				NCOneFieldMessage nickDuplicated = (NCOneFieldMessage) NCMessage.makeNCOneFieldMessage(NCMessage.OP_NICK_DUPLICATED);
				String rawResponseDuplicated = nickDuplicated.toEncodedString();
				dos.writeUTF(rawResponseDuplicated);
			}
		}
	}

	// Mandamos al cliente la lista de salas existentes
	private void sendRoomList() throws IOException {
		//DONE La lista de salas debe obtenerse a partir del RoomManager y después enviarse mediante su mensaje correspondiente
		ArrayList<NCRoomDescription> rooms = (ArrayList<NCRoomDescription>) serverManager.getRoomList();
		NCSendRoomListMessage message = (NCSendRoomListMessage) NCMessage.makeNCSendRoomListMessage(NCMessage.OP_SEND_ROOMLIST, rooms);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
	}

	private void processRoomMessages()  {
		// DONE Comprobamos los mensajes que llegan hasta que el usuario decida salir de
				// la sala
				System.out.println("Client " + user + " is in room " + currentRoom);
				boolean exit = false;
				while (!exit) {
					// DONE Se recibe el mensaje enviado por el usuario
					// DONE Se analiza el código de operación del mensaje y se trata en consecuencia
					NCMessage message = null;
					try {
						message = NCMessage.readMessageFromSocket(dis);
					} catch (IOException e) {
						// DONE Auto-generated catch block
						e.printStackTrace();
					}

					// DONE EXIT Y SEND
					switch (message.getOpcode()) {
					case NCMessage.OP_GET_INFO_ROOM:
						System.out.println("Procesando Roomlist from client : " + user);
						ArrayList<NCRoomDescription> descriptions = new ArrayList<NCRoomDescription>();
						descriptions.add(roomManager.getDescription());
						NCSendRoomListMessage response = (NCSendRoomListMessage) NCMessage
								.makeNCSendRoomListMessage(NCMessage.OP_SEND_ROOMLIST, descriptions);
						try {
							dos.writeUTF(response.toEncodedString());
						} catch (IOException e) {
							// DONE Auto-generated catch block+
							e.printStackTrace();
						}
						break;
					case NCMessage.OP_SEND :
						System.out.println("Procesando Send from client : " + user);
						String mensajeParaBroadcast = ((NCSendMessage) message).getMensaje();
						String usuarioEmisor = ((NCSendMessage) message).getEmisor();
						try {
							roomManager.broadcastMessage(usuarioEmisor, mensajeParaBroadcast);
						} catch (IOException e) {
							// DONE Auto-generated catch block
							e.printStackTrace();

						}
						break;
					case NCMessage.OP_PRIVATE :
						System.out.println("Procesando Private from client : " + user);
						String mensajeParaUsuario = ((NCPrivateMessage) message).getMensaje();
						String usuarioEmisorPrivado = ((NCPrivateMessage) message).getEmisor();
						String usuarioReceptor = ((NCPrivateMessage) message).getReceptor();
						int resultado = roomManager.sendPrivate(mensajeParaUsuario, usuarioEmisorPrivado, usuarioReceptor);
						NCOneFieldMessage m = null;
						if(resultado == 1) {
							m = (NCOneFieldMessage) NCMessage.makeNCOneFieldMessage(NCMessage.OP_NOT_EXISTS);
							try {
								dos.writeUTF(m.toEncodedString());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}else if(resultado == 2) {
							m = (NCOneFieldMessage) NCMessage.makeNCOneFieldMessage(NCMessage.OP_SAME_USER);
							try {
								dos.writeUTF(m.toEncodedString());
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
							
						break;
					case NCMessage.OP_RENAME :
						System.out.println("Procesando Rename from client : " + user);
						String newName = ((NCQueryMessage) message).getName();
						roomManager = serverManager.renameRoom(currentRoom, newName);
						currentRoom = newName;
						break;
					case NCMessage.OP_EXIT:
						System.out.println("Procesando Exit from client : " + user);
						roomManager.sendExitMessage(user);
						serverManager.leaveRoom(user, currentRoom);
						currentRoom = null;
						exit = true;
						break;
					}
			}
	}
}
