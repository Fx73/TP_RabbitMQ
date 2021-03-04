import com.rabbitmq.client.Channel;
import org.apache.commons.lang3.SerializationUtils;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.util.ArrayList;


public class ChatHub implements  Serializable {
    final static String QUEUE_HUB_SERVER = "QUEUE_HUB_SERVER"; //Server emet ici
    final static String QUEUE_HUB_CLIENT = "QUEUE_HUB_CLIENT"; //Server ecoute ici
    protected transient Channel channel;

    final ArrayList<String> namelist = new ArrayList<>();
    final ArrayList<ChatRoom> chatlist = new ArrayList<>();


    public boolean Init_Hub(){
        channel = RMQTools.channelCreatorLocal();
        if (channel == null) {
            System.out.println("Impossible de se connecter au serveur ");
            return false;
        }

        RMQTools.addExchange(channel,QUEUE_HUB_SERVER);
        RMQTools.addQueue(channel,QUEUE_HUB_CLIENT);

        return true;
    }


    public void PublishChatRoomList() {
        try {
            channel.basicPublish("", QUEUE_HUB_SERVER, null, SerializationUtils.serialize( namelist));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public  void PublishRoomURI(String name) throws NotBoundException {
        if(!namelist.contains(name))
            throw new NotBoundException("There is no room with this name : " + name);
        try {
            channel.basicPublish("", QUEUE_HUB_SERVER, null,("Room_"+name+"_Service").getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void NewChatRoom(String name) throws AlreadyBoundException, NotBoundException {
        if(namelist.contains(name))
            throw new AlreadyBoundException("A room already exists with name : " + name);

        ChatRoom newchat = new ChatRoom(name);
        chatlist.add(newchat);
        namelist.add(name);

        PublishRoomURI(name);
    }


    public void RemoveChatRoom(String name) {
        chatlist.remove(namelist.indexOf(name));
        namelist.remove(name);
    }



}