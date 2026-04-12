import com.rabbitmq.client.*;
import java.util.*;

public class Antenna {

  private static final String EXCHANGE_NAME = "sms"; //exchange
  private static final String QUEUE_NAME = "antenna.A"; //antenna
  private static final String BINDING_KEY = "sms.forward.A"; //forward messages
  private static final String NEXT_ROUTING_KEY = "sms.forward.B"; // next hop

    // Create a Set with HashSet
  private static final Set<String> localUsers = new HashSet<>();

  public static void main(String[] argv) throws Exception {
    ConnectionFactory factory = new ConnectionFactory();
    factory.setHost("localhost");
    Connection connection = factory.newConnection();
    Channel channel = connection.createChannel();

    channel.exchangeDeclare(EXCHANGE_NAME, "topic");
    channel.queueDeclare(QUEUE_NAME, false, false, false, null);
    channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, BINDING_KEY);
    //String queueName = channel.queueDeclare().getQueue();

    // add users
    localUsers.add("U1");
    localUsers.add("U2");

    System.out.println(" [*] Antenna is waiting for messages.");

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
          channel.basicPublish(EXCHANGE_NAME, NEXT_ROUTING_KEY, null, data.getBytes("UTF-8"));
        }
    };
    channel.basicConsume(QUEUE_NAME, true, deliverCallback, consumerTag -> { });
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