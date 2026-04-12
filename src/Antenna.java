import com.rabbitmq.client.*;
import java.util.*;

public class Antenna {

  private static final String EXCHANGE_NAME = "sms"; //exchange
  //private static final String QUEUE_NAME = "antenna.A"; //antenna
  //private static final String BINDING_KEY = "sms.forward.A"; //forward messages


  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    // define antenna
    String antennaName;

    // input antenna name
    if (argv.length >= 1) {
    antennaName = argv[0];
    } else {
        System.err.println("Usage: java Antenna <antenna name>");
        return;
    }

    // define queue name
    String queueName = "antenna." + antennaName;

    // define binding key
    String bindingKey = "sms.forward." + antennaName;

    // get next anternna from hashmap
    String next = Config.nextMap.get(antennaName);
    Set<String> localUsers = Config.userMap.get(antennaName);

    String nextRoutingKey = "sms.forward." + next;

    channel.exchangeDeclare(EXCHANGE_NAME, "topic");
    channel.queueDeclare(queueName, false, false, false, null);
    channel.queueBind(queueName, EXCHANGE_NAME, bindingKey);
    //String queueName = channel.queueDeclare().getQueue();

    System.out.println(" [*] Antenna" + " "  + antennaName + " is waiting for messages.");

    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
        String received = new String(delivery.getBody(), "UTF-8");
        Message msg = deserialize(received);
        System.out.println(" [x] Received '" +
            delivery.getEnvelope().getRoutingKey() + "':'" + msg + "'");

        // if user is in the same antenna, send messages to the local antenna and others foward to the next antenna
        if(localUsers.contains(msg.to)){
          System.out.println("Delivered to " + msg.to);
          System.out.println("Message: " + msg.message);
        } else {
          System.out.println("User " + msg.to + " is not in the local antenna");
          msg.ttl--;
          // when ttl becomes 0, stop forwarding
          if (msg.ttl <= 0) {
          System.out.println("TTL expired: " + msg);
          return;
          }

          System.out.println("Forwarding to next antenna... TTL=" + msg.ttl);
          String data = serialize(msg);
          channel.basicPublish(EXCHANGE_NAME, nextRoutingKey, null, data.getBytes("UTF-8"));
        }
    };
    channel.basicConsume(queueName, true, deliverCallback, consumerTag -> { });
  }

  // messages separeted by |
  private static Message deserialize(String s) {
        String[] parts = s.split("\\|", 4);
        return new Message(
                parts[0],
                parts[1],
                parts[3],
                Integer.parseInt(parts[2])
        );
  }

  private static String serialize(Message msg) {
        return msg.from + "|" + msg.to + "|" + msg.ttl + "|" + msg.message;
    }
}