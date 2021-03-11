package Server;

import Tools.RMQTools;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.rmi.NotBoundException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import static Tools.SerializationTools.myStringParser;
import static Tools.RMQTools.DebugPrint;

public class ChatHub {
    final String QUEUE_HUB_SERVER = "QUEUE_HUB_SERVER"; //Server emet ici
    final String QUEUE_HUB_CLIENT = "QUEUE_HUB_CLIENT"; //Server ecoute les clients ici
    final String QUEUE_HUB_ROOM = "QUEUE_HUB_ROOM"; //Server ecoute les rooms ici

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

        return true;
    }


    /**
     * Ecoute les clients
     * Update : Liste des rooms
     * Create : Créer une nouvelle salle
     * DELETE : Supprimer une salle
     */
    public void WaitForMessages(){
        System.out.println("Waiting for clients ...");

        while (true) {

            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String message = new String(delivery.getBody(), "UTF-8");
                if(message.equals("UPDATE")){
                    DebugPrint("Received Update Demand");
                    PublishChatRoomList();
                }else if(message.startsWith("CREATE ")) {
                    //TODO:traiter le message
                }else if (message.startsWith("DELETE ")){
                    //TODO:traiter le message
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
    }
    
    
/**
 * Le hub detecte si une room n'est pas opérationel alors qu'elle devrait 
 * 
 */
    public void WaitForRooms(){
        System.out.println("Waiting for rooms notifications ...");
        while (true) {
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
                channel.basicConsume(QUEUE_HUB_ROOM, true, deliverCallback, consumerTag -> {
                });
            }catch (Exception e){
                System.out.println("Erreur de consommation : " + e.getMessage());
                e.printStackTrace();
            }
        }
    }

    /**
     * Envois la liste des room aux clients
     */
    public void PublishChatRoomList() {
         RMQTools.sendMessageExchanged(channel,QUEUE_HUB_SERVER, myStringParser(namelist.toArray(new String[0])));
    }

/**
 * Envois l'adresse d'une room
 */
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

/**
 * Créer une chatroom
 */
    public void NewChatRoom(String name) {
        if(namelist.contains(name)){
            System.out.println("A room already exists with name : " + name);
            return;
        }

        //Todo: Lancer un nouveau programme


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
 * @param Nom de la room
 */
    public void RemoveChatRoom(String name) {
        //Todo: Arreter un programme
        namelist.remove(name);
    }

    /**
     * Relance une room
     * @param nom de la roomù
     * @return
     */
    TimerTask Relaunch_Room(String name){
        timers.remove(namelist.indexOf(name));
        namelist.remove(name);
        NewChatRoom(name);
        return null;
    }

}