package es.um.redes.nanoChat.messageFV;

public class NCOneFieldMessage extends NCMessage {
	
	public NCOneFieldMessage(byte type) {
		this.opcode = type;
	}

	// Decodifica el mensaje para obetener sus campos
	public static NCOneFieldMessage readFromString(byte code) {
		return new NCOneFieldMessage(code);
	}

	// Codifica el mensaje para ser enviado
	@Override
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();			
		sb.append(OPCODE_FIELD+DELIMITER+opcodeToOperation(opcode)+END_LINE); //Construimos el campo
		sb.append(END_LINE);  //Marcamos el final del mensaje
		return sb.toString(); //Se obtiene el mensaje
	}

}
