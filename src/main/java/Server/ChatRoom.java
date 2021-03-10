package Server;

import Tools.RMQTools;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static Tools.SerializationTools.myStringParser;
import static Tools.RMQTools.DebugPrint;

public class ChatRoom implements Serializable {
    String name;
    ChatLog _chatlog;

    final String QUEUE_HUB_ROOM = "QUEUE_HUB_ROOM"; //Room emet au hub ici
    String QUEUE_ROOM_LOGS_OUT; //Room emet ici
    String QUEUE_ROOM_USERS_OUT; //Room emet ici
    String QUEUE_ROOM_LOGS_IN; //Room ecoute ici
    String QUEUE_ROOM_USERS_IN; //Room ecoute ici

    protected transient Channel channel;

    transient ArrayList<String> users = new ArrayList<>();
    transient ArrayList<Timer> timers = new ArrayList<>();

    ChatRoom(String name){
        this.name = name;
        QUEUE_ROOM_USERS_IN = name + "_USERS_IN";
        QUEUE_ROOM_LOGS_IN = name + "_LOGS_IN";
        QUEUE_ROOM_USERS_OUT = name + "_USERS_OUT";
        QUEUE_ROOM_LOGS_OUT = name + "_LOGS_OUT";
        _chatlog = new ChatLog(100);
    }

    public boolean Init(){
        channel = RMQTools.channelCreatorLocal();
        if (channel == null) {
            System.out.println("Impossible de se connecter au serveur ");
            return false;
        }

        RMQTools.addExchange(channel, QUEUE_ROOM_USERS_OUT);
        RMQTools.addExchange(channel, QUEUE_ROOM_LOGS_OUT);
        RMQTools.addQueue(channel, QUEUE_ROOM_USERS_IN);
        RMQTools.addQueue(channel, QUEUE_ROOM_LOGS_IN);


        // Set up a timer for Hub Notification
        RMQTools.addQueue(channel, QUEUE_HUB_ROOM);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                NotifyHubAlive();
            }
        }, 2000, 10000);

        return true;
    }

    public void WaitForUsers(){
        while (true) {

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                if(message.startsWith("+")){
                    DebugPrint("Received new  user :"+message.substring(1));
                    Register_User(message.substring(1));
                }else if (message.startsWith("-")){
                    DebugPrint("Received end  user :"+message.substring(1));
                    Unregister_User(message.substring(1));
                }
            };
            try {
                channel.basicConsume(QUEUE_ROOM_USERS_IN, true, deliverCallback, consumerTag -> {
                });
            }catch (Exception e){
                System.out.println("Erreur de consommation : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    public void WaitForMessages(){
        while (true) {
            byte[] message = RMQTools.receiveMessageWaited(channel, QUEUE_ROOM_LOGS_IN);
            if(message!= null) {
                String[] m = new String(message).split("<-NAME-SEPARATOR->");
                Say(m[0], m[1]);
            }
            System.out.println("Say");
        }
    }

    public void Say(String name, String s) {
        _chatlog.Add_log(name,s);
        RMQTools.sendMessage(channel,QUEUE_ROOM_LOGS_OUT,_chatlog.Get_Logs());
    }

    public void Register_User(String name){
        Timer timer = new Timer(name);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                AutoUnregister_User(name);
            }
        }, 20000);

        if(users.contains(name)){
            timers.set(users.indexOf(name),timer);
        }
        else{
            users.add(name);
            timers.add(timer);
        }

        RMQTools.sendMessageExchanged(channel,QUEUE_ROOM_USERS_OUT, myStringParser(users.toArray(new String[0])));
    }

    public void Unregister_User(String name){
        int i = users.indexOf(name);
        if(i == -1){
            System.out.println("Skipping failed unregister for " + name);
            return;
        }
        timers.get(i).cancel();
        timers.remove(i);
        users.remove(name);

        RMQTools.sendMessage(channel,QUEUE_ROOM_USERS_OUT, myStringParser(users.toArray(new String[0])));
    }

    private TimerTask AutoUnregister_User(String name){
        Unregister_User(name);
        return null;
    }

    private void NotifyHubAlive(){
        RMQTools.sendMessage(channel,QUEUE_HUB_ROOM,name);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        users = new ArrayList<>();
        timers = new ArrayList<>();
    }

}