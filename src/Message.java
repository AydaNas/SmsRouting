import com.rabbitmq.client.*;

public class Message {
    public String from;
    public String to;
    public String message;
    public int ttl;

    public Message(String from, String to, String message, int ttl) {
        this.from = from;
        this.to = to;
        this.message = message;
        this.ttl = ttl;
    }

    @Override
    public String toString() {
        return "Message{" +
                "from='" + from + '\'' +
                ", to='" + to + '\'' +
                ", message='" + message + '\'' +
                ", ttl=" + ttl +
                '}';
    }
}
