
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Channel;

public class Server {

    private static final String EXCHANGE_NAME = "chat_exchange";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); // RabbitMQ is running locally
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {

            // Declare the exchange
            channel.exchangeDeclare(EXCHANGE_NAME, "fanout");
            System.out.println("Server is ready. Exchange created: " + EXCHANGE_NAME);
        }
    }
}
