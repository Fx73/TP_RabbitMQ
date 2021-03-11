package Client;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.Arrays;

import java.util.Collection;
import java.util.concurrent.TimeUnit;

import Tools.RMQTools;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import static Tools.RMQTools.DebugPrint;

public class ChatClient {
	protected static Channel channel;
	final static String EXCHANGE_HUB_SERVER = "QUEUE_HUB_SERVER"; //Le client se lie ici
	static String QUEUE_HUB_SERVER;					// pour pouvoir écouter ici
	final static String QUEUE_HUB_CLIENT = "QUEUE_HUB_CLIENT"; //Le client emet ici

	static RoomId c_room = null;

	static ArrayList<RoomId> rooms = new ArrayList<>();

	public static void main(String[] args) throws InterruptedException {
		String host;
		if (args.length < 1)
			host = "localhost";
		else
			host = args[0];

		SwingUtilities.invokeLater(() -> Frame.setWindow(host).setVisible(true));

		channel = RMQTools.channelCreatorLocal();
		RMQTools.addQueue(channel,QUEUE_HUB_CLIENT);
		QUEUE_HUB_SERVER = RMQTools.addQueueBound(channel,EXCHANGE_HUB_SERVER);

		new Thread(ChatClient::UpdateRooms).start();


		RMQTools.sendMessage(channel,QUEUE_HUB_CLIENT,"UPDATE");


		//Debug
		RoomId r = new RoomId("A");
		r.QUEUE_ROOM_LOGS_IN="A_LOGS_IN";
		r.EXCHANGE_ROOM_LOGS_OUT="A_LOGS_OUT";
		r.QUEUE_ROOM_USERS_IN = "A_USERS_IN";
		r.EXCHANGE_ROOM_USERS_OUT = "A_USERS_OUT";
		rooms.add(r);
	}

/**
 * Ecoute le contenu d'une room
 * Ecoute les messages reçus
 * 
 */
	static void UpdateLogs(){
			byte[] message = RMQTools.receiveMessage(channel,c_room.QUEUE_ROOM_LOGS_OUT);

			if (message != null) {
				Frame.getWindow().set_chattextarea(new String(message));
				Update();
			}
			try {
				TimeUnit.MILLISECONDS.sleep(100);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	}

/**
 * Ecoute le contenu d'une room
 * La liste des users connectés (connexion deconnexion)
 * 
 */
	static void UpdateUsers(){
		System.out.println("Listening for user list ...");
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				String message = new String(delivery.getBody(), "UTF-8");
				DebugPrint("Received users from Room :\n" + message);
				String[] m = Tools.SerializationTools.myStringUnparser(message);
				Frame.getWindow().UpdateNames(m);
				Update();
			};
			try {
				channel.basicConsume(c_room.QUEUE_ROOM_USERS_OUT, true, deliverCallback, consumerTag -> {});
			}catch (Exception e){
				System.out.println("Erreur de consommation : " + e.getMessage());
				e.printStackTrace();
				return;
			}

		System.out.println("Ending listening thread");
	}

	/**
	 * Ecoute les changements sur la liste des rooms envoyé par le hub
	 * 
	 */
	static void UpdateRooms(){
        System.out.println("Listening for room list ...");
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				String message = new String(delivery.getBody(), "UTF-8");
				DebugPrint("Received Update from hub :\n" + message);
				String[] m = Tools.SerializationTools.myStringUnparser(message);
				Frame.getWindow().UpdateButtons(m);
				Update();
			};
			try {
				channel.basicConsume(QUEUE_HUB_SERVER, true, deliverCallback, consumerTag -> {});
			}catch (Exception e){
				System.out.println("Erreur de consommation : " + e.getMessage());
				e.printStackTrace();
			}


	}


	static void Update() {
		Frame.getWindow().revalidate();
		Frame.getWindow().repaint();
	}

	static void Say(String text) {
		if(text.equals("")){
			RMQTools.sendMessage(channel,QUEUE_HUB_CLIENT,"UPDATE");
			return;
		}
		if(text.startsWith("<connect>")){
			Select_Room(text.substring("<connect>".length()));
			return;
		}
		if(c_room == null)
			return;
		RMQTools.sendMessage(channel,c_room.QUEUE_ROOM_LOGS_IN,text);
	}

	/**
	 * Se connecter à une room spécifique
	 * @param Nom de la room
	 */
	static void Select_Room(String name) {
		String username = Frame.getWindow().user.getText();

		if(c_room != null){
			RMQTools.sendMessage(channel,c_room.QUEUE_ROOM_USERS_IN,"-"+username);
		}

		c_room = RoomId.findByName(rooms,name);

		if(c_room == null){
			//TODO: Appel du hub
			System.out.println("A implementer");
		}

		if(c_room != null){
			if(c_room.QUEUE_ROOM_LOGS_OUT == null) {
				RMQTools.addQueue(channel, c_room.QUEUE_ROOM_USERS_IN);
				c_room.QUEUE_ROOM_USERS_OUT = RMQTools.addQueueBound(channel, c_room.EXCHANGE_ROOM_USERS_OUT);
				RMQTools.addQueue(channel, c_room.QUEUE_ROOM_LOGS_IN);
				c_room.QUEUE_ROOM_LOGS_OUT = RMQTools.addQueueBound(channel, c_room.EXCHANGE_ROOM_LOGS_OUT);
			}
			RMQTools.sendMessage(channel,c_room.QUEUE_ROOM_USERS_IN,"+"+username);
			new Thread(ChatClient::UpdateUsers).start();
			new Thread(ChatClient::UpdateLogs).start();
		}

		Update();
	}

	/**
	 * Demande la création d'une room
	 */
	static void Create_Room() {
        String result = (String)JOptionPane.showInputDialog(
                Frame.getWindow(),
                "Select the room name",
                "New Room",
                JOptionPane.PLAIN_MESSAGE,
                null,
                null,
				""
        );
        if(result == null)
            return;

		RMQTools.sendMessage(channel,QUEUE_HUB_CLIENT,"CREATE "+result);

		Select_Room(result);
	}

	/**
	 * Demande la supression d'une room
	 */
	static void Delete_Room() {
		RMQTools.sendMessage(channel,QUEUE_HUB_CLIENT,"DELETE "+c_room.name);
	}
}
class RoomId{
	String name;
	String EXCHANGE_ROOM_LOGS_OUT ; //Server emet ici
	String QUEUE_ROOM_LOGS_OUT = null; //Client ecoute ici
	String EXCHANGE_ROOM_USERS_OUT; //Server emet ici
	String QUEUE_ROOM_USERS_OUT = null; //Client ecoute ici
	String QUEUE_ROOM_LOGS_IN; //Client emet ici
	String QUEUE_ROOM_USERS_IN; //Client emet ici
	public RoomId(String name){
		this.name = name;
	}
	public static RoomId findByName(Collection<RoomId> listCarnet, String nameIsIn) {
		return listCarnet.stream().filter(carnet -> nameIsIn.equals(carnet.name)).findFirst().orElse(null);
	}
}

