package Server;

import java.io.Serializable;
import java.util.ArrayList;

public class ChatLog implements Serializable {
    public int MAXSIZE;
    private final ArrayList<Message> log = new ArrayList<>();

    ChatLog(int maxsize){
        MAXSIZE = maxsize;
    }

    public String Get_Logs(){
        StringBuilder builder = new StringBuilder();
        for (Message l:log) {
            builder.append("<").append(l.user).append(">\n").append(l.msg).append("\n");
        }

        return builder.toString();
    }

    public void Add_log(String u, String m){
        log.add(new Message(u,m));
        if(log.size() == MAXSIZE){
            log.remove(0);
        }
    }


}

class Message implements Serializable{
    String user;
    String msg;

    Message(String u, String m){
        user = u;
        msg = m;
    }
}