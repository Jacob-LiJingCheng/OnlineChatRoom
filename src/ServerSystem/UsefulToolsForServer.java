package ServerSystem;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class UsefulToolsForServer {
    private static final String USER_DATA_FILE_PATH = "user_data.ser";

    // 保存序列化的User对象列表到本地文件的方法
    public static void saveUsersToFile(ArrayList<User> userList) {
        if(userList.isEmpty()) {
            return;
        }
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(USER_DATA_FILE_PATH))) {
            oos.writeObject(userList);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // 从本地文件读取序列化的User对象列表的方法
    @SuppressWarnings("unchecked")
    public static ArrayList<User> readUsersFromFile() {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(USER_DATA_FILE_PATH))) {
            return (ArrayList<User>) ois.readObject();
        } catch (FileNotFoundException e){
            return null;
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) {
        List<User> userList = readUsersFromFile();
    }

    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(date);
    }


}
