package es.um.redes.nanoChat.messageFV;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;


public abstract class NCMessage {


	// CONSTANTES DE OPCODES
	//TODO Implementar el resto de los opcodes para los distintos mensajes
	public static final byte OP_INVALID_CODE = 0;
	public static final byte OP_NICK = 1;
	public static final byte OP_NICK_OK = 2;
	public static final byte OP_NICK_DUPLICATED = 3;
	public static final byte OP_GET_ROOMLIST = 4;
	public static final byte OP_SEND_ROOMLIST = 5;
	public static final byte OP_GET_INFO_ROOM = 6;
	public static final byte OP_SEND_INFO_ROOM = 7;
	public static final byte OP_ENTER_ROOM = 8;
	public static final byte OP_ENTER_OK = 9;
	public static final byte OP_ENTER_DENIED = 10;
	public static final byte OP_SEND = 11;
	public static final byte OP_EXIT = 12;
	public static final byte OP_QUIT = 13;
	public static final byte OP_CREATE = 14;
	public static final byte OP_PRIVATE = 15;
	public static final byte OP_RENAME = 16;
	public static final byte OP_EXIT_OR_ENTER = 17;
	public static final byte OP_NOT_EXISTS = 18;
	public static final byte OP_SAME_USER = 19;
	public static final byte OP_CREATE_FAIL = 20;

	

	// CONSTANTES CON LOS DELIMITADORES DE LOS MENSAJES DE FIELD:VALUE
	public static final char DELIMITER = ':';    //Define el delimitador
	public static final char END_LINE = '\n';    //Define el carácter de fin de línea

	// CONSTANTE
	public static final String OPCODE_FIELD = "operation";

	// ATRIBUTOS DE CLASE
	/**
	 * Códigos de los opcodes válidos  El orden
	 * es importante para relacionarlos con la cadena
	 * que aparece en los mensajes
	 */
	private static final Byte[] _valid_opcodes = {
			OP_NICK,OP_NICK_OK,OP_NICK_DUPLICATED,OP_GET_ROOMLIST,OP_SEND_ROOMLIST,OP_GET_INFO_ROOM,OP_SEND_INFO_ROOM,OP_ENTER_ROOM,OP_ENTER_OK,OP_ENTER_DENIED,
			OP_SEND,OP_EXIT,OP_QUIT,OP_CREATE,OP_PRIVATE,OP_RENAME,OP_EXIT_OR_ENTER,OP_NOT_EXISTS,OP_SAME_USER,OP_CREATE_FAIL
	};

	/**
	 * cadena exacta de cada orden
	 */
	private static final String[] _valid_operations_str = {
			"Nick", "NickOk", "NickDuplicated","GetRoomList","SendRoomList","GetInfoRoom","SendInfoRoom","EnterRoom","EnterRoomOK","EnterRoomDenied","Send","Exit","Quit",
			"Create","Private","Rename","ExitOrEnter","NotExists","SameUser","CreateFail"
	};

	private static Map<String, Byte> _operation_to_opcode;
	private static Map<Byte, String> _opcode_to_operation;

	// Código que inicializa correctamente las estructuras de datos
	// Se ejecuta cuando se instancia una clase compatible por primera vez
	static {
		_operation_to_opcode = new TreeMap<>();
		_opcode_to_operation = new TreeMap<>();
		for (int i = 0 ; i < _valid_operations_str.length; ++i)
		{
			_operation_to_opcode.put(_valid_operations_str[i].toLowerCase(), _valid_opcodes[i]);
			_opcode_to_operation.put(_valid_opcodes[i], _valid_operations_str[i]);
		}
	}

	// ATRIBUTO
	protected byte opcode;

	/**
	 * Transforma una cadena en el opcode correspondiente
	 */
	protected static byte operationToOpcode(String opStr) {
		return _operation_to_opcode.getOrDefault(opStr.toLowerCase(), OP_INVALID_CODE);
	}

	/**
	 * Transforma un opcode en la cadena correspondiente
	 */
	protected static String opcodeToOperation(byte opcode) {
		return _opcode_to_operation.getOrDefault(opcode, null);
	}

	// Devuelve el opcode del mensaje
	public byte getOpcode() {
		return opcode;
	}

	// Método que debe ser implementado específicamente por cada subclase de NCMessage
	protected abstract String toEncodedString();

	// Extrae la operación del mensaje entrante y usa la subclase para parsear el resto del mensaje
	public static NCMessage readMessageFromSocket(DataInputStream dis) throws IOException {
		String message = dis.readUTF();
		String[] lines = message.split(String.valueOf(END_LINE));
		if (!lines[0].isEmpty()) { // Si la línea no está vacía
			int idx = lines[0].indexOf(DELIMITER); // Posición del delimitador
			String field = lines[0].substring(0, idx).toLowerCase(); 																		// minúsculas
			String value = lines[0].substring(idx + 1).trim();
			if (!field.equalsIgnoreCase(OPCODE_FIELD))
				return null;
			byte code = operationToOpcode(value);
			if (code == OP_INVALID_CODE)
				return null;
			switch (code) {
			case OP_NICK: { return NCQueryMessage.readFromString(code, message); }
			case OP_NICK_OK: { return NCOneFieldMessage.readFromString(code); /*un campo*/ }
			case OP_NICK_DUPLICATED: { return NCOneFieldMessage.readFromString(code); /*un campo*/ }
			case OP_GET_ROOMLIST: { return NCOneFieldMessage.readFromString(code); }
			case OP_SEND_ROOMLIST: { return NCSendRoomListMessage.readFromString(code,message); }
			case OP_GET_INFO_ROOM: { return NCOneFieldMessage.readFromString(code); /*un campo*/}
			case OP_ENTER_ROOM: { return NCQueryMessage.readFromString(code,message); }
			case OP_ENTER_OK: { return NCOneFieldMessage.readFromString(code); /*un campo*/}
			case OP_ENTER_DENIED: { return NCOneFieldMessage.readFromString(code); /*un campo*/}
			case OP_SEND: { return NCSendMessage.readFromString(code, message); }
			case OP_EXIT: { return NCOneFieldMessage.readFromString(code); /*un campo*/}
			case OP_CREATE: { return NCOneFieldMessage.readFromString(code); /*un campo*/}
			case OP_PRIVATE: { return NCPrivateMessage.readFromString(code, message); }
			case OP_RENAME: { return NCQueryMessage.readFromString(code, message); }
			case OP_EXIT_OR_ENTER: { return NCQueryMessage.readFromString(code, message); }
			case OP_NOT_EXISTS: { return NCOneFieldMessage.readFromString(code); }
			case OP_SAME_USER: { return NCOneFieldMessage.readFromString(code); }
			case OP_CREATE_FAIL: { return NCOneFieldMessage.readFromString(code); }

			
			default:
				System.err.println("Unknown message type received:" + code);
				return null;
			}
		} else
			return null;
	}

	public static NCMessage makeNCOneFieldMessage(byte code) {
		return new NCOneFieldMessage(code);
	}
	
	public static NCMessage makeNCQueryMessage(byte code, String room) {
		return new NCQueryMessage(code, room);
	}

	public static NCMessage makeNCSendRoomListMessage(byte code, List<NCRoomDescription> rooms) {
		return new NCSendRoomListMessage(code, rooms);
	}
	
	public static NCMessage makeNCSendMessage (byte code, String mensaje, String usuario) {
		return new NCSendMessage(code, mensaje, usuario);
	}
	
	public static NCMessage makeNCPrivateMessage (byte code, String mensaje, String emisor, String receptor) {
		return new NCPrivateMessage(code, mensaje, emisor,receptor);
	}

}
