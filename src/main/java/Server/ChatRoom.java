package Server;

import Tools.RMQTools;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static Tools.SerializationTools.SEPARATOR;
import static Tools.SerializationTools.myStringParser;
import static Tools.RMQTools.DebugPrint;

public class ChatRoom implements Serializable {
    String owner;
    RoomId roomId;
    ChatLog chatlog;

    final String QUEUE_ROOM_HUB = "QUEUE_ROOM_HUB"; //Room emet au hub ici
    final String EXCHANGE_HUB_ROOM = "QUEUE_HUB_ROOM"; //Room ecoute le hub ici

    String QUEUE_HUB_ROOM;


    protected transient Channel channel;

    transient ArrayList<String> users = new ArrayList<>();
    transient ArrayList<Timer> timers = new ArrayList<>();

    ChatRoom(String name, String user){
        owner = user;
        roomId = new RoomId(name);
        chatlog = new ChatLog(100);
    }

    public boolean Init(){
        channel = RMQTools.channelCreatorLocal();
        if (channel == null) {
            System.out.println("Impossible de se connecter au serveur ");
            return false;
        }

        RMQTools.addExchange(channel, roomId.EXCHANGE_ROOM_USERS_OUT);
        RMQTools.addExchange(channel, roomId.EXCHANGE_ROOM_LOGS_OUT);
        RMQTools.addQueue(channel, roomId.QUEUE_ROOM_USERS_IN);
        RMQTools.addQueue(channel, roomId.QUEUE_ROOM_LOGS_IN);


        // Set up a timer for Hub Notification
        QUEUE_HUB_ROOM = RMQTools.addQueueBound(channel,EXCHANGE_HUB_ROOM);
        RMQTools.addQueue(channel, QUEUE_ROOM_HUB);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                NotifyHubAlive();
            }
        }, 2000, 10000);

        Say("SYSTEM", "- Salle créée par "+ owner +" -");
        return true;
    }

    /**
     * Attend la connexion/deconexion des users sur cette room
     */
    public void WaitForHub() {
        System.out.println("Waiting for hub notifications ...");
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            DebugPrint("Received notif from hub : " + message);

            if(message.startsWith("ALIVE")){
                //TODO: Mettre un timer pour relancer le hub ?
            }else if (message.startsWith("SHUTDOWN")){
                String[] m = message.substring("SHUTDOWN ".length()).split(SEPARATOR);
                if(m[0].equals(roomId.name)){
                    Say("SYSTEM", m[1]+" a demandé la fermeture de la salle "+ roomId.name+" !\n A bientot !");
                    if(new File("roomsave_"+roomId.name+".txt").delete())
                        DebugPrint("Save deleted");
                    System.exit(0);
                }

            }


        };
        try {
            channel.basicConsume(QUEUE_HUB_ROOM, true, deliverCallback, consumerTag -> {
            });
        }catch (Exception e){
            System.out.println("Erreur de consommation : " + e.getMessage());
            e.printStackTrace();
        }
    }


    /**
     * Attend la connexion/deconexion des users sur cette room
     */
    public void WaitForUsers(){
        System.out.println("Waiting for users ...");
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                if(message.startsWith("+")){
                    DebugPrint("Received user :"+message.substring(1));
                    Register_User(message.substring(1));
                }else if (message.startsWith("-")){
                    DebugPrint("Received user end:"+message.substring(1));
                    Unregister_User(message.substring(1));
                }
            };
            try {
                channel.basicConsume(roomId.QUEUE_ROOM_USERS_IN, true, deliverCallback, consumerTag -> {
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
        System.out.println("Waiting for messages ...");
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            try{
                String[] m = message.split(SEPARATOR);
                DebugPrint("Message recu de " + m[0] + " ("+m[1].length()+" chars)");
                Say(m[0], m[1]);
            }catch (Exception e){
            System.out.println("Message recu invalide");
            }
        };
        try {
            channel.basicConsume(roomId.QUEUE_ROOM_LOGS_IN, true, deliverCallback, consumerTag -> {
            });
        }catch (Exception e){
            System.out.println("Erreur de consommation : " + e.getMessage());
            e.printStackTrace();
        }
    }


    public void Say(String name, String s) {
        chatlog.Add_log(name,s);
        System.out.println(chatlog.Get_Logs());
        PublishLogs();
    }

    public void PublishLogs(){
        RMQTools.sendMessageExchanged(channel, roomId.EXCHANGE_ROOM_LOGS_OUT, chatlog.Get_Logs());
    }

    /**
     * Enregistre un utilisateur dans la room
     * @param name Nom de l'user
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
            PublishLogs();
        }

        RMQTools.sendMessageExchanged(channel, roomId.EXCHANGE_ROOM_USERS_OUT, myStringParser(users.toArray(new String[0])));
    }

    /**
     * Deconnecte un utilisateur de la room
     * @param name Nom de l'utilisateur
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

        RMQTools.sendMessage(channel, roomId.EXCHANGE_ROOM_USERS_OUT, myStringParser(users.toArray(new String[0])));
    }

    /**
     * Enlève l'utilisateur de la liste des users connecté
     * @param name utilisateur à deconnecter
     */
    private void AutoUnregister_User(String name){
        Unregister_User(name);
    }

    /**
     * Previens le hub que la room est toujours fonctionel
     */
    private void NotifyHubAlive(){
        RMQTools.sendMessage(channel,QUEUE_ROOM_HUB,roomId.name);
    }

    @Serial
    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        users = new ArrayList<>();
        timers = new ArrayList<>();
    }

}