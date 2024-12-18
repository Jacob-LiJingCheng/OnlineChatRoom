package UserInterface;

import javax.swing.*;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;

// 主类
public class ChatApp {
    public static Vector<Contact> contactArrayList = new Vector<>();

    public static void main(String[] args) {
        String serverAddress = "127.0.0.1";  // 示例的本地回环地址，实际中可能是真实的服务器IP
        int serverPort = 8888;  // 示例端口号，要与服务器监听的端口一致
        Client client = new Client(serverAddress, serverPort);
        SwingUtilities.invokeLater(() -> new LoginFrame(client));
    }

}
