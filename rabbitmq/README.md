# Functionality
* [RabbitMQ](https://www.rabbitmq.com/) server is used

* *Server.java* and *Client.java* classes are implemented using **Publish/Subscribe** design pattern.

* The functionality allows each client to send messages to all connected client.

* No client can send a message to itself as unique ID is assigned to each connecting (binding) queue
