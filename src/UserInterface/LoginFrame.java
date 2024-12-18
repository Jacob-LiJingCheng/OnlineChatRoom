package UserInterface;

import Message.Message;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// 登录界面
class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;
    Client client;

    public LoginFrame(Client passClient) {
        // 设置窗口基本属性
        client = passClient;

        setTitle("Login System");
        setSize(400, 300);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);

        // 使用 BorderLayout 作为主要布局
        setLayout(new BorderLayout());

        // 创建顶部标题面板
        JPanel titlePanel = createTitlePanel();
        add(titlePanel, BorderLayout.NORTH);

        // 创建中间内容面板
        JPanel contentPanel = createContentPanel();
        add(contentPanel, BorderLayout.CENTER);

        // 创建底部按钮面板
        JPanel buttonPanel = createButtonPanel();
        add(buttonPanel, BorderLayout.SOUTH);
        setVisible(true);
    }
    private JPanel createTitlePanel() {
        JPanel panel = new JPanel();
        panel.setBackground(new Color(51, 122, 183));
        panel.setPreferredSize(new Dimension(400, 60));
        panel.setLayout(new FlowLayout(FlowLayout.CENTER));

        JLabel titleLabel = new JLabel("Welcome to Talker");
        titleLabel.setFont(new Font("",Font.BOLD, 28));
        titleLabel.setForeground(Color.WHITE);
        panel.add(titleLabel);

        return panel;
    }

    private JPanel createContentPanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(new EmptyBorder(20, 40, 20, 40));
        GridBagConstraints gbc = new GridBagConstraints();

        // 用户名输入框
        JLabel userLabel = new JLabel("Username:");
        usernameField = new JTextField(15);
        styleTextField(usernameField);

        // 密码输入框
        JLabel passLabel = new JLabel("password:");
        passwordField = new JPasswordField(15);
        styleTextField(passwordField);

        // 设置组件位置
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        gbc.gridx = 0; gbc.gridy = 0;
        panel.add(userLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 0;
        panel.add(usernameField, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        panel.add(passLabel, gbc);

        gbc.gridx = 1; gbc.gridy = 1;
        panel.add(passwordField, gbc);

        return panel;
    }

    private JPanel createButtonPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        panel.setBorder(new EmptyBorder(0, 0, 20, 0));

        // 登录按钮及其逻辑
        JButton loginButton = new JButton("Login");
        styleButton(loginButton);
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            if (username.isEmpty()){
                UIManager.put("OptionPane.okButtonText", "Yes");
                JOptionPane.showMessageDialog(this, "Username cannot be empty！", "Error",
                        JOptionPane.INFORMATION_MESSAGE);
                UIManager.put("OptionPane.okButtonText", null);
            }
            else if (loginToServer(username, password)) {
                UIManager.put("OptionPane.okButtonText", "Yes");
                JOptionPane.showMessageDialog(this, "Success in login！", "Tips",
                        JOptionPane.INFORMATION_MESSAGE);
                UIManager.put("OptionPane.okButtonText", null);

                String filePath ="dat/" + username + "/contactAccount.dat"; // 路径包含新文件夹

                File file = new File(filePath); File parentDir = file.getParentFile(); // 获取父目录

                if (!parentDir.exists()) {
                    boolean dirsCreated = parentDir.mkdirs(); // 创建所有缺失的父目录
                    if (dirsCreated) {
                        System.out.println("文件夹已创建：" + parentDir.getAbsolutePath());
                    } else {
                        System.err.println("文件夹创建失败！");
                        return; // 如果无法创建文件夹，直接退出
                    }
                }

                try {
                    if (file.createNewFile()) {
                        System.out.println("文件已创建：" + file.getAbsolutePath());
                    } else {
                        System.out.println("文件已存在：" + file.getAbsolutePath());
                    }
                } catch (IOException e1) {
                    System.err.println("文件创建失败：" + e1.getMessage());
                }

                try (FileInputStream fis = new FileInputStream(filePath);
                     ObjectInputStream ois = new ObjectInputStream(fis)) {
                    Vector<Contact> oldData = (Vector<Contact>) ois.readObject();
                    ChatApp.contactArrayList = oldData;
                } catch (Exception e1) {
                    System.out.println(e1.getMessage());
                    System.out.println("file not found");;
                }

                //加载所有离线时接收到的消息 还未优化成监听线程返回Message, 因此和下面的不能交换次序
                Map<String, ArrayList<ChatMessage>> contentNewMessageMap = askForContactNewMessageToServer(username);
                for (Map.Entry<String,ArrayList<ChatMessage>> entry : contentNewMessageMap.entrySet()) {
                    for (int i = 0; i < ChatApp.contactArrayList.size(); i++){
                        if(ChatApp.contactArrayList.get(i).name.equals(entry.getKey())){
                            for (ChatMessage newMessage: entry.getValue()){
                                ChatApp.contactArrayList.get(i).addMessage(newMessage);
                            }
                            break;
                        }
                    }
                }

                //开始监听服务端的消息
                ExecutorService executor = Executors.newSingleThreadExecutor();
                executor.submit(()->client.keepReadMessageFromServer());

                //更新所有过去已有联系的用户的状态
                Map<String,Object> contentStatueMap = askForContactStatueToServer();
                for (Map.Entry<String,Object> entry : contentStatueMap.entrySet()) {
                    for (int i = 0; i < ChatApp.contactArrayList.size(); i++) {
                        if(ChatApp.contactArrayList.get(i).name.equals(entry.getKey())){
                            ChatApp.contactArrayList.get(i).online = (boolean)entry.getValue();
                            break;
                        }
                    }
                }
                //绘制用户显示
                new ContactFrame(username, client);
                dispose();
            } else {
                UIManager.put("OptionPane.okButtonText", "Yes");
                JOptionPane.showMessageDialog(this, "Fail to login: wrong username or password！",
                        "Error", JOptionPane.ERROR_MESSAGE);
                UIManager.put("OptionPane.okButtonText", null);
            }
        });

        // 注册按钮
        JButton registerButton = new JButton("Register");
        styleButton(registerButton);
        registerButton.addActionListener(e -> {
            RegisterDialog registerDialog = new RegisterDialog(this, client);
            registerDialog.setVisible(true);
        });

        //此段模拟已实现，可删除该段代码
        /* registerButton.addActionListener(e -> {
            //  添加注册功能
            UIManager.put("OptionPane.okButtonText", "Yes");
            JOptionPane.showMessageDialog(this, "Register is waiting for opening!", "Tips",
                    JOptionPane.INFORMATION_MESSAGE);
            UIManager.put("OptionPane.okButtonText", null);
        });*/

        panel.add(loginButton);
        panel.add(registerButton);

        return panel;
    }

    private void styleButton(JButton button) {
        button.setPreferredSize(new Dimension(100, 35));
        button.setFocusPainted(false);
        button.setBackground(new Color(51, 122, 183));
        button.setForeground(Color.WHITE);
        button.setBorder(BorderFactory.createRaisedBevelBorder());

        // 添加鼠标悬停效果
        button.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(40, 96, 144));
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                button.setBackground(new Color(51, 122, 183));
            }
        });
    }

    private void styleTextField(JTextField textField) {
        textField.setPreferredSize(new Dimension(200, 30));
        textField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(204, 204, 204)),
                BorderFactory.createEmptyBorder(5, 10, 5, 10)
        ));
    }

    //TODO 此处需要适配Message类，此处已完成
    private boolean loginToServer(String username, String password) {
        // 登录验证逻辑
        return client.login(username, password);
    }


    //TODO 此处需要适配
    private Map<String,Object> askForContactStatueToServer(){
        ArrayList<String> listAfter = new ArrayList<>();

        for (int i = 0; i < ChatApp.contactArrayList.size(); i++){
            listAfter.add(ChatApp.contactArrayList.get(i).name);
        }
        if(listAfter.isEmpty()){
            return new HashMap<>();
        }

        return client.requestUsersStatus(listAfter);

    }

    //TODO 此处需要适配
    private Map<String,ArrayList<ChatMessage>> askForContactNewMessageToServer(String username){
        // 这里添加实际的获取用户信息键值对的逻辑
        Map<String,ArrayList<String>> mapBefore = client.requestOfflineMessages();

        Map<String,ArrayList<ChatMessage>> mapAfter = new HashMap<>();
        String time,message;
        if(mapBefore == null){
            mapBefore = new HashMap<String, ArrayList<String>>();
        }
        for(Map.Entry<String, ArrayList<String>> entry : mapBefore.entrySet()) {
            ArrayList<ChatMessage> arrayListAfter = new ArrayList<>();
            for (int i = 0; i < entry.getValue().size();i++){
                String[] parts =entry.getValue().get(i).split("@@");
                if (parts.length == 2) {
                     time = parts[0];
                     message = parts[1];
                     ChatMessage chatMessage = new ChatMessage(time, entry.getKey(), message);
                     arrayListAfter.add(chatMessage);
                }else{time = "";
                      message = "";
                }
            }
            mapAfter.put(entry.getKey(),arrayListAfter);
        }
        return mapAfter;
    }

}

