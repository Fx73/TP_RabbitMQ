import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

import java.util.concurrent.TimeUnit;

public class ChatClientWaiter implements Runnable {
    private Channel channel;
    private String queue_in;

    private Object message;

    public boolean running;

    public ChatClientWaiter (Channel channel, String queue_in){
        running = true;
        this.channel = channel;
        this.queue_in = queue_in;

    }

    @Override
    public void run() {
        receiveMessage();

        while (running) {
            if (this.message != null) {
                String message = new String((byte[])this.message);
                //TODO:Gerer le message
            }
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public void receiveMessage(){
        message = null;
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            message = delivery.getBody();
        };
        try {
            channel.basicConsume(queue_in, true, deliverCallback, consumerTag -> {
            });
        }catch (Exception e){
            System.out.println("Erreur de consommation na : " + e.getMessage());
        }
    }
}
