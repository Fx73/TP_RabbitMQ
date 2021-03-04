import java.io.IOException;
import java.io.Serializable;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

public class ChatRoom implements Serializable {
    ChatLog _chatlog;
    String _name;
    transient ArrayList<String> users = new ArrayList<>();
    transient ArrayList<Timer> timers = new ArrayList<>();

    ChatRoom(String name){
        _name = name;
        _chatlog = new ChatLog(100);
    }

    public String GetRoomName() throws RemoteException{
        return _name;
    }

    public void Say(String name, String s) throws RemoteException {
        _chatlog.Add_log(name,s);
    }

    public String Get_chatlog() throws RemoteException{
        return _chatlog.Get_Logs();
    }

    public void Register_User(String name){
        Timer timer = new Timer(name);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                AutoUnregister_User(name);
            }
        }, 20000);

        if(users.contains(name)){
            timers.set(users.indexOf(name),timer);
        }
        else{
            users.add(name);
            timers.add(timer);
        }
    }

    public void Unregister_User(String name){
        int i = users.indexOf(name);
        if(i == -1){
            System.out.println("Skipping failed unregister for " + name);
            return;
        }
        timers.get(i).cancel();
        timers.remove(i);
        users.remove(name);
    }
    public String[] Get_Users(){
        return users.toArray(new String[0]);
    }

    private TimerTask AutoUnregister_User(String name){
        Unregister_User(name);
        return null;
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        users = new ArrayList<>();
        timers = new ArrayList<>();
    }

}
