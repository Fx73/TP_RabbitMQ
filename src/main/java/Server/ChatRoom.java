package Server;

import Tools.RMQTools;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static Tools.SerializationTools.myStringParser;
import static Tools.RMQTools.DebugPrint;

public class ChatRoom implements Serializable {
    String name;
    ChatLog chatlog;

    final String QUEUE_HUB_ROOM = "QUEUE_HUB_ROOM"; //Room emet au hub ici
    String EXCHANGE_ROOM_LOGS_OUT; //Room emet ici
    String EXCHANGE_ROOM_USERS_OUT; //Room emet ici
    String QUEUE_ROOM_LOGS_IN; //Room ecoute ici
    String QUEUE_ROOM_USERS_IN; //Room ecoute ici

    protected transient Channel channel;

    transient ArrayList<String> users = new ArrayList<>();
    transient ArrayList<Timer> timers = new ArrayList<>();

    ChatRoom(String name){
        this.name = name;
        QUEUE_ROOM_USERS_IN = name + "_USERS_IN";
        QUEUE_ROOM_LOGS_IN = name + "_LOGS_IN";
        EXCHANGE_ROOM_USERS_OUT = name + "_USERS_OUT";
        EXCHANGE_ROOM_LOGS_OUT = name + "_LOGS_OUT";
        chatlog = new ChatLog(100);
    }

    public boolean Init(){
        channel = RMQTools.channelCreatorLocal();
        if (channel == null) {
            System.out.println("Impossible de se connecter au serveur ");
            return false;
        }

        RMQTools.addExchange(channel, EXCHANGE_ROOM_USERS_OUT);
        RMQTools.addExchange(channel, EXCHANGE_ROOM_LOGS_OUT);
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

    /**
     * Attend la connexion/deconexion des users sur cette room
     */
    public void WaitForUsers(){
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                if(message.startsWith("+")){
                    DebugPrint("Received user :"+message.substring(1));
                    Register_User(message.substring(1));
                }else if (message.startsWith("-")){
                    DebugPrint("Received user end:"+message.substring(1));
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

    /**
     * Attend les messages des users de la room
     */
    public void WaitForMessages(){
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            try{
                String[] m = message.split("<-NAME-SEPARATOR->");
                DebugPrint("Message recu de " + m[0] + " ("+m[1].length()+" chars)");
                Say(m[0], m[1]);
            }catch (Exception e){
            System.out.println("Message recu invalide");
            }
        };
        try {
            channel.basicConsume(QUEUE_ROOM_LOGS_IN, true, deliverCallback, consumerTag -> {
            });
        }catch (Exception e){
            System.out.println("Erreur de consommation : " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void Say(String name, String s) {
        chatlog.Add_log(name,s);
        System.out.println(chatlog.Get_Logs());
        RMQTools.sendMessageExchanged(channel, EXCHANGE_ROOM_LOGS_OUT, chatlog.Get_Logs());
    }

    /**
     * Enregistre un utilisateur dans la room
     * @param Nom de l'user
     */
    public void Register_User(String name){
        Timer timer = new Timer(name);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                AutoUnregister_User(name);
            }
        }, 30000);

        if(users.contains(name)){
            timers.set(users.indexOf(name),timer);
        }
        else{
            users.add(name);
            timers.add(timer);
        }

        RMQTools.sendMessageExchanged(channel, EXCHANGE_ROOM_USERS_OUT, myStringParser(users.toArray(new String[0])));
    }

    /**
     * Deconnecte un utilisateur de la room
     * @param Nom de l'utilisateur
     */
    public void Unregister_User(String name){
        int i = users.indexOf(name);
        if(i == -1){
            DebugPrint("Skipping failed unregister for " + name);
            DebugPrint(Thread.currentThread().getStackTrace());
            return;
        }
        timers.get(i).cancel();
        timers.remove(i);
        users.remove(name);

        RMQTools.sendMessage(channel, EXCHANGE_ROOM_USERS_OUT, myStringParser(users.toArray(new String[0])));
    }

    /**
     * Enlève l'utilisateur de la liste des users connecté
     * @param utilisateur à deconnecter
     * @return
     */
    private TimerTask AutoUnregister_User(String name){
        Unregister_User(name);
        return null;
    }

    /**
     * Previens le hub que la room est toujours fonctionel
     */
    private void NotifyHubAlive(){
        RMQTools.sendMessage(channel,QUEUE_HUB_ROOM,name);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        users = new ArrayList<>();
        timers = new ArrayList<>();
    }

}