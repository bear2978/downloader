package ui;

import com.sun.javafx.application.PlatformImpl;
import common.Constant;
import core.Downloader;
import javafx.stage.DirectoryChooser;
import utils.FileUtils;
import utils.StringUtils;
import javax.swing.*;
import javax.swing.border.LineBorder;
import javax.swing.filechooser.FileSystemView;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;


/**
 * 下载器主页面
 * @author zyj
 */
public class HomeJFrame extends JFrame implements ActionListener {

    // 定义窗口的宽和高
    private int width;
    private int length;

    // 所有需要的控件
    private JTextField urlText = new JTextField(50);

    private JButton downloadButton = new JButton("下载");
    private JButton showButton = new JButton("在文件夹中显示");

    // 下载保存路径文本域
    private JTextField savePathText = new JTextField(45);
    // 保存位置
    private static String savePath;

    private JButton modifyPathButton = new JButton("选择保存位置");

    // 信息输出文本域
    private static JTextArea infoArea = new JTextArea(15, 102);

    private static JScrollPane scroll = new JScrollPane(infoArea);

    // 窗口面板
    private JPanel contentPanel = null;

    // 下载器
    private Downloader downloader = new Downloader();

    public HomeJFrame() {
        // 获取当前屏幕的大小，根据屏幕大小确定窗口大小
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.width = new Double(screenSize.getWidth() * 0.6).intValue();
        this.length = new Double(screenSize.getHeight() * 0.6).intValue();
        this.setTitle("M3U8下载器");
        this.setSize(this.width, this.length);
        this.setVisible(true);
        // 居中
        this.setLocationRelativeTo(null);
        // 点X退出程序
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        contentPanel = (JPanel) this.getContentPane();
        // 初始化其他控件
        initComponents();
    }

    // 处理按键监听
    @Override
    public void actionPerformed(ActionEvent e) {
        // 下载按钮事件
        if (e.getSource() == downloadButton) {
            String url = urlText.getText();
            if (StringUtils.isEmpty(url)) {
                JOptionPane.showMessageDialog(contentPanel, "请输入资源链接");
                return;
            }
            if (StringUtils.isEmpty(savePathText.getText()) || StringUtils.isEmpty(savePath)) {
                JOptionPane.showMessageDialog(contentPanel, "请设置保存位置");
                return;
            }
            // 开启线程下载文件
            new Thread(() -> {
                String result = downloader.downloadFile(url, savePath);
                JOptionPane.showMessageDialog(contentPanel, result);
            }).start();
        }else if (e.getSource() == showButton) {
            // 在文件夹中显示
            try {
                Desktop.getDesktop().open(new File(savePath));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (e.getSource() == modifyPathButton) {
            PlatformImpl.startup(() -> {
                DirectoryChooser dirChooser = new DirectoryChooser();
                dirChooser.setTitle("请选择要保存的路径");
                dirChooser.setInitialDirectory(new File(savePath).getParentFile());
                File choicePath = dirChooser.showDialog(null);
                if (null != choicePath) {
                    System.out.println(choicePath.getPath());
                    savePath = choicePath.getPath();
                    savePathText.setText(savePath);
                    FileUtils.setConfig(Constant.CONFIG_PATH, Constant.PATH_NAME_KEY, savePath);
                }
            });
        }
    }

    /**
     * 初始化其他组件
     */
    private void initComponents() {
        contentPanel.setLayout(new GridLayout(2, 1, 0, 0));
        // contentPanel.setBorder(BorderFactory.createLineBorder(Color.RED));

        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        // 左边下载面板
        JPanel downloadPanel = new JPanel();
        // urlText.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Color.BLACK), "标题"));
        // urlText.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "标题"));
        // 链接文本框
        downloadPanel.add(urlText);
        // 按钮注册事件监听器
        downloadButton.addActionListener(this);
        // 下载按钮
        downloadPanel.add(downloadButton);
        // 显示按钮
        showButton.addActionListener(this);
        downloadPanel.add(showButton);
        // downloadPanel.setBorder(BorderFactory.createLineBorder(Color.ORANGE));

        // 右边设置面板
        JPanel settingPanel = new JPanel();
        savePath = FileUtils.getConfig(Constant.CONFIG_PATH, Constant.PATH_NAME_KEY);
        if (null == savePath) {
            savePath = FileSystemView.getFileSystemView().getHomeDirectory().getPath();
        }
        savePathText.setText(savePath);
        // savePathText.setFont(new Font("宋体", Font.PLAIN,18));
        savePathText.setEditable(false);
        savePathText.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "保存位置"));
        settingPanel.add(savePathText);
        settingPanel.add(modifyPathButton);
        modifyPathButton.addActionListener(this);
        JLabel authorLabel = new JLabel("作者：小熊");
        JLabel versionLabel = new JLabel("版本：" + Constant.VERSION);
        settingPanel.add(authorLabel);
        settingPanel.add(versionLabel);
        // settingPanel.setBorder(BorderFactory.createLineBorder(Color.YELLOW));

        mainPanel.add(downloadPanel);
        mainPanel.add(settingPanel);
        // mainPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN));

        // 信息输出面板
        JPanel infoPanel = new JPanel();
        infoArea.setLineWrap(true);
        infoArea.setEditable(false);
        infoArea.setWrapStyleWord(true);
        // infoArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        // 设置边框边距
        // infoArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY),
        //        BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        // infoArea.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "信息"));
        // infoArea.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "信息"));
        // infoPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
        infoPanel.add(scroll);
        infoPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "信息"));

        contentPanel.add(mainPanel);
        contentPanel.add(infoPanel);
        this.setResizable(false);
    }

    /**
     * 追加信息到信息输出区域
     * 多线程操作，使用synchronized
     * @param msg
     */
    public static synchronized void appendMessage(String msg) {
        infoArea.append("[" + StringUtils.formatTime(Constant.DATE_FORMAT_PATTERN) + "] " + msg + "\r\n");
        // 进度条跟随文字滚动
        JScrollBar scrollBar = scroll.getVerticalScrollBar();
        scrollBar.setValue(scrollBar.getMaximum());
    }

    /**
     * 写入信息到信息输出区域
     * @param msg
     */
    public static synchronized void writeMessage(String msg) {
        infoArea.setText("[" + StringUtils.formatTime(Constant.DATE_FORMAT_PATTERN) + "] " + msg + "\r\n");
    }

    public static void main(String[] args) {
        Font song = new Font("宋体", Font.PLAIN,18);
        Font infoFont = new Font("宋体", Font.BOLD,24);
        // Font font1 = new Font("华文行楷", Font.PLAIN, 18);
        Font font2 = new Font("微软黑体", Font.PLAIN, 12);

        // 字体的修饰
        UIManager.put("TextField.font", song);
        UIManager.put("TextArea.font", infoFont);
        UIManager.put("Button.font", font2);
        // UIManager.put("Table.font", font2);
        // UIManager.put("TableHeader.font", font1);
        UIManager.put("TitledBorder.font", font2);
        // 防止线程死锁
        SwingUtilities.invokeLater(() ->
                new HomeJFrame().setVisible(true)
        );
    }

}