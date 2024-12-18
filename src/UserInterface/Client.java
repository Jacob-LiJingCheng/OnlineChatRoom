package UserInterface;
import java.util.HashMap;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.Map;
import Message.Message;

public class Client {

    private Socket socket;
    private ObjectOutputStream outputStream;

    private ObjectInputStream inputStream;
    private boolean isConnected;
    // 在 Client 类中添加的新字段
    private boolean isLoggedIn = false;
    private String currentUser = null;

    private boolean isFinishedSending = false;
    private boolean isFinishedReceiving = false;
    private static Message currentMessage;
    private static boolean alreadyRead = true;
    private static boolean isRunning = false;


    // 构造函数，用于初始化客户端并尝试连接到服务端
    public Client(String serverAddress, int serverPort) {
        try {
            socket = new Socket(serverAddress, serverPort);
            outputStream = new ObjectOutputStream(socket.getOutputStream());
            inputStream = new ObjectInputStream(socket.getInputStream());
            isRunning=true;
        } catch (IOException e) {
            throw new RuntimeException("Failed to connect to server: " + e.getMessage(), e);
        }
    }

    //TODO 加入的代码(文档)
    // 验证用户名和密码格式
    private boolean validateCredentials(String username, String password) {
        // 验证用户名: 6-12字符，仅允许字母和数字
        if (!Pattern.matches("^[a-zA-Z0-9]{6,12}$", username)) {
            System.out.println("User name format is incorrect! Must be 6-12 letters or numbers.");
            return false;
        }

        // 验证密码: 6-12字符，仅允许字母和数字
        if (!Pattern.matches("^[a-zA-Z0-9]{6,12}$", password)) {
            System.out.println("Password format is incorrect! Must be 6-12 letters or numbers.");
            return false;
        }

        return true;
    }

    // 登录方法
    public boolean login(String username, String password) {
        if (!validateCredentials(username, password)) {
            return false;
        }

        try {
            // 构造登录消息
            List<String> receivers = new ArrayList<>();
            receivers.add("SERVER");
            String loginContent = username + "@@" + password;
            Message loginMessage = new Message(
                    username,
                    receivers,
                    Message.Operation.LOGIN,
                    loginContent
            );

            // 发送登录请求
            outputStream.writeObject(loginMessage);
            outputStream.flush();

            // 等待服务器响应
            Message response = (Message) inputStream.readObject();

            // 检查响应类型和内容
            if (response.getOperation() == Message.Operation.LOGIN) {
                if (response.getContent().equals("correct")) {
                    isLoggedIn = true;
                    currentUser = username;
                    System.out.println("login successfully!");
                    return true;
                } else {
                    System.out.println("login failure: " + response.getContent());
                    return false;
                }
            }
            return false;  // 如果不是LOGIN操作
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return false;
        }
    }

