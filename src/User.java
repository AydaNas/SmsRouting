import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import java.util.Scanner;

public class User {

    private static final String EXCHANGE_NAME = "sms";
    private static final String ROUTING_KEY = "sms.forward.A"; // routing key
    private static final int TTL = 10; // set ttl

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        try (Connection connection = factory.newConnection();
            Channel channel = connection.createChannel()) {

            channel.exchangeDeclare(EXCHANGE_NAME, BuiltinExchangeType.TOPIC);

            String from = getFrom(argv);
            String to = getTo(argv);
            String message = getMessage(argv);

            Message msg = new Message(from, to, message, TTL);
            String data = serialize(msg);

            channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, null, data.getBytes("UTF-8"));
            System.out.println(" [x] Sent '" + ROUTING_KEY + "':'" + msg + "'");
        }
    }

    private static String getFrom(String[] args) {
        if (args.length < 1) {
            return "A1";
        }
        return args[0];
    }

    private static String getTo(String[] args) {
        if (args.length < 2) {
            return "A2";
        }
        return args[1];
    }

    private static String getMessage(String[] args) {
        if (args.length < 3) {
            // use scanner to get messages
            Scanner scanner = new Scanner(System.in);
            System.out.println("Enter a message:");
            return scanner.nextLine();
        }
        return joinStrings(args, " ", 2);
    }

    private static String joinStrings(String[] strings, String delimiter, int startIndex) {
        int length = strings.length;
        if (length == 0) return "";
        if (length < startIndex) return "";
        StringBuilder words = new StringBuilder(strings[startIndex]);
        for (int i = startIndex + 1; i < length; i++) {
            words.append(delimiter).append(strings[i]);
        }
        return words.toString();
    }

    private static String serialize(Message msg) {
        return msg.from + "|" + msg.to + "|" + msg.ttl + "|" + msg.message;
    }
}
