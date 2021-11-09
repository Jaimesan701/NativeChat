package es.um.redes.nanoChat.messageFV;

import es.um.redes.nanoChat.server.roomManager.NCRoomDescription;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

public class NCSendRoomListMessage extends NCMessage{

    // Campo específico de este tipo de mensaje
    static protected final String ROOMS_FIELD = "rooms";
    static final String DELIMITER_ROOM_FIELD = "&";
    static final String DELIMITER_INNER = "%";
    static final String DELIMITER_USER = ";";

    // ATRIBUTO
    private List<NCRoomDescription> rooms;

    // CONSTRUCTOR
    public NCSendRoomListMessage(byte type, List<NCRoomDescription> rooms) {
        this.opcode = type;
        this.rooms = rooms;
    }

    // Codifica el mensaje para obetener sus campos
    @Override
    public String toEncodedString() {
        StringBuffer sb = new StringBuffer();
        sb.append(OPCODE_FIELD+DELIMITER+opcodeToOperation(opcode)+END_LINE);
        sb.append(ROOMS_FIELD+DELIMITER);
        for(NCRoomDescription room : rooms){
            sb.append(room.roomName+DELIMITER_INNER);
           
            for(String user : room.members){
                sb.append(user+DELIMITER_USER);
            }
            sb.append(DELIMITER_INNER + room.timeLastMessage + DELIMITER_ROOM_FIELD);
        }
        sb.append(END_LINE);
        sb.append(END_LINE);  // Marcamos el final del mensaje
        return sb.toString(); // Se obtiene el mensaje
    }



    // Decodifica el mensaje
    public static NCSendRoomListMessage readFromString(byte code, String message) {
        String[] lines = message.split(String.valueOf(END_LINE));
        String[] rooms = null;
        LinkedList<NCRoomDescription> roomsList = new LinkedList<>();
        int idx = lines[1].indexOf(DELIMITER); // Posición del delimitador
        String field = lines[1].substring(0, idx).toLowerCase();                                                                                                                                                // minúsculas
        String value = lines[1].substring(idx + 1).trim();
        if (field.equalsIgnoreCase(ROOMS_FIELD)) {
            rooms = value.split(DELIMITER_ROOM_FIELD);
            for(String s : rooms){
                String[] roomElements = s.split(DELIMITER_INNER);
                LinkedList<String> users = new LinkedList<String>();
                String[] array = roomElements[1].split(DELIMITER_USER);
                Collections.addAll(users, array); 
                if(array[0].equals(""))
                	roomsList.add(new NCRoomDescription(roomElements[0], new LinkedList<>(), Long.parseLong(roomElements[2])));
                else
                	roomsList.add(new NCRoomDescription(roomElements[0], users, Long.parseLong(roomElements[2])));
            }
            return new NCSendRoomListMessage(code, roomsList);
        }
        return null;
    }


    public List<NCRoomDescription> getRooms() {
        return Collections.unmodifiableList(rooms);
    }
}
