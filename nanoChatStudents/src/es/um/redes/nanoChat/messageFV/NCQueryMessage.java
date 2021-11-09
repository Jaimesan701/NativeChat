package es.um.redes.nanoChat.messageFV;

public class NCQueryMessage extends NCMessage {

	// Campo específico de este tipo de mensaje
		static protected final String QUERY_FIELD = "query";

	
		private String query;

	
		public NCQueryMessage(byte type, String query) {
			this.opcode = type;
			this.query = query;
		}
		//	Codifica el mensaje para ser enviado
		@Override
		public String toEncodedString() {
			StringBuffer sb = new StringBuffer();			
			sb.append(OPCODE_FIELD+DELIMITER+opcodeToOperation(opcode)+END_LINE); //Construimos el campo
			sb.append(QUERY_FIELD+DELIMITER+query+END_LINE); //Construimos el campo
			sb.append(END_LINE);  //Marcamos el final del mensaje
			return sb.toString(); //Se obtiene el mensaje
		}

		// Decodifica el mensaje para obetener sus campos
		public static NCQueryMessage readFromString(byte code, String message) {
			String[] lines = message.split(String.valueOf(END_LINE));
			String name = null;
			int idx = lines[1].indexOf(DELIMITER); // Posición del delimitador
			String field = lines[1].substring(0, idx).toLowerCase();                                                                                                                                                // minúsculas
			String value = lines[1].substring(idx + 1).trim();
			if (field.equalsIgnoreCase(QUERY_FIELD))
				name = value;
			return new NCQueryMessage(code, name);
		}

		public String getName() {
			return query;
		}
		
	
	

}
