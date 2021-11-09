package es.um.redes.nanoChat.client.application;

import es.um.redes.nanoChat.client.comm.NCConnector;
import es.um.redes.nanoChat.client.shell.NCCommands;
import es.um.redes.nanoChat.client.shell.NCShell;
import es.um.redes.nanoChat.directory.connector.DirectoryConnector;
import es.um.redes.nanoChat.messageFV.NCMessage;
import es.um.redes.nanoChat.messageFV.NCQueryMessage;
import es.um.redes.nanoChat.messageFV.NCSendMessage;
import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;
import sun.font.CreatedFontTracker;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class NCController {
	// CONSTANTES
	private static final byte PRE_CONNECTION = 1; // estados del cliente de acuerdo con el autómata antes de conectarse
	private static final byte PRE_REGISTRATION = 2; // estados del cliente de acuerdo con el autómata una vez registrado
	private static final byte OUT_OF_ROOM = 3; // estados del cliente de acuerdo con el autómata una vez registrado
	private static final int PROTOCOL = 37; // código de protocolo implementado por este cliente

	// ATRIBUTOS
	private DirectoryConnector directoryConnector; // conector para enviar y recibir mensajes del directorio
	private NCConnector ncConnector; // conector para enviar y recibir mensajes con el servidor de NanoChat
	private NCShell shell; // shell para leer comandos de usuario de la entrada estándar
	private byte currentCommand; // último comando proporcionado por el usuario
	private String nickname; // nick del usuario
	private String room; // sala de chat en la que se encuentra el usuario (si está en alguna)
	private String chatMessage; // mensaje enviado o por enviar al chat
	private InetSocketAddress serverAddress; // dirección de internet del servidor de NanoChat
	private String receptor;
	private String newRoomName;
	private byte clientStatus = PRE_CONNECTION; // estado actual del cliente, de acuerdo con el autómata

	// CONSTRUCTOR
	public NCController() {
		shell = new NCShell();
	}

	// Devuelve el comando actual introducido por el usuario
	public byte getCurrentCommand() {
		return this.currentCommand;
	}

	// -------------------------------------- SHELL
	// ---------------------------------------------------------------------

	// Establece el comando actual
	public void setCurrentCommand(byte command) {
		currentCommand = command;
	}

	// Registra en atributos internos los posibles parámetros del comando tecleado
	// por el usuario
	public void setCurrentCommandArguments(String[] args) {
		// Comprobaremos también si el comando es válido para el estado actual del
		// autómata
		switch (currentCommand) {
		case NCCommands.COM_NICK:
			if (clientStatus == PRE_REGISTRATION)
				nickname = args[0];
			break;
		case NCCommands.COM_ENTER:
			room = args[0];
			break;
		case NCCommands.COM_SEND:
			chatMessage = args[0];
			break;
		case NCCommands.COM_RENAME:
			newRoomName = args[0];
			break;
		case NCCommands.COM_PRIVATE:
			chatMessage = args[1];
			receptor = args[0];
			break;
		default:
		}
	}

	// Procesa los comandos introducidos por un usuario que aún no está dentro de
	// una sala
	public void processCommand() {
		switch (currentCommand) {
		case NCCommands.COM_NICK:
			if (clientStatus == PRE_REGISTRATION)
				registerNickName();
			else
				System.out.println("* You have already registered a nickname (" + nickname + ")");
			break;
		case NCCommands.COM_ROOMLIST:
			// DONE LLamar a getAndShowRooms() si el estado actual del autómata lo permite
			if (clientStatus == OUT_OF_ROOM) {
				getAndShowRooms();
			} else {
				// DONE Si no está permitido informar al usuario
				System.out.println("* You must be registered");
			}

			break;
		case NCCommands.COM_ENTER:
			// DONE LLamar a enterChat() si el estado actual del autómata lo permite
			if (clientStatus == OUT_OF_ROOM)
				enterChat();
			else
				System.out.println("You must be registered and out of a room");
			// DONE Si no está permitido informar al usuario
			break;
		case NCCommands.COM_QUIT:
			// Cuando salimos tenemos que cerrar todas las conexiones y sockets abiertos
			ncConnector.disconnect();
			directoryConnector.close();
			break;
		case NCCommands.COM_CREATE:
			if (clientStatus == OUT_OF_ROOM) {
				createRoom();
			} else {
				// DONE Si no está permitido informar al usuario
				System.out.println("* You must be registered");
			}
			break;
		default:
		}
	}

	// MéDONE para leer un comando de la sala
	public void readRoomCommandFromShell() {
		// Pedimos un nuevo comando de sala al shell (pasando el conector por si nos
		// llega un mensaje entrante)
		shell.readChatCommand(ncConnector);
		// Establecemos el comando tecleado (o el mensaje recibido) como comando actual
		setCurrentCommand(shell.getCommand());
		// Procesamos los posibles parámetros (si los hubiera)
		setCurrentCommandArguments(shell.getCommandArguments());
	}

	// MéDONE para leer un comando general (fuera de una sala)
	public void readGeneralCommandFromShell() {
		// Pedimos el comando al shell
		shell.readGeneralCommand();
		// Establecemos que el comando actual es el que ha obtenido el shell
		setCurrentCommand(shell.getCommand());
		// Analizamos los posibles parámetros asociados al comando
		setCurrentCommandArguments(shell.getCommandArguments());
	}

	// MéDONE que comprueba si el usuario ha introducido el comando para salir de la
	// aplicación
	public boolean shouldQuit() {
		return currentCommand == NCCommands.COM_QUIT;
	}

	// ---------------------------------- IMPLEMENTACIÓN
	// COMANDOS--------------------------------------------------------

	// MéDONE para obtener el servidor de NanoChat que nos proporcione el directorio
	// a través de una conexión UDP
	public boolean getServerFromDirectory(String directoryHostname) {
		// Inicializamos el conector con el directorio y el shell
		System.out.println("* Connecting to the directory...");
		// Intentamos obtener la dirección del servidor de NanoChat que trabaja con
		// nuestro protocolo
		try {
			directoryConnector = new DirectoryConnector(directoryHostname);
			serverAddress = directoryConnector.getServerForProtocol(PROTOCOL);
		} catch (IOException e1) {
			serverAddress = null;
		}
		// Si no hemos recibido la dirección entonces nos quedan menos intentos
		if (serverAddress == null) {
			System.out.println("* Check your connection, the directory is not available.");
			return false;
		} else
			return true;
	}

	// MéDONE para establecer la conexión con el servidor de Chat a través de una
	// conexión TCP (a través del NCConnector)
	public boolean connectToChatServer() {
		try {
			// Inicializamos el conector para intercambiar mensajes con el servidor de
			// NanoChat (lo hace la clase NCConnector)
			ncConnector = new NCConnector(serverAddress);
		} catch (IOException e) {
			System.out.println("* Check your connection, the game server is not available.");
			serverAddress = null;
		}
		// Si la conexión se ha establecido con éxito informamos al usuario y cambiamos
		// el estado del autómata
		if (serverAddress != null) {
			System.out.println("* Connected to " + serverAddress);
			clientStatus = PRE_REGISTRATION;
			return true;
		} else
			return false;
	}
	// metodo para regitrar el nick en el servidor
	private void registerNickName() {
		try {
			boolean registered = ncConnector.registerNickname(nickname);
			if (registered) {
				System.out.println("* Your nickname is now " + nickname);
				clientStatus = OUT_OF_ROOM;
			} else
				System.out.println("* The nickname is already registered. Try a different one.");
		} catch (IOException e) {
			System.out.println("* There was an error registering the nickname");
		}
	}
	// metodo para mostar las salas
	private void getAndShowRooms() {
		List<NCRoomDescription> rooms = new ArrayList<>();

		try {
			rooms = ncConnector.getRooms();
		} catch (IOException e) {
			e.printStackTrace();
		}

		for (NCRoomDescription room : rooms) {
			System.out.println(room.toPrintableString());
		}
	}
	// metodo para entrar en una sala del chat 
	private void enterChat() {

		boolean enterRoomSuccess = false;
		try {
			enterRoomSuccess = ncConnector.enterRoom(room);
		} catch (IOException e) {
			e.printStackTrace();
		}
		if (enterRoomSuccess) {
			do {
				readRoomCommandFromShell();
				processRoomCommand();
			} while (currentCommand != NCCommands.COM_EXIT);
			System.out.println("* Your are out of the room");

		} else
			System.out.println("Enter room petition denied");

	}
	// metodo para procesor los comandos dentro de la sala de chat 
	private void processRoomCommand() {
		switch (currentCommand) {
		case NCCommands.COM_ROOMINFO:

			getAndShowInfo();
			break;
		case NCCommands.COM_SEND:
			sendChatMessage();
			break;
		case NCCommands.COM_RENAME:
			changeRoomName();
			break;
		case NCCommands.COM_SOCKET_IN:
			processIncommingMessage();
			break;
		case NCCommands.COM_PRIVATE:
			sendPrivate(chatMessage, receptor);
			break;
		case NCCommands.COM_EXIT:
			exitTheRoom();
		}
	}
	// metodo para obtener la informacion de una sola sala
	private void getAndShowInfo() {
		NCRoomDescription room = null;
		try {
			room = ncConnector.getRoomInfo(this.room);
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.out.println(room.toPrintableString());
	}
	// metodo para salir de la sala de chat
	private void exitTheRoom() {

		try {
			ncConnector.leaveRoom();
		} catch (IOException e) {
			e.printStackTrace();
		}
		clientStatus = OUT_OF_ROOM;
	}
	// metodo para mandar un mensaje a la sala de chat
	private void sendChatMessage() {
		boolean mensajeEnviado = ncConnector.sendMessage(chatMessage, nickname);
		if (!mensajeEnviado)
			System.out.println("El mensaje no ha podido enviarse");

	}
	// metodo para procesar un mensaje de la sala de chat
	private void processIncommingMessage() {

		NCMessage message = ncConnector.receiveMessage();
		switch (message.getOpcode()) {
		case NCMessage.OP_SEND:
			System.out.println(
					"[" + ((NCSendMessage) message).getEmisor() + "] " + ((NCSendMessage) message).getMensaje());
			break;
		case NCMessage.OP_EXIT_OR_ENTER:
			System.out.println(
					"[SERVER] " + ((NCQueryMessage) message).getName());
			break;
		case NCMessage.OP_NOT_EXISTS:
			System.out.println(
					"[SERVER]El usuario no se encuentra en la sala");
			break;
		case NCMessage.OP_SAME_USER:
			System.out.println(
					"[SERVER]No puedes enviarte un mensaje privado a ti mismo");
			break;
		}

	}
	// metodo para crear una nueva sala de chat
	public void createRoom() {
		boolean resultado = true;
		try {
			resultado = ncConnector.createRoom();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(!resultado)
			System.out.println("[SERVER]No es posible crear más salas");
	}
	// metodo para mandar un mensaje privado
	public void sendPrivate(String mensaje, String receptor) {
		ncConnector.sendPrivate(mensaje, receptor, nickname);
	}
	// metodo para cambiar el nombre de una sala de chat
	public void changeRoomName() {
		room = newRoomName;
		ncConnector.sendNewRoomNme(newRoomName);
	}
}
