import com.rabbitmq.client.Channel;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

public class ChatServer {
	static ChatHub hub;


	public static void  main(String [] args){
		if(!Load())
            hub = new ChatHub();

        if(!hub.Init_Hub())
            return ;


		// Set up a timer for save
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				Save();
			}
		}, 10000, 10000);

		hub.WaitForMessages();

  }

  static boolean Load(){
	  try {
		  FileInputStream fi = new FileInputStream("hubsave.txt");
		  ObjectInputStream oi = new ObjectInputStream(fi);

		  hub = (ChatHub) oi.readObject();

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

		  o.writeObject(hub);

		  o.close();
		  f.close();
	  } catch (IOException e) {
		  System.out.println("Save log fail : "+e);
	  }
  }

}
