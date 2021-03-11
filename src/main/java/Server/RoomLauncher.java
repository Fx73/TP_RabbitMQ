package Server;

import java.io.*;
import java.util.Timer;
import java.util.TimerTask;

public class RoomLauncher {
    static ChatRoom room;
    public static void  main(String [] args){
        if(args.length == 0){
            System.out.println("You must specify a name");
            return;
        }
        if(!Load(args[0]))
            room = new ChatRoom(args[0]);

        if(!room.Init())
            return ;


        // Set up a timer for save
        Timer timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                Save(args[0]);
            }
        }, 10000, 10000);


        new Thread(() -> room.WaitForUsers()).start();
        room.WaitForMessages();


    }

    static boolean Load(String name){
        try {
            FileInputStream fi = new FileInputStream("roomsave_"+name+".txt");
            ObjectInputStream oi = new ObjectInputStream(fi);

            room = (ChatRoom) oi.readObject();

            oi.close();
            fi.close();
            System.out.println("Save File found");
        } catch (IOException | ClassNotFoundException e) {
            System.out.println("No save found");
            e.printStackTrace();
            return false;
        }
        return true;
    }


    static void Save(String name){
        try {
            FileOutputStream f = new FileOutputStream("roomsave_"+name+".txt");
            ObjectOutputStream o = new ObjectOutputStream(f);

            o.writeObject(room);

            o.close();
            f.close();
        } catch (IOException e) {
            System.out.println("Save log fail : "+e);
        }
    }

}
