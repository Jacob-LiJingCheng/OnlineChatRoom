package UserInterface;

import javax.swing.*;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Vector;

class MessageFrame extends JFrame {
    private String currentUser;
    private String contact;
    private JTextPane chatArea;  // 使用JTextPane替代JTextArea以支持富文本
    private JTextField messageField;
    private final Color myMessageColor = new Color(220, 248, 198);  // 自己发送的消息背景色
    private final Color otherMessageColor = new Color(255, 255, 255);  // 对方消息背景色
    private final Font messageFont = new Font("", Font.PLAIN, 14);
    private final Font timeFont = new Font("", Font.PLAIN, 12);
    //正在运行
    boolean isRunning = true;

    Client client;

    public MessageFrame(String currentUser, String contact,Client passClient) {
        this.currentUser = currentUser;
        this.contact = contact;
        client = passClient;
        ContactFrame.messageFrameUpdateMap.put(contact,new Vector<>());
        initializeFrame();
        setupComponents();
        setVisible(true);
//        for(int i = 0; i < ChatApp.contactArrayList.size(); i++){
//            if(ChatApp.contactArrayList.get(i).name.equals(currentUser)){
//                ChatApp.contactArrayList.get(i).frameOpen = true;
//                break;
//            }
//        }

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                isRunning=false;
                ContactFrame.messageFrameUpdateMap.remove(contact);

//                for (int i = 0; i < ChatApp.contactArrayList.size(); i++){
//                    if (ChatApp.contactArrayList.get(i).name.equals(contact)){
//                        ChatApp.contactArrayList.get(i).frameOpen = false;
//                        break;
//                    }
//                }
            }
        });
    }

    private void initializeFrame() {
        setTitle("Talk to " + contact);
        setSize(600, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(0, 0));
        getContentPane().setBackground(new Color(245, 245, 245));
    }

    private void setupComponents() {
        // 顶部面板 - 显示联系人信息
        JPanel headerPanel = createHeaderPanel();
        add(headerPanel, BorderLayout.NORTH);

        // 聊天区域
        chatArea = createChatArea();
        JScrollPane scrollPane = new JScrollPane(chatArea);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);
        add(scrollPane, BorderLayout.CENTER);

        // 底部输入区域
        JPanel inputPanel = createInputPanel();
        add(inputPanel, BorderLayout.SOUTH);

        // 初始聊天记录
        appendSystemMessage("Talking Beginning with " + contact);

        // 加载历史记录
        loadHistoryMessages();

        // 启动服务器监听
        startServerMessageListener();
    }

    private JPanel createHeaderPanel() {
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(Color.WHITE);
        headerPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JLabel contactLabel = new JLabel(contact);
        contactLabel.setFont(new Font("", Font.BOLD, 16));
        headerPanel.add(contactLabel, BorderLayout.CENTER);

        return headerPanel;
    }

    private JTextPane createChatArea() {
        JTextPane textPane = new JTextPane();
        textPane.setEditable(false);
        textPane.setBackground(new Color(245, 245, 245));
        textPane.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        textPane.putClientProperty(JEditorPane.HONOR_DISPLAY_PROPERTIES, Boolean.TRUE);
        return textPane;
    }

    private JPanel createInputPanel() {
        JPanel inputPanel = new JPanel(new BorderLayout(10, 0));
        inputPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(230, 230, 230)),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));
        inputPanel.setBackground(Color.WHITE);

        // 消息输入框
        messageField = new JTextField();
        messageField.setFont(messageFont);
        messageField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(200, 200, 200), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)
        ));

        // 发送按钮
        JButton sendButton = createSendButton();

        // 添加回车发送功能
        messageField.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    sendMessage();
                }
            }
        });

        inputPanel.add(messageField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);

        return inputPanel;
    }

    private JButton createSendButton() {
        JButton sendButton = new JButton("Send");
        sendButton.setFont(new Font("", Font.BOLD, 14));
        sendButton.setForeground(Color.WHITE);
        sendButton.setBackground(new Color(0, 150, 136));
        sendButton.setBorder(BorderFactory.createEmptyBorder(8, 20, 8, 20));
        sendButton.setFocusPainted(false);
        sendButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        // 添加悬停效果
        sendButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                sendButton.setBackground(new Color(0, 137, 123));
            }
            public void mouseExited(MouseEvent e) {
                sendButton.setBackground(new Color(0, 150, 136));
            }
        });

        sendButton.addActionListener(e -> sendMessage());
        return sendButton;
    }
    //发送消息方法
    private void sendMessage() {
        String message = messageField.getText().trim();
        if (!message.isEmpty()) {
            String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            appendMessage(currentUser, message, time, true);
            ChatMessage newChatMessage = new ChatMessage(time, currentUser, message);
            for(int i = 0; i < ChatApp.contactArrayList.size(); i++){
                if(ChatApp.contactArrayList.get(i).name.equals(contact)){
                    ChatApp.contactArrayList.get(i).addMessage(newChatMessage);
                    break;
                }
            }
            sendMessageToServer(time, contact, message);
            messageField.setText("");
        }
    }

    //重要方法：向消息框发送消息

    private void appendMessage(String sender, String message, String time, boolean isCurrentUser) {
        if (!SwingUtilities.isEventDispatchThread()) {
            SwingUtilities.invokeLater(() -> appendMessage(sender, message, time, isCurrentUser));
            return;
        }
        StyledDocument doc = chatArea.getStyledDocument();

        // 创建消息气泡样式
        Style bubbleStyle = chatArea.addStyle("BubbleStyle", null);
        StyleConstants.setBackground(bubbleStyle, isCurrentUser ? myMessageColor : otherMessageColor);
        StyleConstants.setFontFamily(bubbleStyle, messageFont.getFamily());
        StyleConstants.setFontSize(bubbleStyle, messageFont.getSize());

        // 创建时间样式
        Style timeStyle = chatArea.addStyle("TimeStyle", null);
        StyleConstants.setFontFamily(timeStyle, timeFont.getFamily());
        StyleConstants.setFontSize(timeStyle, timeFont.getSize());
        StyleConstants.setForeground(timeStyle, Color.GRAY);

        try {
            // 添加换行
            doc.insertString(doc.getLength(), "\n", null);

            // 添加发送者名称和时间
            String header = String.format("%s  %s\n", sender, time);
            doc.insertString(doc.getLength(), header, timeStyle);

            // 添加消息内容
            doc.insertString(doc.getLength(), message + "\n", bubbleStyle);

            // 自动滚动到底部
            SwingUtilities.invokeLater(() ->
                    chatArea.setCaretPosition(doc.getLength())
            );

        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    private void appendSystemMessage(String message) {
        StyledDocument doc = chatArea.getStyledDocument();
        Style systemStyle = chatArea.addStyle("SystemStyle", null);
        StyleConstants.setForeground(systemStyle, Color.GRAY);
        StyleConstants.setItalic(systemStyle, true);
        StyleConstants.setAlignment(systemStyle, StyleConstants.ALIGN_CENTER);

        try {
            doc.insertString(doc.getLength(), "\n" + message + "\n", systemStyle);
        } catch (BadLocationException e) {
            e.printStackTrace();
        }
    }

    //TODO 此处需要适配Message类，此处已完成
    private void sendMessageToServer(String time, String to, String message) {
        ArrayList<String> arrayList = new ArrayList<>();
        arrayList.add(to);
        boolean isSuccess = client.sendMessage(message,arrayList);
        System.out.println("Send to: " + to + ": " + message + " at " + time);

        if (!isSuccess) {
            //提示未成功发出消息
            String timeNew = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"));
            appendMessage(contact, "Received your message: " + message, timeNew, false);
        }



    }
    private void loadHistoryMessages() {

        ArrayList<ChatMessage> messageLog = new ArrayList<>();
        for(int i = 0; i < ChatApp.contactArrayList.size(); i++){
            if (ChatApp.contactArrayList.get(i).name.equals(contact)){
                messageLog.addAll(ChatApp.contactArrayList.get(i).loadMessageLog());
                break;
            }
        }

        if (!messageLog.isEmpty()) {
            for (ChatMessage msg : messageLog) {
                appendMessage(msg.getSenderName(), msg.getMessageInString(), msg.getSendTime(), msg.getSenderName() == currentUser);
            }
        }
    }
    //监听服务器消息
    //TODO 此处需要添加来自Message的方法,此处已完成

    private void startServerMessageListener() {
        new Thread(() -> {
            while (isRunning) {
                try {
                    ChatMessage newMessage = null;
                    if(!ContactFrame.messageFrameUpdateMap.get(contact).isEmpty()){
                        newMessage=ContactFrame.messageFrameUpdateMap.get(contact).removeFirst();
                    }

                    if (newMessage != null) {
                        // 保存消息的最终引用，以便在lambda表达式中使用
                        final ChatMessage finalMessage = newMessage;

                        // 使用SwingUtilities.invokeLater确保UI更新在EDT线程中进行
                        SwingUtilities.invokeLater(() -> {
                            appendMessage(
                                    finalMessage.getSenderName(),
                                    finalMessage.getMessageInString(),
                                    finalMessage.getSendTime(),
                                    finalMessage.getSenderName().equals(currentUser)  // 使用equals而不是==
                            );
                        });

                        // 更新消息日志
//                        updateMessageLog(newMessage);
                    }
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }).start();  // 给线程一个有意义的名字便于调试
    }

//    private void updateMessageLog(ChatMessage message) {
//        for (int i = 0; i < ChatApp.contactArrayList.size(); i++) {
//            Contact currentContact = ChatApp.contactArrayList.get(i);
//            if (currentContact.name.equals(contact)) {  // 使用equals进行字符串比较
//                synchronized (currentContact.loadMessageLog()) {  // 同步访问消息日志
//                    currentContact.loadMessageLog().add(message);
//                }
//                break;  // 找到匹配的联系人后退出循环
//            }
//        }
//    }
}

//旧代码，可安全删除
/*
class MessageFrame extends JFrame {

    private String contactName;
    private JTextArea messageArea;
    private JTextField inputField;

    public MessageFrame(String contactName) {
        this.contactName = contactName;

        setTitle("Chat - To " + contactName);
        setSize(400, 500);
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 消息显示区域
        messageArea = new JTextArea();
        messageArea.setEditable(false); // 禁止编辑
        JScrollPane messageScrollPane = new JScrollPane(messageArea);
        add(messageScrollPane, BorderLayout.CENTER);

        // 消息输入区域
        JPanel inputPanel = new JPanel(new BorderLayout());
        inputField = new JTextField();
        JButton sendButton = new JButton("Send");

        inputPanel.add(inputField, BorderLayout.CENTER);
        inputPanel.add(sendButton, BorderLayout.EAST);
        add(inputPanel, BorderLayout.SOUTH);

        // 发送按钮功能
        sendButton.addActionListener(e -> sendMessage());

        setVisible(true);
    }

    // 发送消息
    private void sendMessage() {
        String message = inputField.getText().trim();
        if (!message.isEmpty()) {
            messageArea.append("You: " + message + "\n");
            inputField.setText("");
            // 模拟发送到服务端，或添加实际逻辑
            //receiveMessage(contactName + ": " + message); // 模拟服务端响应
        }
    }

    // 接收消息（模拟服务端回复）
    private void receiveMessage(String message) {
        messageArea.append(contactName + ": " + message + "\n");
    }
}
*/
