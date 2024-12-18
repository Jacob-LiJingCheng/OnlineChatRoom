package ServerSystem;

import Message.Message;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Pattern;

public class Server {

    private ServerSocket serverSocket;
    // 用于存储已连接客户端的信息，键为客户端的唯一标识（这里简单用Socket表示），值为对应的输出流
    private final Map<Socket, ObjectOutputStream> clientStreams = new ConcurrentHashMap<>();
    // 存储用户名和对应Socket的映射
    private final Map<String, Socket> userSocketMap = new ConcurrentHashMap<>();
    private ArrayList<User> users = new ArrayList<>();
    // 用户名和密码的正则表达式
    private static final Pattern USERNAME_PASSWORD_PATTERN = Pattern.compile("^[a-zA-Z0-9]{6,12}$");
    
    // 构造函数，用于初始化服务端并监听指定端口
    public Server(int port) {
        try {
            users = UsefulToolsForServer.readUsersFromFile();
            if(users==null){
                System.out.println("Users file not found");
                users = new ArrayList<>();
            }
            serverSocket = new ServerSocket(port);
            System.out.println("Server started and listening on port " + port);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 处理客户端连接的方法，为每个新连接的客户端创建一个线程来处理消息接收和转发
    public void handleConnections() {
        while (true) {
            try {
                if (serverSocket.isClosed()) {
                    break; // 退出循环，停止接受新的客户端连接
                }
                Socket clientSocket = serverSocket.accept();
                System.out.println("client connect：" + clientSocket.getInetAddress());

                // 为新连接的客户端创建输出流并存储到clientStreams中
                ObjectOutputStream outputStream = new ObjectOutputStream(clientSocket.getOutputStream());
                clientStreams.put(clientSocket, outputStream);

                // 创建一个线程来处理该客户端的消息接收和转发
                Thread clientThread = new Thread(() -> handleClient(clientSocket));
                clientThread.start();
            } catch (SocketException e) {
                System.out.println("socket exception");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    // 处理单个客户端消息的方法，接收客户端发送的消息，进行处理并转发给相应的接收者
    private void handleClient(Socket clientSocket) {
        boolean online = false;
        try {
            ObjectInputStream inputStream = new ObjectInputStream(clientSocket.getInputStream());
            Message receivedMessage;
            Message sendMessage;
            User user = null;
            //offline
            while (!online) {
                receivedMessage = (Message) inputStream.readObject();
                switch (receivedMessage.getOperation()) {
                    case LOGIN:
                        if((sendMessage = dealLoginMes(receivedMessage,clientSocket))!=null){
                            if(sendMessage.getContent().equals("correct")){
                                online = true;
                                String name = receivedMessage.getContent().split("@@")[0];
                                user=findUserByUsername(name);
                                assert user != null;
                                user.setStatus(User.Status.ONLINE);
                            }
                            sendMessage(sendMessage,clientSocket);
                        }
                        break;
                    case REGISTER:
                        if((sendMessage = dealRegisterMes(receivedMessage))!=null){
                            sendMessage(sendMessage,clientSocket);
                        }
                        break;
                }
            }
            //online
            while (online) {
                receivedMessage = (Message) inputStream.readObject();
                switch (receivedMessage.getOperation()){
                    case UPDATE_STATUE:
                        Message statusResponse = dealUpdateStatus(receivedMessage);
                        sendMessage(statusResponse, clientSocket);
                        System.out.println(user.getUsername()+" ask for status list");
                        break;
                    case SEND_MESSAGE:
                        dealChatMes(receivedMessage,clientSocket);
                        break;
                    case SEARCH_USER:
                        if((sendMessage = dealSearchUser(receivedMessage))!=null){
                            sendMessage(sendMessage,clientSocket);
                            System.out.println(user.getUsername()+" check the user exist or not");
                        }
                        break;
                    case ASK_OFFLINE_MES:
                        dealAskOfflineMes(receivedMessage,clientSocket, user.getUsername());
                        System.out.println(user.getUsername()+" ask for offline mes");
                        break;
                    case UPDATE_STATUE_SINGLE:
                        dealUpdateStatusSingle(receivedMessage,clientSocket);
                        System.out.println(user.getUsername()+" ask for one's status");
                        break;
                }
            }
        } catch (IOException e) {
            System.out.println("one user exit");
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            handleClientDisconnection(clientSocket);
        }
    }

    private void dealUpdateStatusSingle(Message requestMessage, Socket clientSocket) {
        String username = requestMessage.getContent();
        User user = findUserByUsername(username);

        // 构建响应
        Message response;
        if (user != null) {
            // 如果用户存在，返回用户名和状态
            String status = (user.isOnline()) ? "ONLINE" : "OFFLINE";
            response = new Message(
                    "server", // 消息发送者
                    null, // 消息接收者
                    Message.Operation.UPDATE_STATUE_SINGLE, // 请求操作
                    username + "@@" + status // 状态信息
            );
        } else {
            // 用户不存在，返回错误信息
            response = new Message(
                    "server", // 消息发送者
                    null, // 消息接收者
                    Message.Operation.UPDATE_STATUE_SINGLE, // 请求操作
                    "error" // 错误信息
            );
        }

        // 发送响应
        try {
            ObjectOutputStream outputStream = clientStreams.get(clientSocket);
            outputStream.writeObject(response);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 验证用户名和密码的格式
    private boolean isValidFormat(String username, String password) {
        return USERNAME_PASSWORD_PATTERN.matcher(username).matches()
                && USERNAME_PASSWORD_PATTERN.matcher(password).matches();
    }

    // 登录请求处理
    public Message dealLoginMes(Message mes, Socket clientSocket) {
        String content = mes.getContent();
        String[] parts = content.split("@@");

        if (parts.length == 2) {
            String username = parts[0];
            String password = parts[1];

            for (User user : users) {
                if (user.getUsername().equals(username) && user.getPassword().equals(password)) {
                    // 用户验证成功，将用户名与Socket绑定
                    userSocketMap.put(username, clientSocket);
                    System.out.println("user " + username + " login in and connected");
                    // 返回成功消息
                    return new Message(null, null, Message.Operation.LOGIN, "correct");
                }
            }

            // 用户名或密码错误
            return new Message(null, null, Message.Operation.LOGIN, "error@@match");
        }

        // 格式错误
        return new Message(null, null, Message.Operation.LOGIN, "error@@form");
    }


    // 注册请求处理
    public Message dealRegisterMes(Message mes) {
        String content = mes.getContent();
        String[] parts = content.split("@@");

        if (parts.length == 2) {
            String username = parts[0];
            String password = parts[1];

            if (!isValidFormat(username, password)) {
                // 格式错误
                return new Message(null, null, Message.Operation.REGISTER, "error@@format");
            }

            for (User user : users) {
                if (user.getUsername().equals(username)) {
                    // 用户名已存在
                    return new Message(null, null, Message.Operation.REGISTER, "error@@exist");
                }
            }

            // 创建新用户并添加到用户列表
            User newUser = new User(username, password);
            users.add(newUser);

            // 更新用户数据到文件（假设已有对应方法）
//            UsefulToolsForServer.writeUsersToFile(users);

            // 注册成功
            System.out.println(username+" register successfully with "+password);
            return new Message(null, null, Message.Operation.REGISTER, "correct");
        }
        // 格式错误
        return new Message(null, null, Message.Operation.REGISTER, "error@@format");
    }

    public void dealChatMes(Message message, Socket senderSocket) {
        String sender = message.getSender();
        List<String> receivers = message.getReceivers();
        String content = message.getContent();

        // 验证消息格式
        if (content == null || content.length() > 50) {
            sendErrorMessage(senderSocket, "error@@format", sender);
            return;
        }

        // 遍历接收者列表，将消息转发
        for (String receiver : receivers) {
            Socket receiverSocket = findSocketByUsername(receiver);
            Message send = new Message(message.getSender(), receivers,
                    Message.Operation.RECEIVE_MESSAGE, message.getContent());
            send.setTimestamp(message.getTimestamp());
            if (receiverSocket != null && clientStreams.containsKey(receiverSocket)) {
                try {
                    ObjectOutputStream outputStream = clientStreams.get(receiverSocket);
                    outputStream.writeObject(send);
                    outputStream.flush();
                    System.out.println(sender + " sends message to " + receiver);
                } catch (IOException e) {
                    e.printStackTrace();
                    sendErrorMessage(senderSocket, "error@@delivery", sender);
                }
            } else {
                for(User user:users){
                    if (user.getUsername().equals(receiver)){
                        // 如果接收者离线且存在，将消息存储为离线消息
                        storeOfflineMessage(receiver, message);
                        break;
                    }
                }
            }
        }

        // 向发送者确认消息已成功处理(暂时不用)
        //sendSuccessMessage(senderSocket, sender, content);
    }

    private Message dealUpdateStatus(Message request) {
        List<String> usernameList = request.getElementList(); // 获取请求中的用户名列表
        Map<String, Object> userStatusMap = new HashMap<>();

        // 遍历 users 列表，获取每个用户名的状态
        for (String username : usernameList) {
            boolean found = false;
            for (User user : users) {
                if (user.getUsername().equals(username)) {
                    found = true;
                    userStatusMap.put(username, user.isOnline());
                    break;
                }
            }
            // 如果用户不在 users 列表中，状态默认为离线
            if (!found) {
                userStatusMap.put(username, false);
            }
        }

        // 构造响应消息
        return new Message(
                "server", // 消息发送者
                null, // 消息接收者
                Message.Operation.UPDATE_STATUE, // 消息类型
                "User status update response", // 消息内容
                userStatusMap // 状态信息
        );
    }

    private Message dealSearchUser(Message request) {
        String usernameToCheck = request.getContent(); // 获取待检查的用户名
        Message response;

        if (usernameToCheck == null || usernameToCheck.isEmpty()) {
            // 如果用户名为空或格式错误
            response = new Message(
                    null,
                    null,
                    Message.Operation.SEARCH_USER,
                    "error@@format"
            );
        } else {
            // 遍历用户列表检查用户名是否存在
            boolean userExists = users.stream()
                    .anyMatch(user -> user.getUsername().equals(usernameToCheck));

            // 构造响应消息
            if (userExists) {
                response = new Message(
                        null,
                        null,
                        Message.Operation.SEARCH_USER,
                        "exist"
                );
            } else {
                response = new Message(
                        null,
                        null,
                        Message.Operation.SEARCH_USER,
                        "miss"
                );
            }
        }
        return response;
    }

    // 处理离线消息请求
    private void dealAskOfflineMes(Message requestMessage, Socket clientSocket, String username) {
        User user = findUserByUsername(username);

        // 构建离线消息响应
        Message response;
        if (user != null) {
            Map<String,ArrayList<String>> offlineMessages = user.getOfflineMessages();  // 获取离线消息
            if (offlineMessages != null && !offlineMessages.isEmpty()) {
                response = new Message(
                        "server", // 消息发送者
                        null, // 消息接收者
                        Message.Operation.ASK_OFFLINE_MES, // 请求操作
                        "correct" // 正确响应
                );
                response.setMesMap(offlineMessages);
                //清空消息
                user.clearOfflineMessage();
            } else {
                // 如果没有离线消息
                response = new Message(
                        "server", // 消息发送者
                        null, // 消息接收者
                        Message.Operation.ASK_OFFLINE_MES, // 请求操作
                        "null"
                );
            }
        } else {
            // 用户不存在
            response = new Message(
                    "server", // 消息发送者
                    null, // 消息接收者
                    Message.Operation.ASK_OFFLINE_MES, // 请求操作
                    "error" // 错误内容
            );
        }

        try {
            ObjectOutputStream outputStream = clientStreams.get(clientSocket);
            outputStream.writeObject(response);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(Message mes,Socket socket) throws IOException {
        ObjectOutputStream outputStream = clientStreams.get(socket);
        try {
            synchronized(outputStream) {  // 确保线程安全
                outputStream.writeObject(mes);
                outputStream.flush();  // 确保数据被发送
            }
        } catch (IOException e) {
            System.err.println("Error sending message: " + e.getMessage());
            // 处理连接断开等情况
        }
    }

    private Socket findSocketByUsername(String username) {
        return userSocketMap.get(username); // 根据用户名直接获取对应的Socket
    }

    private void sendErrorMessage(Socket socket, String errorContent, String sender) {
        try {
            ObjectOutputStream outputStream = clientStreams.get(socket);
            Message errorMessage = new Message(sender, null, Message.Operation.SEND_MESSAGE, errorContent);
            outputStream.writeObject(errorMessage);
            outputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendSuccessMessage(Socket socket, String sender, String content) {
        try {
            ObjectOutputStream outputStream = clientStreams.get(socket);
            Message successMessage = new Message(sender, null, Message.Operation.SEND_MESSAGE, "Success");
            outputStream.writeObject(successMessage);
            outputStream.flush();
            System.out.println(" Successful response sender: "+ sender);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private User findUserByUsername(String username) {
        for (User user : users) {
            if (user.getUsername().equals(username)) {
                return user; // 找到对应的用户，返回该 User 对象
            }
        }
        return null; // 如果未找到对应的用户，返回 null
    }

    private void storeOfflineMessage(String username, Message message) {
        User user = findUserByUsername(username);
        if (user != null) {
            user.addOfflineMessage(message);
            System.out.println("the offline message is stored in " + username);
        }
    }

    private void handleClientDisconnection(Socket clientSocket) {
        // 找到断开连接的用户
        String disconnectedUser = null;
        for (Map.Entry<String, Socket> entry : userSocketMap.entrySet()) {
            if (entry.getValue().equals(clientSocket)) {
                disconnectedUser = entry.getKey();
                break;
            }
        }

        // 移除映射关系
        if (disconnectedUser != null) {
            findUserByUsername(disconnectedUser).setStatus(User.Status.OFFLINE);
            userSocketMap.remove(disconnectedUser);
            System.out.println("user " + disconnectedUser + " disconnected.");
        }

        // 从客户端流列表中移除Socket
        clientStreams.remove(clientSocket);

        // 关闭Socket
        try {
            clientSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 关闭服务端的方法
    public void closeServer() {
        try {
            //set all users to offline and save data to local
            for (User user : users) {
                user.setStatus(User.Status.OFFLINE);
            }
            UsefulToolsForServer.saveUsersToFile(users);

            // 关闭所有已连接客户端的输出流（这里只是简单遍历关闭，实际可能需要更细致的处理）
            for (ObjectOutputStream outputStream : clientStreams.values()) {
                if (outputStream!= null) {
                    outputStream.close();
                }
            }
            if (serverSocket!= null) {
                serverSocket.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        // 示例用法：创建服务端并监听8888端口
        Server server = new Server(8888);
        // 启动一个线程监听控制台输入，用于退出服务端
        Thread consoleThread = new Thread(() -> {
            Scanner scanner = new Scanner(System.in);
            while (true) {
                String command = scanner.nextLine();
                if ("exit".equalsIgnoreCase(command)) {
                    System.out.println("Exit command received, shutting down server...");
                    server.closeServer();
                    System.exit(0); // 退出程序
                }
            }
        });
        consoleThread.setDaemon(true); // 设置为守护线程，确保主线程结束时自动退出
        consoleThread.start();

        // 开始处理客户端连接
        server.handleConnections();
    }
}