package Server;

import Tools.SerializationTools;

import java.io.Serializable;
import java.util.Collection;

public class RoomId implements Serializable {
    public String name;
    public String EXCHANGE_ROOM_LOGS_OUT; //Server emet ici
    public String QUEUE_ROOM_LOGS_OUT = null; //Client ecoute ici
    public String EXCHANGE_ROOM_USERS_OUT; //Server emet ici
    public String QUEUE_ROOM_USERS_OUT = null; //Client ecoute ici
    public String QUEUE_ROOM_LOGS_IN; //Client emet ici
    public String QUEUE_ROOM_USERS_IN; //Client emet ici

    public RoomId(String name) {
        this.name = name;
        QUEUE_ROOM_USERS_IN = name + "_USERS_IN";
        QUEUE_ROOM_LOGS_IN = name + "_LOGS_IN";
        EXCHANGE_ROOM_USERS_OUT = name + "_USERS_OUT";
        EXCHANGE_ROOM_LOGS_OUT = name + "_LOGS_OUT";
    }
    
}
