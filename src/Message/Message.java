package Message;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

// 消息类，包含了消息的各种属性以及操作类型枚举
public class Message implements Serializable,Cloneable {

    private Map<String, ArrayList<String>> mesMap;

    // 消息的发送者
    private String sender;
    // 消息的接收者列表，可支持向多个用户发送消息
    private List<String> receivers;
    // 消息的内容
    private String content;
    // 消息创建的时间戳
    private Date timestamp;
    // 与消息相关的操作类型
    private Operation operation;

    //消息想要的对象列表
    private List<String> elementList;
    private Map<String, Object> attributes;

    // 操作类型枚举类，定义了与消息相关的各种操作
    public enum Operation {
        LOGIN("登录确认"),
        REGISTER("注册"),
        SEND_MESSAGE("发送消息"),
        RECEIVE_MESSAGE("接受消息"),
        UPDATE_STATUE("更新用户状态"),
        UPDATE_STATUE_SINGLE("更新单一用户状态"),
        SEARCH_USER("检查用户是否存在"),
        ASK_OFFLINE_MES("请求离线消息");

        private final String description;

        // 构造函数，用于初始化每个操作的描述
        Operation(String description) {
            this.description = description;
        }

        // 获取操作描述的方法
        public String getDescription() {
            return description;
        }
    }

    public Message() {}

    // 构造函数，用于初始化消息对象的所有属性，包括操作类型和状态
    public Message(String sender, List<String> receivers, Operation operation, String content, List<String> elementList) {
        this.elementList = elementList;
        this.operation = operation;
        this.content = content;
        this.receivers = receivers;
        this.sender = sender;
    }

    public Message(String sender, List<String> receivers, Operation operation,String content ) {
        this.sender = sender;
        this.receivers = receivers;
        this.content = content;
        this.operation = operation;
        this.timestamp = new Date();
    }

    public Message(String sender, List<String> receivers, Operation operation, String content, Map<String, Object> attributes) {
        this.sender = sender;
        this.content = content;
        this.operation = operation;
        this.attributes = attributes;
        this.receivers = receivers;
        this.timestamp = new Date();
    }

    private Message(Map<String, Object> attributes, List<String> elementList, Operation operation, Date timestamp, String content, List<String> receivers, String sender, Map<String, ArrayList<String>> mesMap) {
        this.attributes = attributes;
        this.elementList = elementList;
        this.operation = operation;
        this.timestamp = timestamp;
        this.content = content;
        this.receivers = receivers;
        this.sender = sender;
        this.mesMap = mesMap;
    }

    // 获取消息发送者的方法
    public String getSender() {
        return sender;
    }

    // 获取消息接收者列表的方法
    public List<String> getReceivers() {
        return receivers;
    }

    // 获取消息内容的方法
    public String getContent() {
        return content;
    }

    // 获取消息操作类型的方法
    public Operation getOperation() {
        return operation;
    }

    // 获取消息时间戳的方法
    public Date getTimestamp() {
        return timestamp;
    }

    public List<String> getElementList() {
        return elementList;
    }

    public void setElementList(List<String> elementList) {
        this.elementList = elementList;
    }

    public Map<String, ArrayList<String>> getMesMap() {
        return mesMap;
    }

    public void setMesMap(Map<String, ArrayList<String>> mesMap) {
        this.mesMap = mesMap;
    }

    public Map<String, Object> getAttributes() {
        return attributes;
    }

    public void setAttributes(Map<String, Object> attributes) {
        this.attributes = attributes;
    }

    public void setOperation(Operation operation) {
        this.operation = operation;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public void setReceivers(List<String> receivers) {
        this.receivers = receivers;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    @Override
    public String toString() {
        return "Message{" +
                "mesMap=" + mesMap +
                ", sender='" + sender + '\'' +
                ", receivers=" + receivers +
                ", content='" + content + '\'' +
                ", timestamp=" + timestamp +
                ", operation=" + operation +
                ", elementList=" + elementList +
                ", attributes=" + attributes +
                '}';
    }

    @Override
    public Message clone() throws CloneNotSupportedException {
        super.clone();

        return new Message(attributes, elementList, operation, timestamp,
                content, receivers, sender, mesMap);
    }
}