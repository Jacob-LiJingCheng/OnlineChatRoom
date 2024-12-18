package UserInterface;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.CategoryDataset;
import org.jfree.data.category.DefaultCategoryDataset;

import java.awt.*;
import java.time.Duration;
import java.time.Instant;

public class MessageDelayChart {

    public static void main(String[] args) throws Exception {
        // 例子：字符串数组，格式为 "消息名,延迟时间"
        String serverAddress = "127.0.0.1";  // 示例的本地回环地址，实际中可能是真实的服务器IP
        int serverPort = 8888;  // 示例端口号，要与服务器监听的端口一致
        Client client = new Client(serverAddress, serverPort);
        String n="";
        String[] messageData = new String[5];
        double average =0;
        long total=0;
        for (int i = 0; i < 100; i++) {
            n= i+"00000";
            Instant start = Instant.now();
            client.register(n,n);
            Instant end = Instant.now();
            total += Duration.between(start, end).toMillis();
        }
        messageData[0] = "register,"+(double) total / 10;

        total=0;
        for (int i = 0; i < 100; i++) {
            n= i+"00000";
            Instant start = Instant.now();
            client.login(n,n);
            Instant end = Instant.now();
            total += Duration.between(start, end).toMillis();
        }
        messageData[1] = "login,"+(double) total / 10;

        total=0;
        for (int i = 0; i < 100; i++) {
            Instant start = Instant.now();
            client.requestOfflineMessages();
            Instant end = Instant.now();
            total += Duration.between(start, end).toMillis();
        }
        messageData[2] = "request offline messages(null),"+(double) total / 10;

        total=0;
        for (int i = 0; i < 100; i++) {
            n= i+"00000";
            Instant start = Instant.now();
            client.requestUserStatus(n);
            Instant end = Instant.now();
            total += Duration.between(start, end).toMillis();
        }
        messageData[3] = "update status of a user,"+(double) total / 10;

        total=0;
        for (int i = 0; i < 100; i++) {
            n= i+"00000";
            Instant start = Instant.now();
            client.checkUserExists(n);
            Instant end = Instant.now();
            total += Duration.between(start, end).toMillis();
        }
        messageData[4] = "check if user exits,"+(double) total / 10;


        // 创建数据集
        CategoryDataset dataset = createDataset(messageData);

        // 根据数据集创建图表
        JFreeChart chart = createChart(dataset);

        // 将图表显示在一个窗口中
        displayChart(chart);
    }

    // 创建数据集的方法，根据传入的字符串数组生成
    private static CategoryDataset createDataset(String[] messageData) {
        DefaultCategoryDataset dataset = new DefaultCategoryDataset();

        // 遍历传入的字符串数组，解析数据并添加到数据集中
        for (String data : messageData) {
            String[] parts = data.split(",");
            String messageName = parts[0];  // 消息名称
            double delayTime = Double.parseDouble(parts[1]);  // 延迟时间
            System.out.println(delayTime);

            // 添加数据到数据集中
            dataset.addValue(delayTime, messageName, "delay time");
        }

        return dataset;
    }

    // 创建图表的方法
    private static JFreeChart createChart(CategoryDataset dataset) {
        JFreeChart chart = ChartFactory.createBarChart(
                "Comparison among Different Message Delay",  // 标题
                "Message Type",                            // X轴标签
                "Average Delay Time (ms) ",     // Y轴标签
                dataset,                                  // 数据集
                PlotOrientation.VERTICAL,                 // 图表方向
                true,                                     // 显示图例
                true,                                     // 提供提示
                false                                     // 不生成URL
        );

        // 设置图表的背景颜色
        chart.setBackgroundPaint(Color.white);

        return chart;
    }

    // 显示图表的方法
    private static void displayChart(JFreeChart chart) {
        ChartFrame frame = new ChartFrame("Message delay chart", chart);
        frame.pack();
        frame.setVisible(true);
    }
}
