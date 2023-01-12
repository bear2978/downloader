package core;

import common.Constant;
import ui.HomeJFrame;
import utils.LogUtils;

import java.util.concurrent.atomic.LongAdder;

public class DownloadInfoThread implements Runnable {

    // 下载文件总大小
    private final long httpFileContentLength;

    // 本地已下载文件的大小，
    public LongAdder finishedSize = new LongAdder();

    // 本次累计下载的大小
    public volatile LongAdder downSize = new LongAdder();

    // 前一次下载的大小
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
        // 计算文件总大小 单位：mb
        String httpFileSize = String.format("%.2f", httpFileContentLength / Constant.MB);

        // 计算每秒下载速度 kb
        int speed = (int)((downSize.doubleValue() - prevSize) / 1024d);
        prevSize = downSize.doubleValue();

        // 剩余文件的大小
        double remainSize = httpFileContentLength - finishedSize.doubleValue() - downSize.doubleValue();

        // 计算剩余时间
        String remainTime = String.format("%.1f", remainSize / 1024d / speed);

        if ("Infinity".equalsIgnoreCase(remainTime)) {
            remainTime = "-";
        }

        // 已下载大小
        String currentFileSize = String.format("%.2f", (downSize.doubleValue() - finishedSize.doubleValue()) / Constant.MB);
        String downInfo = String.format("已下载 %smb / %smb，速度 %skb/s，预计剩余时间 %ss", currentFileSize, httpFileSize, speed, remainTime);

        LogUtils.info(downInfo);
        // 显示信息到界面
        HomeJFrame.appendMessage(downInfo);
    }
}
