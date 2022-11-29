package core;

import common.Constant;
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

    public DownloaderTask(String url, String path, long startPos, long endPos, int part, DownloadInfoThread downloadInfoThread, CountDownLatch countDownLatch) {
        this.url = url;
        this.path = path;
        this.startPos = startPos;
        this.endPos = endPos;
        this.part = part;
        this.downloadInfoThread = downloadInfoThread;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public Boolean call() throws Exception {

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
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            LogUtils.error("下载文件不存在{}", url);
            return false;
        } catch (Exception e) {
            LogUtils.error("{} 下载失败！", file.getName());
            new Thread (() -> {
                new DownloaderTask(this.url, this.path, this.startPos, this.endPos, this.part, downloadInfoThread, countDownLatch);
            }).start();
        } finally {
            httpURLConnection.disconnect();
        }
        return true;
    }

}
