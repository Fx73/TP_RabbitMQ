
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeoutException;

public class ChatServer {
	static ChatHub hub;




	public static void  main(String [] args) throws IOException, TimeoutException {
		hub	= new ChatHub();

		Load();




		// Set up a timer for save
		Timer timer = new Timer();
		timer.scheduleAtFixedRate(new TimerTask() {
			public void run() {
				Save();
			}
		}, 10000, 10000);


		Runtime.getRuntime().addShutdownHook(new Thread(() -> {
			try {
				Save();

			} catch (Exception e) {
				System.out.println("Error on exit : ");
				e.printStackTrace();
			}

		}));
  }

  static void Load(){
	  try {
		  FileInputStream fi = new FileInputStream("hubsave.txt");
		  ObjectInputStream oi = new ObjectInputStream(fi);

		  hub = (ChatHub) oi.readObject();

		  oi.close();
		  fi.close();
		  System.out.println("Save File found");
	  } catch (IOException | ClassNotFoundException e) {
		  hub = new ChatHub();
		  System.out.println("No save found");
	  }
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
