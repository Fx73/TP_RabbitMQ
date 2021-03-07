import Tools.RMQTools;
import com.rabbitmq.client.Channel;

import java.nio.charset.StandardCharsets;

public class TestProducer {
    private final static String QUEUE_NAME = "hello";

    public static void main(String[] argv) throws Exception {
        Channel channel = RMQTools.channelCreatorLocal();


        RMQTools.addQueue(channel,QUEUE_NAME);

        String message = "Hello World!";

        RMQTools.sendMessage(channel,QUEUE_NAME,message);

        System.out.println(" [x] Sent '" + message + "'");
    }

}
