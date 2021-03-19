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


      /*
        Les fonctions suivantes ont été faites pour une version plus scalable,
        ou les queues des rooms n'avait pas un nom fixe, et ou le serveur envoyait le RoomId au client

     */

    public static RoomId findByName(Collection<RoomId> listCarnet, String nameIsIn) {
        return listCarnet.stream().filter(carnet -> nameIsIn.equals(carnet.name)).findFirst().orElse(null);
    }

    public String RoomIdToString(){
        String[] r = {name, EXCHANGE_ROOM_LOGS_OUT, EXCHANGE_ROOM_USERS_OUT, QUEUE_ROOM_LOGS_IN, QUEUE_ROOM_USERS_IN};
        return SerializationTools.myStringParser(r);
    }

    public RoomId RoomIdFromString(String rparsed){
        String[] r = SerializationTools.myStringUnparser(rparsed);
        name = r[0];
        EXCHANGE_ROOM_LOGS_OUT = r[1];
        EXCHANGE_ROOM_USERS_OUT = r[2];
        QUEUE_ROOM_LOGS_IN = r[3];
        QUEUE_ROOM_USERS_IN = r[4];
        return this;
    }
}
