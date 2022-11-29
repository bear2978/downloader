package core;

import common.Constant;
import ui.HomeJFrame;
import utils.LogUtils;

import java.util.concurrent.atomic.LongAdder;

public class DownloadInfoThread implements Runnable {

    // �����ļ��ܴ�С
    private final long httpFileContentLength;

    // �����������ļ��Ĵ�С��
    public LongAdder finishedSize = new LongAdder();

    // �����ۼ����صĴ�С
    public volatile LongAdder downSize = new LongAdder();

    // ǰһ�����صĴ�С
    public double prevSize;

    public LongAdder getDownSize() {
        return downSize;
    }

    public void setDownSize(LongAdder downSize) {
        this.downSize = downSize;
    }

    public DownloadInfoThread(long httpFileContentLength) {
        this.httpFileContentLength = httpFileContentLength;
    }


    @Override
    public void run() {
        // �����ļ��ܴ�С ��λ��mb
        String httpFileSize = String.format("%.2f", httpFileContentLength / Constant.MB);

        // ����ÿ�������ٶ� kb
        int speed = (int)((downSize.doubleValue() - prevSize) / 1024d);
        prevSize = downSize.doubleValue();

        // ʣ���ļ��Ĵ�С
        double remainSize = httpFileContentLength - finishedSize.doubleValue() - downSize.doubleValue();

        // ����ʣ��ʱ��
        String remainTime = String.format("%.1f", remainSize / 1024d / speed);

        if ("Infinity".equalsIgnoreCase(remainTime)) {
            remainTime = "-";
        }

        // �����ش�С
        String currentFileSize = String.format("%.2f", (downSize.doubleValue() - finishedSize.doubleValue()) / Constant.MB);
        String downInfo = String.format("������ %smb / %smb���ٶ� %skb/s��Ԥ��ʣ��ʱ�� %ss", currentFileSize, httpFileSize, speed, remainTime);

        LogUtils.info(downInfo);
        // ��ʾ��Ϣ������
        HomeJFrame.appendMessage(downInfo);
    }
}
