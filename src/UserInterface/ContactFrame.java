package UserInterface;

import Message.Message;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.util.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

class Contact implements Serializable {
    private ArrayList<ChatMessage> messageLog = new ArrayList<>();
    String name;
    boolean online;
    ImageIcon avatar;

//    boolean frameOpen;

    public Contact(String name, boolean online) {
        this.name = name;
        this.online = online;
        // 创建一个默认头像（colored circle with initials）
        this.avatar = createDefaultAvatar(name);
//        this.frameOpen = false;
    }

    public String getStatus(){
        if (online){
            return "Online";
        }else {
            return "Offline";
        }
    }


    public String getName(){
        return name;
    }
    private ImageIcon createDefaultAvatar(String name) {
        int size = 40;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = image.createGraphics();

        // 设置抗锯齿
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // 绘制圆形背景
        g2d.setColor(getColorForName(name));
        g2d.fillOval(0, 0, size, size);

        // 绘制首字母
        g2d.setColor(Color.WHITE);
        g2d.setFont(new Font("Arial", Font.BOLD, 20));
        String initial = name.substring(0, 1).toUpperCase();
        FontMetrics metrics = g2d.getFontMetrics();
        int x = (size - metrics.stringWidth(initial)) / 2;
        int y = ((size - metrics.getHeight()) / 2) + metrics.getAscent();
        g2d.drawString(initial, x, y);

        g2d.dispose();
        return new ImageIcon(image);
    }

    private Color getColorForName(String name) {
        // 根据名字生成固定的颜色
        int hash = name.hashCode();
        return new Color(
                (hash & 0xFF0000) >> 16,
                (hash & 0x00FF00) >> 8,
                hash & 0x0000FF
        );
    }
    public void addMessage(ChatMessage message){
        messageLog.add(message);
    }

    public ArrayList<ChatMessage> loadMessageLog(){
        return messageLog;
    }

}

class ContactListRenderer extends JPanel implements ListCellRenderer<Contact> {
    private JLabel avatarLabel = new JLabel();
    private JLabel nameLabel = new JLabel();
    private JLabel statusLabel = new JLabel();
    private JPanel textPanel = new JPanel();

    public ContactListRenderer() {
        setLayout(new BorderLayout(10, 0));
        textPanel.setLayout(new GridLayout(2, 1));
        setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

        textPanel.add(nameLabel);
        textPanel.add(statusLabel);

        add(avatarLabel, BorderLayout.WEST);
        add(textPanel, BorderLayout.CENTER);

        setOpaque(true);
        textPanel.setOpaque(false);
    }

    @Override
    public Component getListCellRendererComponent(
            JList<? extends Contact> list, Contact contact, int index,
            boolean isSelected, boolean cellHasFocus) {

        avatarLabel.setIcon(contact.avatar);
        nameLabel.setText(contact.name);
        nameLabel.setFont(new Font("Arial", Font.BOLD, 14));

        statusLabel.setText(contact.getStatus());
        statusLabel.setFont(new Font("Arial", Font.PLAIN, 12));
        statusLabel.setForeground(contact.online ? new Color(46, 139, 87) : Color.GRAY);

        Color background = isSelected ? list.getSelectionBackground() : list.getBackground();
        Color foreground = isSelected ? list.getSelectionForeground() : list.getForeground();

        setBackground(background);
        nameLabel.setForeground(foreground);

        return this;
    }
}

public class ContactFrame extends JFrame {
    //决定了消息界面的新消息队列
    public static Map<String,Vector<ChatMessage>> messageFrameUpdateMap=new HashMap<>();
    private String username;
    private DefaultListModel<Contact> contactsModel;
    Client client;


    public ContactFrame(String username,Client passClient){

        client = passClient;

        this.username = username;
        setTitle("Talker - Contact List - " + username);
        setSize(300, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);



        JPanel searchPanel = new JPanel(new BorderLayout());
        JTextField searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "Search for a contact...");
        JButton searchButton = new JButton("Search");

        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.add(searchButton, BorderLayout.EAST);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 初始化联系人列表
        contactsModel = new DefaultListModel<>();

