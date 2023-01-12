package core;

import common.Constant;
import ui.HomeJFrame;
import utils.FileUtils;
import utils.HttpUtils;
import utils.LogUtils;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

/**
 * 分片下载任务
 * @author zyj
 */
public class DownloaderTask implements Callable<Boolean> {

    private String url;

    private String path;

    private int part;

    private long startPos;

    private long endPos;

    private DownloadInfoThread downloadInfoThread;

    private CountDownLatch countDownLatch;

    private int frequency;

    public DownloaderTask(String url, String path, long startPos, long endPos, int part, DownloadInfoThread downloadInfoThread,
                          CountDownLatch countDownLatch, int frequency) {
        this.url = url;
        this.path = path;
        this.startPos = startPos;
        this.endPos = endPos;
        this.part = part;
        this.downloadInfoThread = downloadInfoThread;
        this.countDownLatch = countDownLatch;
        this.frequency = frequency;
    }

    @Override
    public Boolean call() throws Exception {
        String msg = "";
        // 判断该片文件是否下载
        File file = new File (this.path);
        if (file.exists() && FileUtils.getFileContentLength(this.path) >= endPos - startPos + 1){
            LogUtils.info("文件片 {} 已存在", file.getPath());
            countDownLatch.countDown();
            return true;
        }

        // 获取分块下载的连接
        HttpURLConnection httpURLConnection = HttpUtils.getHttpURLConnection(url, startPos, endPos);

        try (
                InputStream input = httpURLConnection.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(input);
                RandomAccessFile accessFile = new RandomAccessFile(file.getPath(), "rw")
        ) {
            byte[] buffer = new byte[Constant.BYTE_SIZE];
            int len = -1;
            // 循环读取数据
            while ((len = bis.read(buffer)) != -1) {
                // 1秒内下载数据之和, 通过原子类进行操作
                downloadInfoThread.getDownSize().add(len);
                accessFile.write(buffer,0, len);
            }
            msg = String.format("第 %s 个片段【%s】下载成功！", this.part + 1, file.getName());
            LogUtils.info(msg);
            HomeJFrame.appendMessage(msg);
            countDownLatch.countDown();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            LogUtils.error("下载文件不存在\n{}", url);
            countDownLatch.countDown();
            throw new FileNotFoundException();
        } catch (Exception e) {
            if (this.frequency < Constant.TRY_TIME) {
                msg = String.format("【%s】下载失败！开始第%s次重试", file.getName(), this.frequency + 1);
                LogUtils.error(msg);
                HomeJFrame.appendMessage(msg);
                new Thread (() -> {
                    new DownloaderTask(this.url, this.path, this.startPos, this.endPos, this.part, downloadInfoThread,
                            countDownLatch, this.frequency + 1);
                }).start();
            } else {
                LogUtils.error("{} 下载失败！", file.getName());
                countDownLatch.countDown();
            }
        } finally {
            httpURLConnection.disconnect();
        }
        return true;
    }

}