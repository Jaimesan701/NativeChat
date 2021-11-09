package es.um.redes.nanoChat.messageFV;

public class NCSendMessage extends NCMessage {
	// Campo específico de este tipo de mensaje
	static protected final String SEND_FIELD = "send";
	static protected final String SENDER_FIELD = "sender";
	// ATRIBUTO
	private String mensaje;
	private String emisor;
	// CONSTRUCTOR
	public NCSendMessage(byte type, String mensaje, String emisor) {
		this.opcode = type;
		this.mensaje = mensaje;
		this.emisor = emisor;
	}
	
	// Codifica el mensaje para ser enviado
	@Override
	public String toEncodedString() {
		StringBuffer sb = new StringBuffer();			
		sb.append(OPCODE_FIELD+DELIMITER+opcodeToOperation(opcode)+END_LINE); //Construimos el campo
		sb.append(SEND_FIELD+DELIMITER+mensaje+END_LINE); //Construimos el campo 
		sb.append(SENDER_FIELD+DELIMITER+emisor+END_LINE); //Construimos el campo 
		sb.append(END_LINE);  //Marcamos el final del mensaje
		return sb.toString(); //Se obtiene el mensaje
	}
	

	// decodifica el mensaje para obtener sus campos
	public static NCSendMessage readFromString(byte code, String message) {
		String[] lines = message.split(String.valueOf(END_LINE));
		String mensaje = null;
		String emisor = null;
		for(int i = 1; i < lines.length; i++) {
			int idx = lines[i].indexOf(DELIMITER); // Posición del delimitador
			String field = lines[i].substring(0, idx).toLowerCase();                                                                                                                                                // minúsculas
			String value = lines[i].substring(idx + 1).trim();
			if (field.equalsIgnoreCase(SEND_FIELD))
				mensaje = value;
			else 
				emisor = value;
		}  
		return new NCSendMessage(code, mensaje,emisor);
	}

	// Getter del campo name
	public String getMensaje() {
		return mensaje;
	}
	
	public String getEmisor() {
		return emisor;
	}
}
