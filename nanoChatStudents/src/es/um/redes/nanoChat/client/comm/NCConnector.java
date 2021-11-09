package es.um.redes.nanoChat.client.comm;


import es.um.redes.nanoChat.messageFV.NCMessage;
import es.um.redes.nanoChat.messageFV.NCOneFieldMessage;
import es.um.redes.nanoChat.messageFV.NCPrivateMessage;
import es.um.redes.nanoChat.messageFV.NCQueryMessage;
import es.um.redes.nanoChat.messageFV.NCSendMessage;
import es.um.redes.nanoChat.messageFV.NCSendRoomListMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.List;

// Esta clase proporciona la funcionalidad necesaria para intercambiar mensajes entre el cliente y el servidor de NanoChat
public class NCConnector {

	// ATRIBUTOS
	private Socket socket;
	protected DataOutputStream dos;
	protected DataInputStream dis;

	// CONSTRUCTOR
	public NCConnector(InetSocketAddress serverAddress) throws UnknownHostException, IOException {
		socket = new Socket(serverAddress.getAddress(), serverAddress.getPort());
		dos = new DataOutputStream(socket.getOutputStream());
		dis = new DataInputStream(socket.getInputStream());
	}

	// metodo para mandar un mensaje con el nickname a registrar
	public boolean registerNickname(String nick) throws IOException {
		NCQueryMessage message = (NCQueryMessage) NCMessage.makeNCQueryMessage(NCMessage.OP_NICK, nick);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCMessage response = NCMessage.readMessageFromSocket(dis);
		if(response.getOpcode() == NCMessage.OP_NICK_OK)
			return true;
		else
			return false;
	}
	// metodo para mandar un mensaje para obtener una lista con los descriptores de cada sala
	public List<NCRoomDescription> getRooms() throws IOException {

		NCOneFieldMessage message = (NCOneFieldMessage) NCMessage.makeNCOneFieldMessage(NCMessage.OP_GET_ROOMLIST);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCSendRoomListMessage response = (NCSendRoomListMessage) NCMessage.readMessageFromSocket(dis);	
		return response.getRooms();
	}
	
	
	// metodo para mandar un mensaje con una peticion para entrar a un sala de chat
	public boolean enterRoom(String room) throws IOException {

		NCQueryMessage message = (NCQueryMessage) NCMessage.makeNCQueryMessage(NCMessage.OP_ENTER_ROOM, room);
	
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCMessage response = NCMessage.readMessageFromSocket(dis);
		if(response.getOpcode() == NCMessage.OP_ENTER_OK)
			return true;
		else
			return false;
	}
	

	public boolean isDataAvailable() throws IOException {
		return (dis.available() != 0);
	}
	
	// metodo para mandar un mensaje broadcast a una sala de chat
	public boolean sendMessage(String mensaje, String usuario){
		NCMessage message = (NCSendMessage) NCMessage.makeNCSendMessage(NCMessage.OP_SEND, mensaje,usuario);
		String rawMessage = ((NCSendMessage)message).toEncodedString();
		try {
			dos.writeUTF(rawMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	public NCMessage receiveMessage(){
		NCMessage message = null;
		try {
			message =  NCMessage.readMessageFromSocket(dis);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		return message;
	}
	// metodo para mandar un mensaje para abandonar una sala
	public void leaveRoom() throws IOException {
		NCOneFieldMessage message = (NCOneFieldMessage) NCMessage.makeNCOneFieldMessage(NCMessage.OP_EXIT);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
	}
	// metodo para mandar un mensaje para obtener la informacion de una sala
	public NCRoomDescription getRoomInfo(String room) throws IOException {
		NCOneFieldMessage message = (NCOneFieldMessage) NCMessage.makeNCOneFieldMessage(NCMessage.OP_GET_INFO_ROOM);
		String rawMessage = message.toEncodedString();
		dos.writeUTF(rawMessage);
		NCMessage response = NCMessage.readMessageFromSocket(dis);
		return ((NCSendRoomListMessage)response).getRooms().get(0);
	}
	// metodo para mandar un mensaje para informar al servidor de que debe crear una nueva sala
	public boolean createRoom() throws IOException {
		NCOneFieldMessage message = (NCOneFieldMessage) NCMessage.makeNCOneFieldMessage(NCMessage.OP_CREATE);
		String rawMessage = message.toEncodedString();
		
		try {
			dos.writeUTF(rawMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
		NCMessage r = NCMessage.readMessageFromSocket(dis);
		if(r.getOpcode() == NCMessage.OP_CREATE_FAIL)
			return false;
		else
			return true;
	}
	// metodo para mandar un mensaje privado a un usuario de la sala de chat
	public void sendPrivate(String mensaje,String receptor,String emisor)   {
		NCPrivateMessage message = (NCPrivateMessage) NCMessage.makeNCPrivateMessage(NCMessage.OP_PRIVATE,mensaje,emisor,receptor);
		String rawMessage = message.toEncodedString();
		try {
			dos.writeUTF(rawMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
	}
	// metodo para mandar un mensaje con el nuevo nombre para renombrar una sala
	public void sendNewRoomNme(String name) {
		NCQueryMessage message = (NCQueryMessage) NCMessage.makeNCQueryMessage(NCMessage.OP_RENAME,name);
		String rawMessage = message.toEncodedString();
		try {
			dos.writeUTF(rawMessage);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	// metodo para desconectars del servidor
	public void disconnect() {
		try {
			if (socket != null) {
				socket.close();
			}
		} catch (IOException e) {
		} finally {
			socket = null;
		}
	}

}
