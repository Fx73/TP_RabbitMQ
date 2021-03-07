package Server;

import Client.ChatClient;

import java.io.*;
import java.rmi.AlreadyBoundException;
import java.util.Arrays;
import java.util.Timer;
import java.util.TimerTask;

public class ChatServer {
	static ChatHub hub;


	public static void  main(String [] args){
		hub = new ChatHub();

		if(!hub.Init())
			return ;

		Load();




		// Set up a timer for save
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				Save();
			}
		}, 10000, 10000);

        new Thread(()->hub.WaitForRooms()).start();
		hub.WaitForMessages();

  }

  static boolean Load(){
	  try {
		  FileInputStream fi = new FileInputStream("hubsave.txt");
		  ObjectInputStream oi = new ObjectInputStream(fi);

		  String[] roomlist = (String[]) oi.readObject();
		  hub.namelist.addAll(Arrays.asList(roomlist));

          for (String roomname:roomlist) {
              Timer timer = new Timer(roomname);
              timer.schedule(new TimerTask() {
                  @Override
                  public void run() {
                      hub.Relaunch_Room(roomname);
                  }
              }, 20000);
              hub.timers.add(timer);
          }


		  oi.close();
		  fi.close();
		  System.out.println("Save File found");
	  } catch (IOException | ClassNotFoundException e) {
		  System.out.println("No save found");
		  return false;
	  }
	  return true;
  }

  static void Save(){
	  try {
		  FileOutputStream f = new FileOutputStream("hubsave.txt");
		  ObjectOutputStream o = new ObjectOutputStream(f);

		  o.writeObject(hub.namelist.toArray(new String[0]));

		  o.close();
		  f.close();
	  } catch (IOException e) {
		  System.out.println("Save log fail : "+e);
	  }
  }

}