    public Message register(String username, String password) {
        // 验证输入格式
        if (!isValidInput(username, password)) {
            System.out.println("Incorrect format: The username and password must be a 6-12 digit alphanumeric combination.");
            return null;
        }

        try {
            // 构造注册消息
            List<String> receivers = new ArrayList<>();
            receivers.add("SERVER");
            String content = username + "@@" + password;
            Message registerMessage = new Message(
                    username,
                    receivers,
                    Message.Operation.REGISTER,
                    content
            );

            // 发送注册请求
            outputStream.writeObject(registerMessage);
            outputStream.flush();

            // 等待服务器响应
            Message response = (Message) inputStream.readObject();

            // 检查响应类型和内容
            if (response.getOperation() == Message.Operation.REGISTER) {
                if (response.getContent().equals("correct")) {
                    System.out.println("registered successfully! ");
                } else {
                    System.out.println("fail to register: " + response.getContent());
                }
                return response;  // 返回服务器的响应消息
            }
            return null; // 如果不是REGISTER操作

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    private boolean isValidInput(String username, String password) {
        // 验证用户名和密码是否符合要求：6-12位字母数字
        String regex = "^[a-zA-Z0-9]{6,12}$";
        return username != null && password != null &&
                username.matches(regex) && password.matches(regex);
    }

    // 发送消息方法
    public boolean sendMessage(String content, List<String> receivers) {
        if (!isLoggedIn) {
            System.out.println("Please log in first!");
            return false;
        }

        if (content == null || content.length() > 50) {
            System.out.println("Message content is invalid or exceeds the 50 character limit");
            return false;
        }

        try {
            Message message = new Message(
                    currentUser,
                    receivers,
                    Message.Operation.SEND_MESSAGE,
                    content
            );

            outputStream.writeObject(message);
            outputStream.flush();
            return true;
        } catch (IOException e) {
            System.out.println("Failed to send message:" + e.getMessage());
            return false;
        }
    }

    // 接收消息的方法
    public ChatMessage receiveOnlineMessage() {
        ChatMessage chatMessage = null;
        if (inputStream == null) {
            System.err.println("Input stream is not initialized");
            return null;
        }

        Message message = null;
        if (!isAlreadyRead()) {
            if(currentMessage!=null && currentMessage.getOperation() == Message.Operation.RECEIVE_MESSAGE) {
                try {
                    message = currentMessage.clone();
                } catch (CloneNotSupportedException e) {
                    throw new RuntimeException(e);
                }
                System.out.println("receive:"+ message.getContent());
                setAlreadyRead(true);
                chatMessage = new ChatMessage(UsefulToolsForClient.formatDate(message.getTimestamp())
                        , message.getSender(), message.getContent());
            }
        }

        return chatMessage;
    }

    // 关闭连接
    public void disconnect() {
        try {
            isFinishedSending = true;
            isFinishedReceiving = true;
            if (socket != null) socket.close();
            if (inputStream != null) inputStream.close();
            if (outputStream != null) outputStream.close();
        } catch (IOException e) {
            System.out.println("fail to disconnect: " + e.getMessage());
        }
    }

    public boolean checkUserExists(String username) {
        if (!isLoggedIn) {
            System.out.println("You should login first.");
            return false;
        }

        // 验证用户名格式
        if (!Pattern.matches("^[a-zA-Z0-9]{6,12}$", username)) {
            System.out.println("User name format is incorrect! Must be 6-12 letters or numbers.");
            return false;
        }

        try {
            // 构造用户查找消息
            List<String> receivers = new ArrayList<>();
            receivers.add("SERVER");

            Message searchMessage = new Message(
                    currentUser,           // 当前登录用户作为发送者
                    receivers,            // 接收者为服务器
                    Message.Operation.SEARCH_USER,  // 操作类型为搜索用户
                    username             // 要查找的用户名作为content
            );

            // 发送查找请求
            outputStream.writeObject(searchMessage);
            outputStream.flush();

            Message response = null;

            while (true){
                if(!isAlreadyRead()) {
                    // 等待服务器响应
                    if (currentMessage!=null && currentMessage.getOperation() == Message.Operation.SEARCH_USER){
                        try {
                            response = currentMessage.clone();
                        } catch (CloneNotSupportedException e) {
                            throw new RuntimeException(e);
                        }
                        setAlreadyRead(true);
                        break;
                    }

                }
            }

            // 处理响应

            if (response.getOperation() == Message.Operation.SEARCH_USER) {
                if (response.getContent().equals("exist")) {
                    System.out.println("user: " + username + " exists");
                    return true;
                } else if (response.getContent().equals("miss")) {
                    System.out.println("user: " + username + " doesn't exist");
                    return false;
                }else if (response.getContent().equals("error")) {
                    throw new RuntimeException("checkUserExists: " + response.getContent());
                }
            }

            System.out.println("An unexpected response type was received to search user ");
            return false;
        } catch (IOException e) {
            System.out.println("Failed to search user: " + e.getMessage());
            return false;
        }
    }

    public Map<String, ArrayList<String>> requestOfflineMessages() {
        if (!isLoggedIn) {
            System.out.println("You should login first.");
            return null;
        }

        try {
            // 构造离线消息请求
            Message requestMessage = new Message(
                    currentUser,
                    new ArrayList<>(),
                    Message.Operation.ASK_OFFLINE_MES,
                    null
            );

            // 发送请求
            outputStream.writeObject(requestMessage);
            outputStream.flush();
            Message response =null;
            // 接收响应
            while (response ==null) {
                response = (Message) (inputStream.readObject());
            }
            if (response.getOperation() == Message.Operation.ASK_OFFLINE_MES) {
                switch (response.getContent()) {
                    case "correct":
                        Map<String, ArrayList<String>> offlineMessages = response.getMesMap();
                        System.out.println("get offline messages successfully");
                        return offlineMessages;
                    case "null":
                        System.out.println("offline messages don't exist");
                        return new HashMap<>();
                    case "error":
                        System.out.println("fail to ask offline messages");
                        return null;
                }
            }
            System.out.println("unknown message error to ask offline messages");
            return null;

        } catch (IOException | ClassNotFoundException e) {
            System.out.println("fail to ask offline message: " + e.getMessage());
            return null;
        }
    }

//    //发送消息的线程类
//    private class SendMessageThread extends Thread {
//        private Message message;
//        private Socket socket;
//
//        public SendMessageThread(Message message, Socket socket) {
//            this.message = message;
//            this.socket = socket;
//        }
//
//        @Override
//        public void run() {
//            try {
//                // 获取Socket的输出流，并包装成ObjectOutputStream用于发送对象
//                ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
//                // 将Message对象写入输出流，发送到服务器端
//                outputStream.writeObject(message);
//                outputStream.flush();
////                System.out.println("消息已发送: " + message.getContent());
//            } catch (IOException e) {
//                e.printStackTrace();
//                System.err.println("error happen when sending message");
//            } finally {
//                try {
//                    // 关闭输出流和Socket连接（这里简单关闭，实际可能需要更复杂的逻辑判断）
//                    if (socket!= null) {
//                        socket.close();
//                    }
//                } catch (IOException ex) {
//                    ex.printStackTrace();
//                    System.err.println("error happen when closing socket");
//                }
//            }
//        }
//    }
//
//        // 接收消息的线程类
//        private class ReceiveMessageThread extends Thread {
//            @Override
//            public void run() {
//                try {
//                    while (true) {
//                        Message receivedMessage = (Message) inputStream.readObject();
//                        switch (receivedMessage.getOperation()) {
//                            case SEARCH_USER:
//                                break;
//                        }
//
//                    }
//                } catch (IOException | ClassNotFoundException e) {
//                    e.printStackTrace();
//                    // 尝试重新建立连接
//                    try {
//                        socket = new Socket(getServerAddress(), getServerPort());
//                        outputStream = new ObjectOutputStream(socket.getOutputStream());
//                        inputStream = new ObjectInputStream(socket.getInputStream());
//                    } catch (IOException ex) {
//                        ex.printStackTrace();
//                    }
//                } finally {
//                    isFinishedReceiving = true;
//                }
//            }
//        }
//
//        // 发送消息的方法，创建一个发送消息线程并启动
//        public void sendMessage (Message message,Socket socket) {
//            SendMessageThread sendThread = new SendMessageThread(message, socket);
//            sendThread.start();
//        }
//
//        // 接收消息的方法，创建一个接收消息线程并启动
//        public void receiveMessage () {
//            ReceiveMessageThread receiveThread = new ReceiveMessageThread();
//            receiveThread.start();
//        }

    public Map<String, Object> requestUsersStatus(ArrayList<String> usernames) {
        if (!isLoggedIn) {
            System.out.println("You should login first.");
            return null;
        }

        try {
        // 构造状态更新请求消息
            Message statusRequest = new Message(
                    currentUser,          // 当前登录用户作为发送者
                    new ArrayList<>(),    // 接收者为空，服务器会处理
                Message.Operation.UPDATE_STATUE,
                null                  // 内容为空，用elementList传递用户列表
        );

        // 设置要查询的用户名列表
        statusRequest.setElementList(usernames);

        // 发送状态更新请求
        outputStream.writeObject(statusRequest);
        outputStream.flush();

        Message response = null;
        while (true){
            if(!isAlreadyRead()) {
                // 等待服务器响应
                if (currentMessage!=null && currentMessage.getOperation() == Message.Operation.UPDATE_STATUE){
                    try {
                        response = currentMessage.clone();
                    } catch (CloneNotSupportedException e) {
                        throw new RuntimeException(e);
                    }
                    setAlreadyRead(true);
                    break;
                }
            }
        }

        // 处理响应
        if (response.getOperation() == Message.Operation.UPDATE_STATUE) {
            // 从响应中获取用户状态Map
            @SuppressWarnings("unchecked")
            Map<String, Object> userStatusMap =  response.getAttributes();
            if(userStatusMap==null){
                return new HashMap<>();
            }

            // 打印每个用户的状态 测试用
//            for (Map.Entry<String, Object> entry : userStatusMap.entrySet()) {
//                System.out.println("user: " + entry.getKey() + " - status: " + entry.getValue());
//            }
            return userStatusMap;
        } else {
            System.out.println("An unexpected response type was received to ask user status update");
            return null;
        }

        } catch (IOException e) {
            System.out.println("failed to ask user status update: " + e.getMessage());
            return null;
        }
    }

    public boolean requestUserStatus(String username) throws Exception {
        if (!isLoggedIn) {
            throw (new RuntimeException("You should login first."));
        }
        // 构造状态更新请求消息
        Message statusRequest = new Message(
                currentUser,
                null,
                Message.Operation.UPDATE_STATUE_SINGLE,
                username
        );
        // 发送状态更新请求
        outputStream.writeObject(statusRequest);
        outputStream.flush();

        // 等待服务器响应
        Message response = null;

        while (true){
            if(!isAlreadyRead()) {
                // 等待服务器响应
                if (currentMessage!=null && currentMessage.getOperation() == Message.Operation.UPDATE_STATUE_SINGLE){
                    try {
                        response = currentMessage.clone();
                    } catch (CloneNotSupportedException e) {
                        throw new RuntimeException(e);
                    }
                    setAlreadyRead(true);
                    break;
                }

            }
        }

        // 处理响应
        if (response.getOperation() == Message.Operation.UPDATE_STATUE_SINGLE) {
            if (response.getContent().equals("error")) {
                System.out.println("user do not exist");
            } else if (response.getContent().split("@@")[0].equals(username)) {
                if (response.getContent().split("@@")[1].equals("ONLINE")) {
                    return true;
                } else if (response.getContent().split("@@")[1].equals("OFFLINE")) {
                    return false;
                }
            }
        }
        throw (new RuntimeException("User don't exist or mes error"));
    }

    public void keepReadMessageFromServer(){
        try {
            while (isRunning){
                if(isAlreadyRead()){
                    Message mes= (Message) inputStream.readObject();
//                    System.out.println("received"+mes.getContent());
                    if(mes!=null){
                        currentMessage = mes;
                        setAlreadyRead(false);
                    }

                }
                Thread.sleep(100);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public ObjectInputStream getInputStream() {
        return inputStream;
    }

    public ObjectOutputStream getOutputStream() {
        return outputStream;
    }


    public Message getCurrentMessage() {
        return currentMessage;
    }

    public void setCurrentMessage(Message currentMessage) {
        Client.currentMessage = currentMessage;
    }

    public boolean isIsRunning() {
        return isRunning;
    }

    public void setIsRunning(boolean isRunning) {
        Client.isRunning = isRunning;
    }

    private static final Object lock = new Object(); // 锁对象

    public boolean isAlreadyRead() {
        synchronized (lock) {
            return alreadyRead;
        }
    }

    public void setAlreadyRead(boolean value) {
        synchronized (lock) {
            alreadyRead = value;
        }
    }

    public static void main(String[] args) throws Exception {
        String serverAddress = "127.0.0.1";  // 示例的本地回环地址，实际中可能是真实的服务器IP
        int serverPort = 8888;  // 示例端口号，要与服务器监听的端口一致
        Client client = new Client(serverAddress, serverPort);
        client.login("222222","222222");
        client.requestUserStatus("111111");

    }


}



