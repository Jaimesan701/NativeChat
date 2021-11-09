package es.um.redes.nanoChat.messageFV;

public class NCPrivateMessage extends NCMessage{
	
	// Campo específico de este tipo de mensaje
		static protected final String SEND_FIELD = "send";
		static protected final String SENDER_FIELD = "sender";
		static protected final String RECEIVER_FIELD = "receiver";
		// ATRIBUTO
		private String mensaje;
		private String emisor;
		private String receiver;
		// CONSTRUCTOR
		public NCPrivateMessage(byte type, String mensaje, String emisor, String receiver) {
			this.opcode = type;
			this.mensaje = mensaje;
			this.emisor = emisor;
			this.receiver = receiver;
		}
		
		// Codifica el mensaje para ser enviado
		@Override
		public String toEncodedString() {
			StringBuffer sb = new StringBuffer();			
			sb.append(OPCODE_FIELD+DELIMITER+opcodeToOperation(opcode)+END_LINE); //Construimos el campo
			sb.append(SEND_FIELD+DELIMITER+mensaje+END_LINE); //Construimos el campo 
			sb.append(SENDER_FIELD+DELIMITER+emisor+END_LINE); //Construimos el campo 
			sb.append(RECEIVER_FIELD+DELIMITER+receiver+END_LINE); //Construimos el campo 
			sb.append(END_LINE);  //Marcamos el final del mensaje
			return sb.toString(); //Se obtiene el mensaje
		}
		
		// Decodifica el mensaje para obetener sus campos

		public static NCPrivateMessage readFromString(byte code, String message) {
			String[] lines = message.split(String.valueOf(END_LINE));
			String mensaje = null;
			String emisor = null;
			String receptor = null;
			for(int i = 1; i < lines.length; i++) {
				int idx = lines[i].indexOf(DELIMITER); // Posición del delimitador
				String field = lines[i].substring(0, idx).toLowerCase();                                                                                                                                                // minúsculas
				String value = lines[i].substring(idx + 1).trim();
				if (field.equalsIgnoreCase(SEND_FIELD))
					mensaje = value;
				else if (field.equalsIgnoreCase(SENDER_FIELD))
					emisor = value;
				else
					receptor = value;
					
			}  
			return new NCPrivateMessage(code, mensaje,emisor,receptor);
		}

		// Getter del campo name
		public String getMensaje() {
			return mensaje;
		}
		
		public String getEmisor() {
			return emisor;
		}
		
		public String getReceptor() {
			return receiver;
		}

}