        //真正的加载段
        for (int i = 0; i < ChatApp.contactArrayList.size(); i++){
            // 此处的所有Contact都已在Login加载完用户登录状态以及离线的消息
             contactsModel.addElement(ChatApp.contactArrayList.get(i));
        }

        //模拟段,结束测试后删除。
//        contactsModel.addElement(new Contact("Alice", true));
//        contactsModel.addElement(new Contact("Bob", true));
//        contactsModel.addElement(new Contact("Charlie", false));
//        contactsModel.addElement(new Contact("David", true));
//        contactsModel.addElement(new Contact("Eve", false));

        JList<Contact> contactList = new JList<>(contactsModel);
        contactList.setCellRenderer(new ContactListRenderer());
        contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 设置固定的单元格高度
        contactList.setFixedCellHeight(60);

        // 添加选择监听器
        contactList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Contact selectedContact = contactList.getSelectedValue();
                if (selectedContact != null&&!messageFrameUpdateMap.containsKey(selectedContact.name)) {
                    new MessageFrame(username, selectedContact.name,client);
                }
                SwingUtilities.invokeLater(() -> contactList.clearSelection()); // 延迟清除选中状态
            }
        });

        // 创建一个滚动面板，并设置边框
        JScrollPane scrollPane = new JScrollPane(contactList);
        scrollPane.setBorder(null);  // 移除滚动面板的边框

        // 使用BorderLayout来组织界面
        setLayout(new BorderLayout());
        add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // 设置窗口最小尺寸
        setMinimumSize(new Dimension(250, 400));

        setVisible(true);

        //client.checkUserExists("111111");

        // 启动后台线程监听服务器消息
        startListeningForServerUpdates();

        //监听窗口关闭事件
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                onWindowClosing();
            }
        });


        //搜索按钮逻辑
        searchButton.addActionListener(e -> {
            String searchUsername = searchField.getText().trim();

            if (!searchUsername.isEmpty()) {
                // 列表内用户查重以避免重复
                boolean contactExists = ChatApp.contactArrayList.stream()
                        .anyMatch(contact -> contact.name.equalsIgnoreCase(searchUsername));

                if (contactExists) {
                    JOptionPane.showMessageDialog(
                            this,
                            "The contact '" + searchUsername + "' is already in your list.",
                            "Search Failed",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return;
                }else if (searchUsername.equals(username)){
                    JOptionPane.showMessageDialog(
                            this,
                            "The contact cannot be yourself.",
                            "Search Failed",
                            JOptionPane.INFORMATION_MESSAGE
                    );
                    return;
                }

                // 模拟发送一个搜索请求
                Contact searchResult = searchForContactOnServer(searchUsername);
                if (searchResult != null) {
                    // 更新GUI
                    ChatApp.contactArrayList.add(searchResult);
                    contactsModel.addElement(searchResult);

                    //打开一个对应的Message Frame
                    new MessageFrame(username, searchResult.name, client);
                } else {
                    //展示一个错误框以示未找到
                    JOptionPane.showMessageDialog(
                            this,
                            "Invalid input or the contact '" + searchUsername + "' does not exist.",
                            "Search Failed",
                            JOptionPane.WARNING_MESSAGE
                    );
                }
            }
        });

    }

    //监听服务器状态线程
    private void startListeningForServerUpdates() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                while (true) {
                    // 模拟从服务器接收联系人状态更新
                    // 本句不需要修改，直接搜索跳转至该方法的位置

                    ChatMessage newChatMes = client.receiveOnlineMessage();
                    if(newChatMes!=null){
                        if(messageFrameUpdateMap.containsKey(newChatMes.getSenderName())){
                            messageFrameUpdateMap.get(newChatMes.getSenderName()).add(newChatMes);
                        }
                        boolean isExist = false;
                        for (int i = 0; i < ChatApp.contactArrayList.size(); i++ ){
                            if (newChatMes.getSenderName().equals(ChatApp.contactArrayList.get(i).name)){
                                isExist = true;
                                ChatApp.contactArrayList.get(i).addMessage(newChatMes);
                                break;
                            }
                        }
                        if(!isExist){
                            // 如果联系人不存在，添加新联系人到面板
                            Contact newContact = new Contact(newChatMes.getSenderName(), true);
                            ChatApp.contactArrayList.add(newContact);
                            newContact.addMessage(newChatMes);
                            contactsModel.addElement(newContact);
                        }
                    }
                    //线程用于更新状态
                    SwingUtilities.invokeLater(() -> updateContactStatus());

                    // 控制监听的频率
                    Thread.sleep(500); // 每0.5秒检查一次
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    // 从服务器接收的搜索联系人结果的方法
    // TODO 此处需要适配Message类,此处已完成
    private Contact searchForContactOnServer(String username) {
        try {
            if (client.checkUserExists(username)) {
                return new Contact(username, client.requestUserStatus(username));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null; // 用户不存在
    }

    // 更新联系人状态
    private void updateContactStatus() {
        ArrayList<String> usernameList = new ArrayList<>();
        for(Contact contact: ChatApp.contactArrayList){
            usernameList.add(contact.name);
        }
        Map<String, Object> update = client.requestUsersStatus(usernameList);

        for (int i = 0; i < contactsModel.size(); i++) {
            ChatApp.contactArrayList.get(i).online =
                    (boolean)update.get(ChatApp.contactArrayList.get(i).name);
            contactsModel.set(i, ChatApp.contactArrayList.get(i));  // 更新联系人

        }

    }

    //用于保存数据
    private void onWindowClosing() {
        disconnectToServer();
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream("dat/" + username + "/contactAccount.dat"))) {
            oos.writeObject(ChatApp.contactArrayList);
            oos.flush();
            oos.close();
            System.out.println("Contacts saved successfully.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    private void disconnectToServer(){
        client.disconnect();
    }


    // 模拟服务器发送的状态更新
    // TODO 需要适配Message类，此处完成与上方一致
    private static class ContactStatusUpdate {
        String name;
        boolean online;
        ChatMessage newMessage;

        public ContactStatusUpdate(String name, boolean online, ChatMessage newMessage) {
            this.name = name;
            this.online = online;
            this.newMessage = newMessage;
        }
    }
}


//优化前的代码：未加入监听器以接收服务器消息
    /*
class ContactFrame extends JFrame {
    private String username;
    private DefaultListModel<Contact> contactsModel;




    public ContactFrame(String username) {
        this.username = username;
        setTitle("Talker - Contact List - " + username);
        setSize(300, 600);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 创建一个带有搜索框的面板
        JPanel searchPanel = new JPanel(new BorderLayout());
        JTextField searchField = new JTextField();
        searchField.putClientProperty("JTextField.placeholderText", "搜索联系人...");
        searchPanel.add(searchField, BorderLayout.CENTER);
        searchPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        // 初始化联系人列表
        contactsModel = new DefaultListModel<>();
        contactsModel.addElement(new Contact("Alice", true, "Online"));
        contactsModel.addElement(new Contact("Bob", true, "Offline"));
        contactsModel.addElement(new Contact("Charlie", false, "Offline"));
        contactsModel.addElement(new Contact("David", true, "Online"));
        contactsModel.addElement(new Contact("Eve", false, "Offline"));

        JList<Contact> contactList = new JList<>(contactsModel);
        contactList.setCellRenderer(new ContactListRenderer());
        contactList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        // 设置固定的单元格高度
        contactList.setFixedCellHeight(60);

        // 添加选择监听器
        contactList.addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                Contact selectedContact = contactList.getSelectedValue();
                if (selectedContact != null) {
                    new MessageFrame(username, selectedContact.name);
                }
            }
        });

        // 创建一个滚动面板，并设置边框
        JScrollPane scrollPane = new JScrollPane(contactList);
        scrollPane.setBorder(null);  // 移除滚动面板的边框

        // 使用BorderLayout来组织界面
        setLayout(new BorderLayout());
        //add(searchPanel, BorderLayout.NORTH);
        add(scrollPane, BorderLayout.CENTER);

        // 设置窗口最小尺寸
        setMinimumSize(new Dimension(250, 400));

        setVisible(true);
    }
}*/
