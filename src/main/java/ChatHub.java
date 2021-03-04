import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.io.Serializable;
import java.rmi.AlreadyBoundException;
import java.rmi.NotBoundException;
import java.util.ArrayList;


public class ChatHub implements  Serializable {
    final static String QUEUE_HUB_SERVER = "QUEUE_HUB_SERVER";
    final static String QUEUE_HUB_CLIENT = "QUEUE_HUB_CLIENT";
    protected transient Channel channel;

    final ArrayList<String> namelist = new ArrayList<>();
    final ArrayList<ChatRoom> chatlist = new ArrayList<>();


    ChatHub(){
        Init_Hub();
    }
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        Init_Hub();

    }

    private boolean Init_Hub(){
        channel = RMQTools.channelCreatorLocal();
        if (channel == null) {
            System.out.println("Impossible de se connecter au serveur ");
            return false;
        }
        return true;
    }


    public  int GetChatRoomNumber() {
        return chatlist.size();
    }


    public String[] PublishChatRoomList() {



        return namelist.toArray(new String[0]);
    }


    public  String GetChatRoomURI(String name) throws NotBoundException {
        if(!namelist.contains(name))
            throw new NotBoundException("There is no room with this name : " + name);
        return "Room_"+name+"_Service";
    }


    public String NewChatRoom(String name) throws AlreadyBoundException, NotBoundException {
        if(namelist.contains(name))
            throw new AlreadyBoundException("A room already exists with name : " + name);

        ChatRoom newchat = new ChatRoom(name);
        chatlist.add(newchat);
        namelist.add(name);

        return GetChatRoomURI(name);
    }


    public void RemoveChatRoom(String name) {
        chatlist.remove(namelist.indexOf(name));
        namelist.remove(name);
    }



}