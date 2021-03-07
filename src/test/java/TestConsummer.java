import Tools.RMQTools;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;

public class TestConsummer {

    private final static String QUEUE_NAME = "hello";

    public static void main(String[] argv) {
        Channel channel = RMQTools.channelCreatorLocal();

        RMQTools.addQueue(channel,QUEUE_NAME);
        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");

        //Ca marche
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), "UTF-8");
            System.out.println(" [x] Received '" + message + "'");
        };
        try {
            channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> {
            });
        }catch (Exception e){
            System.out.println("Erreur de consommation : " + e.getMessage());
            e.printStackTrace();
        }

        //Ca ne marche pas
        /*
        String message = new String(RMQTools.receiveMessage(channel,QUEUE_NAME));
        System.out.println(" [x] Received '" + message + "'");
        */



    }
}
