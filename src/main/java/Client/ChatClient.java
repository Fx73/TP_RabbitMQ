package Client;

import javax.swing.*;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import java.util.Timer;

import Server.RoomId;
import Tools.RMQTools;
import Tools.SerializationTools;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import static Tools.RMQTools.DebugPrint;
import static Tools.SerializationTools.SEPARATOR;

public class ChatClient {
	protected static Channel channel;
	final static String EXCHANGE_HUB_SERVER = "QUEUE_HUB_SERVER"; //Le client se lie ici
	static String QUEUE_HUB_SERVER;					// pour pouvoir écouter ici
	final static String QUEUE_HUB_CLIENT = "QUEUE_HUB_CLIENT"; //Le client emet ici

	static String usertag,logtag;

	static RoomId c_room = null;

	public static void main(String[] args)  {
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
		java.util.Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				NotifyUserAlive();
			}
		}, 20000, 20000);

	}

/**
 * Ecoute le contenu d'une room
 * Ecoute les messages reçus
 * 
 */
	static void UpdateLogs(){
		System.out.println(" -- Listening for logs ");
		DeliverCallback deliverCallback = (consumerTag, delivery) -> {
			String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
			Frame.getWindow().set_chattextarea(message);
			Update();
		};
		try {
			logtag = channel.basicConsume(c_room.QUEUE_ROOM_LOGS_OUT, true, deliverCallback, consumerTag -> {});
		}catch (Exception e){
			System.out.println("Erreur de consommation : " + e.getMessage());
			e.printStackTrace();
		}
	}

/**
 * Ecoute le contenu d'une room
 * La liste des users connectés (connexion deconnexion)
 * 
 */
	static void UpdateUsers(){
		System.out.println(" -- Listening for user list ...");
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
				DebugPrint("Received users from Room :\n" + message);
				String[] m = Tools.SerializationTools.myStringUnparser(message);
				Frame.getWindow().UpdateNames(m);
				Update();
			};
			try {
				usertag = channel.basicConsume(c_room.QUEUE_ROOM_USERS_OUT, true, deliverCallback, consumerTag -> {});
			}catch (Exception e){
				System.out.println("Erreur de consommation : " + e.getMessage());
				e.printStackTrace();
			}

	}

	/**
	 * Ecoute les changements sur la liste des rooms envoyé par le hub
	 * 
	 */
	static void UpdateRooms(){
        System.out.println(" - Listening for room list ...");
			DeliverCallback deliverCallback = (consumerTag, delivery) -> {
				String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
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



	static void NotifyUserAlive(){
		if(c_room != null)
			RMQTools.sendMessage(channel,c_room.QUEUE_ROOM_USERS_IN,"+"+Frame.getWindow().user.getText());
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
		if(c_room != null)
			RMQTools.sendMessage(channel,c_room.QUEUE_ROOM_LOGS_IN,Frame.getWindow().user.getText()+ SEPARATOR +text);
	}

	/**
	 * Se connecter à une room spécifique
	 * @param name Nom de la room
	 */
	static void Select_Room(String name) {
		String username = Frame.getWindow().user.getText();

		if(c_room != null){
			RMQTools.sendMessage(channel,c_room.QUEUE_ROOM_USERS_IN,"-"+username);
			//TODO : Fermer les listeners
			try {
				channel.basicCancel(logtag);
				channel.basicCancel(usertag);
			} catch (IOException e) {
				System.out.println("Erreur de fermeture du listener");
				e.printStackTrace();
			}
		}

		c_room = new RoomId(name);


		if(c_room.QUEUE_ROOM_LOGS_OUT == null) {
			RMQTools.addQueue(channel, c_room.QUEUE_ROOM_USERS_IN);
			c_room.QUEUE_ROOM_USERS_OUT = RMQTools.addQueueBound(channel, c_room.EXCHANGE_ROOM_USERS_OUT);
			RMQTools.addQueue(channel, c_room.QUEUE_ROOM_LOGS_IN);
			c_room.QUEUE_ROOM_LOGS_OUT = RMQTools.addQueueBound(channel, c_room.EXCHANGE_ROOM_LOGS_OUT);
		}
		RMQTools.sendMessage(channel,c_room.QUEUE_ROOM_USERS_IN,"+"+username);
		new Thread(ChatClient::UpdateUsers).start();
		new Thread(ChatClient::UpdateLogs).start();
		Update();
	}


	/**
	 * Demande la création d'une room
	 */
	static void Create_Room() {
		String result = Frame.getWindow().AskForName("New Room","Select the room name");

        if(result == null)
            return;

		RMQTools.sendMessage(channel,QUEUE_HUB_CLIENT,"CREATE " +Frame.getWindow().user.getText()+ SEPARATOR + result);

		Select_Room(result);
	}

	/**
	 * Demande la supression d'une room
	 */
	static void Delete_Room() {
		RMQTools.sendMessage(channel,QUEUE_HUB_CLIENT,"DELETE " +Frame.getWindow().user.getText()+ SEPARATOR +c_room.name);
	}

	static void Update() {
		Frame.getWindow().revalidate();
		Frame.getWindow().repaint();
	}
}

