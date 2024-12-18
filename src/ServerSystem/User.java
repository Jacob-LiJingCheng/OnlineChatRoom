package ServerSystem;

import Message.Message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 用户类，包含了用户的各种属性，如用户名、用户ID、邮箱、密码等
// 还管理着用户的好友列表、离线消息列表等
class User implements Serializable {
    // 用户的用户名
    private String username;
    // 用户的密码
    private String password;
    // 用户的离线消息列表
    private Map<String,ArrayList<String>> offlineMessages;
    // 用户的头像（这里可以根据实际需求设置合适的数据类型来存储头像信息，比如字节数组、图片路径等，此处先以字符串表示头像路径为例）
    private String avatarPath;
    // 当前用户的状态
    private Status status;

    // 用户状态枚举类，用于表示用户的上线或下线状态
    enum Status {
        ONLINE("上线"),
        OFFLINE("下线")
        ;

        private final String description;

        // 构造函数，用于初始化每个状态的描述
        Status(String description) {
            this.description = description;
        }

        // 获取状态描述的方法
        public String getDescription() {
            return description;
        }
    }

    // 构造函数，用于初始化用户对象的所有基本属性
    public User(String username,String password) {
        this.username = username;
        this.password = password;
        this.offlineMessages = new HashMap<>();
        this.avatarPath = ""; // 初始化头像路径为空字符串，可根据实际情况修改初始化值
        this.status = Status.OFFLINE; // 初始化用户状态为下线
    }

    // 获取用户用户名的方法
    public String getUsername() {
        return username;
    }

    // 设置用户用户名的方法
    public void setUsername(String username) {
        this.username = username;
    }

    // 获取用户密码的方法
    public String getPassword() {
        return password;
    }

    // 设置用户密码的方法
    public void setPassword(String password) {
        this.password = password;
    }

    // 获取用户离线消息列表的方法
    public Map<String,ArrayList<String>> getOfflineMessages() {
        if(offlineMessages.isEmpty()) {
            return null;
        }
        return offlineMessages;
    }

    // 添加离线消息到用户离线消息列表的方法
    public void addOfflineMessage(Message message) {
        if (offlineMessages.containsKey(message.getSender())) {
            offlineMessages.get(message.getSender()).add(UsefulToolsForServer.formatDate
                    (message.getTimestamp())+"@@"+ message.getContent());
        }else {
            ArrayList<String> messages = new ArrayList<>();
            messages.add(UsefulToolsForServer.formatDate(message.getTimestamp())+"@@"+ message.getContent());
            offlineMessages.put(message.getSender(),messages);
        }
    }

    public void clearOfflineMessage() {
        offlineMessages.clear();
    }

    // 获取用户头像路径的方法
    public String getAvatarPath() {
        return avatarPath;
    }

    // 设置用户头像路径的方法
    public void setAvatarPath(String avatarPath) {
        this.avatarPath = avatarPath;
    }

    public boolean isOnline() {
        return status == Status.ONLINE;
    }
    // 获取用户状态的方法
    public Status getStatus() {
        return status;
    }

    // 设置用户状态的方法
    public void setStatus(Status status) {
        this.status = status;
    }

}