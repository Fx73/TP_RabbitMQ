package Tools;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class RMQTools {

    public static void addExchange(Channel channel, String exchangename){
        boolean durable = false;    //durable - RabbitMQ will never lose the queue if a crash occurs
        boolean exclusive = false;  //exclusive - if queue only will be used by one connection
        boolean autoDelete = false; //autodelete - queue is deleted when last consumer unsubscribes

        try {
            channel.exchangeDeclare(exchangename, "fanout");
        }
        catch (Exception e){
            System.out.println("Erreur de creation de exchanger " + exchangename + " sur le channel : ");
            e.printStackTrace();
        }
    }
    public static String addQueueBound(Channel channel, String exchangename){
        boolean durable = false;    //durable - RabbitMQ will never lose the queue if a crash occurs
        boolean exclusive = false;  //exclusive - if queue only will be used by one connection
        boolean autoDelete = false; //autodelete - queue is deleted when last consumer unsubscribes
        String queuename = null;
        try {
            channel.exchangeDeclare(exchangename, "fanout");
            queuename = channel.queueDeclare().getQueue();
            channel.queueBind(queuename, exchangename, "");
        }
        catch (Exception e){
            System.out.println("Erreur de creation de queue " + exchangename + " sur le channel : "+ e.getMessage());
        }
        return queuename;
    }
    public static void addQueue(Channel channel, String queuename){
        boolean durable = false;    //durable - RabbitMQ will never lose the queue if a crash occurs
        boolean exclusive = false;  //exclusive - if queue only will be used by one connection
        boolean autoDelete = false; //autodelete - queue is deleted when last consumer unsubscribes

        try {
            channel.queueDeclare(queuename, durable, exclusive, autoDelete, null);
        }
        catch (Exception e){
            System.out.println("Erreur de creation de queue " + queuename + " sur le channel : "+ e.getMessage());
        }
    }

    public static void deleteQueue(Channel channel, String queuename){
        try {
            channel.queueDelete(queuename);
        }
        catch (Exception e){
            System.out.println("Erreur de creation de queue " + queuename + " sur le channel : "+ e.getMessage());
        }
    }

    public static Channel channelCreatorCloud() {
        ConnectionFactory factory = new ConnectionFactory();

        factory.setHost("chinook.rmq.cloudamqp.com");
        factory.setUsername("bosxyftt");
        factory.setPassword("IOZfKOaTzJ9eIfyHXNalUBvvlgNRRP6T");
        factory.setVirtualHost("bosxyftt");

        return channelCreator(factory);
    }

    public static Channel channelCreatorLocal() {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");

        return channelCreator(factory);
    }

    private static Channel channelCreator(ConnectionFactory factory){
        //Recommended settings
        factory.setRequestedHeartbeat(30);
        factory.setConnectionTimeout(30000);

        Connection connection = null;
        Channel channel = null;

        try {
            connection = factory.newConnection();
        }
        catch (Exception e){
            System.out.println("Erreur de connection au serveur CloudAmq : " + e.getMessage());
            return null;
        }

        try {
            channel = connection.createChannel();
        }
        catch (Exception e){
            System.out.println("Erreur de creation de channel sur le serveur");
            return null;
        }

        return channel;
    }



    public static byte[] receiveMessage(Channel channel, String queuename){
        final RMQMessage m = new RMQMessage();
        DeliverCallback deliverCallback = (consumerTag, delivery) -> m.msg = delivery.getBody();
        try {
            channel.basicConsume(queuename, true, deliverCallback, consumerTag -> {
            });
        }catch (Exception e){
            System.out.println("Erreur de consommation na : " + e.getMessage());
        }
        return m.msg;
    }

    public static byte[] receiveMessageWaited(Channel channel, String queuename, int waittimemax){
        final RMQMessage m = new RMQMessage();

        DeliverCallback deliverCallback = (consumerTag, delivery) -> m.msg = delivery.getBody();

        try {
            channel.basicConsume(queuename, true, deliverCallback, consumerTag -> {});
        }catch (Exception e){
            System.out.println("Erreur de consommation a : " +e.getMessage());
        }

        while (m.msg == null){
            try {
                TimeUnit.MILLISECONDS.sleep(100);
            }catch (Exception e){
                System.out.println("Erreur d'attente de thread : " + e.getMessage());
            }
            waittimemax--;
            if(waittimemax <= 0){
                System.out.println("Time out d'attente du message");
                return null;
            }
        }
        return m.msg;
    }


    public static void sendMessage(Channel channel, String queue, String s){
        try {
            channel.basicPublish(queue, "", null, s.getBytes());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}

class RMQMessage{
    byte[] msg;
}
