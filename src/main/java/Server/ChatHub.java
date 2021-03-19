package Server;

import Tools.RMQTools;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static Tools.SerializationTools.myStringParser;
import static Tools.RMQTools.DebugPrint;

public class ChatHub {
    final String QUEUE_HUB_SERVER = "QUEUE_HUB_SERVER"; //Server emet ici
    final String QUEUE_HUB_CLIENT = "QUEUE_HUB_CLIENT"; //Server ecoute les clients ici
    final String QUEUE_HUB_ROOM = "QUEUE_HUB_ROOM"; //Server emet aux rooms ici
    final String QUEUE_ROOM_HUB = "QUEUE_ROOM_HUB"; //Server ecoute les rooms ici

    protected Channel channel;

    final ArrayList<String> namelist = new ArrayList<>();
    transient ArrayList<Timer> timers = new ArrayList<>();


    public boolean Init(){
        channel = RMQTools.channelCreatorLocal();
        if (channel == null) {
            System.out.println("Impossible de se connecter au serveur ");
            return false;
        }

        RMQTools.addExchange(channel,QUEUE_HUB_SERVER);
        RMQTools.addQueue(channel,QUEUE_HUB_CLIENT);

        // Set up a timer for Rooms Notification
        RMQTools.addExchange(channel,QUEUE_HUB_ROOM);
        RMQTools.addQueue(channel,QUEUE_ROOM_HUB);
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                NotifyRoomsAlive();
            }
        }, 2000, 20000);

        return true;
    }


    /**
     * Ecoute les clients
     * UPDATE : Liste des rooms
     * CREATE : Créer une nouvelle salle
     * DELETE : Supprimer une salle
     */
    public void WaitForMessages(){
        System.out.println("Waiting for clients ...");

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                if(message.equals("UPDATE")){
                    DebugPrint("Received Update Demand");
                    PublishChatRoomList();
                }else if(message.startsWith("CREATE ")) {
                    NewChatRoom(message.substring("CREATE ".length()));
                }else if (message.startsWith("DELETE ")){
                    RemoveChatRoom(message.substring("DELETE ".length()));
                }
            };
            try {
                channel.basicConsume(QUEUE_HUB_CLIENT, true, deliverCallback, consumerTag -> {
                });
            }catch (Exception e){
                System.out.println("Erreur de consommation : " + e.getMessage());
                e.printStackTrace();
            }

    }
    
    
/**
 * Le hub detecte si une room n'est pas opérationel alors qu'elle devrait 
 * 
 */
    public void WaitForRooms(){
        System.out.println("Waiting for rooms notifications ...");
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
                DebugPrint("Received notif from room " + message);
                Timer timer = new Timer(message);
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        Relaunch_Room(message);
                    }
                }, 20000);
                if(namelist.contains(message)) {
                    timers.get(namelist.indexOf(message)).cancel();
                    timers.set(namelist.indexOf(message), timer);
                }
                else{
                    namelist.add(message);
                    timers.add(timer);

                    PublishChatRoomList();
                }
            };
            try {
                channel.basicConsume(QUEUE_ROOM_HUB, true, deliverCallback, consumerTag -> {
                });
            }catch (Exception e){
                System.out.println("Erreur de consommation : " + e.getMessage());
                e.printStackTrace();
            }
    }

    /**
     * Envois la liste des room aux clients
     */
    public void PublishChatRoomList() {
         RMQTools.sendMessageExchanged(channel,QUEUE_HUB_SERVER, myStringParser(namelist.toArray(new String[0])));
    }



/**
 * Créer une chatroom
 */
    public void NewChatRoom(String name) {
        if(namelist.contains(name)){
            System.out.println("A room already exists with name : " + name);
            return;
        }

        String cmd ="java -Dfile.encoding=UTF-8 -classpath target\\classes;lib\\amqp-client-5.11.0.jar;lib\\slf4j-api-1.7.30.jar Server.RoomLauncher " + name;
        try {
            ProcessBuilder builder = new ProcessBuilder();
            builder.directory(new File(System.getProperty("user.dir")));
            if (System.getProperty("os.name").toLowerCase().startsWith("windows")) {
                builder.command("cmd.exe", "/c", cmd);
            } else {
                builder.command("sh", "-c", cmd);
            }
            builder.start();
        } catch (IOException e){
            System.out.println("Echec d'instanciation de la room");
        }


        Timer timer = new Timer(name);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                Relaunch_Room(name);
            }
        }, 20000);
        namelist.add(name);

        timers.add(timer);

        PublishChatRoomList();
    }

/**
 * Supprime une room
 * @param name Nom de la room
 */
    public void RemoveChatRoom(String name) {
        RMQTools.sendMessageExchanged(channel,QUEUE_HUB_ROOM,"SHUTDOWN "+name );

        timers.get(namelist.indexOf(name)).cancel();
        timers.remove(namelist.indexOf(name));
        namelist.remove(name);

        PublishChatRoomList();
    }

    /**
     * Relance une room
     * @param name Nom de la room
     */
    void Relaunch_Room(String name){
        timers.remove(namelist.indexOf(name));
        namelist.remove(name);
        NewChatRoom(name);
    }

    /**
     * Previent les rooms qu'il est toujours fonctionel
     */
    private void NotifyRoomsAlive(){
        RMQTools.sendMessageExchanged(channel,QUEUE_HUB_ROOM,"ALIVE");
    }


}