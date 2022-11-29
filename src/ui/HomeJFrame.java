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
 * ��������ҳ��
 * @author zyj
 */
public class HomeJFrame extends JFrame implements ActionListener {

    // ���崰�ڵĿ�͸�
    private int width;
    private int length;

    // ������Ҫ�Ŀؼ�
    private JTextField urlText = new JTextField(50);

    private JButton downloadButton = new JButton("����");
    private JButton showButton = new JButton("���ļ�������ʾ");

    // ���ر���·���ı���
    private JTextField savePathText = new JTextField(45);
    // ����λ��
    private static String savePath;

    private JButton modifyPathButton = new JButton("ѡ�񱣴�λ��");

    // ��Ϣ����ı���
    private static JTextArea infoArea = new JTextArea(15, 102);

    private static JScrollPane scroll = new JScrollPane(infoArea);

    // �������
    private JPanel contentPanel = null;

    // ������
    private Downloader downloader = new Downloader();

    public HomeJFrame() {
        // ��ȡ��ǰ��Ļ�Ĵ�С��������Ļ��Сȷ�����ڴ�С
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        this.width = new Double(screenSize.getWidth() * 0.6).intValue();
        this.length = new Double(screenSize.getHeight() * 0.6).intValue();
        this.setTitle("M3U8������");
        this.setSize(this.width, this.length);
        this.setVisible(true);
        // ����
        this.setLocationRelativeTo(null);
        // ��X�˳�����
        this.setDefaultCloseOperation(EXIT_ON_CLOSE);
        contentPanel = (JPanel) this.getContentPane();
        // ��ʼ�������ؼ�
        initComponents();
    }

    // ����������
    @Override
    public void actionPerformed(ActionEvent e) {
        // ���ذ�ť�¼�
        if (e.getSource() == downloadButton) {
            String url = urlText.getText();
            if (StringUtils.isEmpty(url)) {
                JOptionPane.showMessageDialog(contentPanel, "��������Դ����");
                return;
            }
            if (StringUtils.isEmpty(savePathText.getText()) || StringUtils.isEmpty(savePath)) {
                JOptionPane.showMessageDialog(contentPanel, "�����ñ���λ��");
                return;
            }
            // �����߳������ļ�
            new Thread(() -> {
                String result = downloader.downloadFile(url, savePath);
                JOptionPane.showMessageDialog(contentPanel, result);
            }).start();
        }else if (e.getSource() == showButton) {
            // ���ļ�������ʾ
            try {
                Desktop.getDesktop().open(new File(savePath));
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        } else if (e.getSource() == modifyPathButton) {
            PlatformImpl.startup(() -> {
                DirectoryChooser dirChooser = new DirectoryChooser();
                dirChooser.setTitle("��ѡ��Ҫ�����·��");
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
     * ��ʼ���������
     */
    private void initComponents() {
        contentPanel.setLayout(new GridLayout(2, 1, 0, 0));
        // contentPanel.setBorder(BorderFactory.createLineBorder(Color.RED));

        JPanel mainPanel = new JPanel(new GridLayout(1, 2, 10, 10));
        // ����������
        JPanel downloadPanel = new JPanel();
        // urlText.setBorder(BorderFactory.createCompoundBorder(new LineBorder(Color.BLACK), "����"));
        // urlText.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "����"));
        // �����ı���
        downloadPanel.add(urlText);
        // ��ťע���¼�������
        downloadButton.addActionListener(this);
        // ���ذ�ť
        downloadPanel.add(downloadButton);
        // ��ʾ��ť
        showButton.addActionListener(this);
        downloadPanel.add(showButton);
        // downloadPanel.setBorder(BorderFactory.createLineBorder(Color.ORANGE));

        // �ұ��������
        JPanel settingPanel = new JPanel();
        savePath = FileUtils.getConfig(Constant.CONFIG_PATH, Constant.PATH_NAME_KEY);
        if (null == savePath) {
            savePath = FileSystemView.getFileSystemView().getHomeDirectory().getPath();
        }
        savePathText.setText(savePath);
        // savePathText.setFont(new Font("����", Font.PLAIN,18));
        savePathText.setEditable(false);
        savePathText.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "����λ��"));
        settingPanel.add(savePathText);
        settingPanel.add(modifyPathButton);
        modifyPathButton.addActionListener(this);
        JLabel authorLabel = new JLabel("���ߣ�С��");
        JLabel versionLabel = new JLabel("�汾��" + Constant.VERSION);
        settingPanel.add(authorLabel);
        settingPanel.add(versionLabel);
        // settingPanel.setBorder(BorderFactory.createLineBorder(Color.YELLOW));

        mainPanel.add(downloadPanel);
        mainPanel.add(settingPanel);
        // mainPanel.setBorder(BorderFactory.createLineBorder(Color.GREEN));

        // ��Ϣ������
        JPanel infoPanel = new JPanel();
        infoArea.setLineWrap(true);
        infoArea.setEditable(false);
        infoArea.setWrapStyleWord(true);
        // infoArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        // ���ñ߿�߾�
        // infoArea.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.GRAY),
        //        BorderFactory.createEmptyBorder(10, 10, 10, 10)));
        // infoArea.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "��Ϣ"));
        // infoArea.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "��Ϣ"));
        // infoPanel.setBorder(BorderFactory.createLineBorder(Color.BLUE));
        infoPanel.add(scroll);
        infoPanel.setBorder(BorderFactory.createTitledBorder(new LineBorder(Color.BLACK), "��Ϣ"));

        contentPanel.add(mainPanel);
        contentPanel.add(infoPanel);
        this.setResizable(false);
    }

    /**
     * ׷����Ϣ����Ϣ�������
     * ���̲߳�����ʹ��synchronized
     * @param msg
     */
    public static synchronized void appendMessage(String msg) {
        infoArea.append("[" + StringUtils.formatTime(Constant.DATE_FORMAT_PATTERN) + "] " + msg + "\r\n");
        // �������������ֹ���
        JScrollBar scrollBar = scroll.getVerticalScrollBar();
        scrollBar.setValue(scrollBar.getMaximum());
    }

    /**
     * д����Ϣ����Ϣ�������
     * @param msg
     */
    public static synchronized void writeMessage(String msg) {
        infoArea.setText("[" + StringUtils.formatTime(Constant.DATE_FORMAT_PATTERN) + "] " + msg + "\r\n");
    }

    public static void main(String[] args) {
        Font song = new Font("����", Font.PLAIN,18);
        Font infoFont = new Font("����", Font.BOLD,24);
        // Font font1 = new Font("�����п�", Font.PLAIN, 18);
        Font font2 = new Font("΢�����", Font.PLAIN, 12);

        // ���������
        UIManager.put("TextField.font", song);
        UIManager.put("TextArea.font", infoFont);
        UIManager.put("Button.font", font2);
        // UIManager.put("Table.font", font2);
        // UIManager.put("TableHeader.font", font1);
        UIManager.put("TitledBorder.font", font2);
        // ��ֹ�߳�����
        SwingUtilities.invokeLater(() ->
                new HomeJFrame().setVisible(true)
        );
    }

}