// 注册功能对话框类
class RegisterDialog extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JPasswordField confirmPasswordField;
    Client client;

    public RegisterDialog(JFrame parent, Client passClient) {
        super(parent, "Register", true);

        client = passClient;

        // 设置对话框布局
        setSize(400, 300);
        setLocationRelativeTo(parent);
        setLayout(new BorderLayout());

        // 创建内容面板
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(new EmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;

        // 用户名输入框
        JLabel userLabel = new JLabel("Username:");
        usernameField = new JTextField(15);

        gbc.gridx = 0; gbc.gridy = 0;
        contentPanel.add(userLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 0;
        contentPanel.add(usernameField, gbc);

        // 密码输入框
        JLabel passLabel = new JLabel("Password:");
        passwordField = new JPasswordField(15);

        gbc.gridx = 0; gbc.gridy = 1;
        contentPanel.add(passLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 1;
        contentPanel.add(passwordField, gbc);

        // 确认密码输入框
        JLabel confirmPassLabel = new JLabel("Confirm Password:");
        confirmPasswordField = new JPasswordField(15);

        gbc.gridx = 0; gbc.gridy = 2;
        contentPanel.add(confirmPassLabel, gbc);
        gbc.gridx = 1; gbc.gridy = 2;
        contentPanel.add(confirmPasswordField, gbc);

        add(contentPanel, BorderLayout.CENTER);

        // 创建按钮面板
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        JButton registerButton = new JButton("Register");
        JButton cancelButton = new JButton("Cancel");

        // 注册按钮动作监听器
        registerButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            String confirmPassword = new String(confirmPasswordField.getPassword());

            if (username.isEmpty() || password.isEmpty()) {
                UIManager.put("OptionPane.okButtonText", "Yes");
                JOptionPane.showMessageDialog(this, "Username and password cannot be empty!", "Error",
                        JOptionPane.ERROR_MESSAGE);
                UIManager.put("OptionPane.okButtonText", null);
            } else if (!password.equals(confirmPassword)) {
                UIManager.put("OptionPane.okButtonText", "Yes");
                JOptionPane.showMessageDialog(this, "Passwords do not match!", "Error",
                        JOptionPane.ERROR_MESSAGE);
                UIManager.put("OptionPane.okButtonText", null);
            } else {
                try {
                    //此处连接服务端并注册
                    String problem = registerToServer(username,password);
                    if(problem.equals("exist")){
                        // 注册失败,重复
                        UIManager.put("OptionPane.okButtonText", "Yes");
                        JOptionPane.showMessageDialog(this, "Registration failed! Username may already exist.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        UIManager.put("OptionPane.okButtonText", null);
                    } else if (problem.equals("format")){
                        //注册失败，不合法
                        UIManager.put("OptionPane.okButtonText", "Yes");
                        JOptionPane.showMessageDialog(this, "Registration failed! The username and password must be a 6-12 digit alphanumeric combination.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        UIManager.put("OptionPane.okButtonText", null);
                    } else if(problem.equals("Correct")){
                        // 注册成功
                        UIManager.put("OptionPane.okButtonText", "Yes");
                        JOptionPane.showMessageDialog(this, "Registration successful!", "Info",
                                JOptionPane.INFORMATION_MESSAGE);
                        UIManager.put("OptionPane.okButtonText",null);
                        dispose();

                    }else {
                        UIManager.put("OptionPane.okButtonText", "Yes");
                        JOptionPane.showMessageDialog(this, "Registration failed! Unexpected error.",
                                "Error", JOptionPane.ERROR_MESSAGE);
                        UIManager.put("OptionPane.okButtonText", null);
                    }
                }catch (Exception e1){
                    UIManager.put("OptionPane.okButtonText", "Yes");
                    JOptionPane.showMessageDialog(this, e1.getMessage());
                    UIManager.put("OptionPane.okButtonText", null);
                }

            }
        });

        // 取消按钮动作监听器
        cancelButton.addActionListener(e -> dispose());

        buttonPanel.add(registerButton);
        buttonPanel.add(cancelButton);

        add(buttonPanel, BorderLayout.SOUTH);
    }

    //TODO 此处需要适配Message类,此处已完成
    public String registerToServer(String username, String password) {
        Message message = client.register(username,password);

        // 使用 split 方法根据 @@ 分割
        String[] parts = message.getContent().split("@@");

        // 确保字符串中确实包含分隔符 @@
        if (parts.length == 2) {
            String problem = parts[1];
            return problem;
        }else if(parts.length == 1) {return "Correct";}
        else return null;

    }

}

//旧UI代码，可安全删除
/*class LoginFrame extends JFrame {
    private JTextField usernameField;
    private JPasswordField passwordField;

    public LoginFrame() {
        setTitle("Login To Talker");
        setSize(300, 200);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // 使用 GridBagLayout 布局
        setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(10, 10, 10, 10); // 设置组件之间的间距


        // 添加用户名标签和文本框
        gbc.gridx = 0;
        gbc.gridy = 0;
        add(new JLabel("UserName:"), gbc);

        gbc.gridx = 1;
        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(150, 25)); // 宽度150，高度25
        add(usernameField, gbc);

        // 添加密码标签和文本框
        gbc.gridx = 0;
        gbc.gridy = 1;
        add(new JLabel("Password:"), gbc);

        gbc.gridx = 1;
        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(150, 25)); // 宽度150，高度25
        add(passwordField, gbc);

        // 添加登录按钮
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2; // 按钮占两列
        gbc.weightx = 0; // 按钮不需要扩展
        gbc.anchor = GridBagConstraints.CENTER; // 居中对齐
        JButton loginButton = new JButton("Login");
        loginButton.addActionListener(e -> {
            String username = usernameField.getText();
            String password = new String(passwordField.getPassword());
            try {
                if (login(username, password)) {
                    new ContactFrame(username);
                    dispose();
                } else {
                    UIManager.put("OptionPane.okButtonText", "Yes");
                    JOptionPane.showMessageDialog(this, "Login failed, please check username and password!");
                    UIManager.put("OptionPane.okButtonText", null);
                }
            }catch (Exception e1){
                UIManager.put("OptionPane.okButtonText", "Yes");
                JOptionPane.showMessageDialog(this, e1.getMessage());
                UIManager.put("OptionPane.okButtonText", null);
            }
        });
        add(loginButton, gbc);


        setVisible(true);
    }
    // 模拟登录验证
    private boolean login(String username, String password) {
        // 这里可以调用底层接口，模拟验证成功
        return true;
    }
    }*/

