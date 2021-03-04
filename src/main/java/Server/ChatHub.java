package Server;

import Tools.RMQTools;
import com.rabbitmq.client.Channel;
import org.json.JSONStringer;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.util.ArrayList;

import static Tools.SerializationTools.myStringParser;


public class ChatHub implements  Serializable {
    final String QUEUE_HUB_SERVER = "QUEUE_HUB_SERVER"; //Server emet ici
    final String QUEUE_HUB_CLIENT = "QUEUE_HUB_CLIENT"; //Server ecoute ici
    protected transient Channel channel;

    final ArrayList<String> namelist = new ArrayList<>();

    public boolean Init(){
        channel = RMQTools.channelCreatorLocal();
        if (channel == null) {
            System.out.println("Impossible de se connecter au serveur ");
            return false;
        }

        RMQTools.addExchange(channel,QUEUE_HUB_SERVER);
        RMQTools.addQueue(channel,QUEUE_HUB_CLIENT);

        return true;
    }


    public void WaitForMessages(){
        while (true) {
            byte[] message = RMQTools.receiveMessage(channel, QUEUE_HUB_CLIENT);
            if(message != null){
                //TODO:traiter le message
            }
        }
    }


    public void PublishChatRoomList() {
        JSONStringer mystringer = new JSONStringer();
        mystringer.array();

        try {
            channel.basicPublish("", QUEUE_HUB_SERVER, null, myStringParser(namelist.toArray(new String[0])).getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void PublishRoomURI(String name, String private_queue) throws NotBoundException {
        if(!namelist.contains(name))
            throw new NotBoundException("There is no room with this name : " + name);
        try {
            RMQTools.addQueue(channel,private_queue);
            channel.basicPublish("", private_queue, null,("Room_"+name+"_Service").getBytes());
            RMQTools.deleteQueue(channel,private_queue);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void NewChatRoom(String name) throws AlreadyBoundException{
        if(namelist.contains(name))
            throw new AlreadyBoundException("A room already exists with name : " + name);
        //Todo: Lancer un nouveau programme

        namelist.add(name);
    }


    public void RemoveChatRoom(String name) {
        //Todo: Arreter un programme
        namelist.remove(name);
    }


}