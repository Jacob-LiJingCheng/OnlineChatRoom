package UserInterface;

import java.text.SimpleDateFormat;
import java.util.Date;

public class UsefulToolsForClient {
    public static String formatDate(Date date) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss");
        return sdf.format(date);
    }
}
