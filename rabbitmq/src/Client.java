
import com.rabbitmq.client.*;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.UUID;

public class Client {

    private static final String EXCHANGE_NAME = "chat_exchange";

    public static void main(String[] args) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost"); // Connect to RabbitMQ server

        String clientId = UUID.randomUUID().toString(); // Generate unique client ID
        System.out.println("Your client ID: " + clientId);

        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {

            // Declare a unique queue for this client
            String queueName = channel.queueDeclare().getQueue();
            channel.queueBind(queueName, EXCHANGE_NAME, "");
            System.out.println("Client connected. Waiting for messages...");

            // Start a thread to listen for messages
            Thread receiverThread = new Thread(() -> {
                try {
                    DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                        String message = new String(delivery.getBody(), "UTF-8");
                        String senderId = delivery.getProperties().getHeaders().get("clientId").toString();

                        // Ignore messages sent by this client
                        if (!senderId.equals(clientId)) {
                            System.out.println("Received: " + message);
                        }
                    };

                    channel.basicConsume(queueName, true, deliverCallback, consumerTag -> {
                    });
                } catch (Exception e) {
                    System.err.println("Error in receiver thread: " + e.getMessage());
                }
            });
            receiverThread.start();

            // Main thread handles sending messages
            BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                System.out.print("Enter a message: ");
                String message = reader.readLine();
                if (message.equalsIgnoreCase("exit")) {
                    System.out.println("Exiting...");
                    break;
                }

                // Add client ID to the message headers
                AMQP.BasicProperties props = new AMQP.BasicProperties.Builder()
                        .headers(Map.of("clientId", clientId))
                        .build();

                channel.basicPublish(EXCHANGE_NAME, "", props, message.getBytes("UTF-8"));
                System.out.println("Sent: " + message);
            }
        }
    }
}
