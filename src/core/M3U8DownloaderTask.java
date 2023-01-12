package core;

import common.Constant;
import ui.HomeJFrame;
import utils.FileUtils;
import utils.HttpUtils;
import utils.LogUtils;

import java.io.*;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;

public class M3U8DownloaderTask implements Callable<Boolean> {

    private String url;

    private String path;

    private M3U8 m3u8;

    private int index;

    private CountDownLatch countDownLatch;

    private int frequency;

    public M3U8DownloaderTask(String url, String path, M3U8 m3u8, int index, CountDownLatch countDownLatch, int frequency) {
        this.url = url;
        this.path = path;
        this.m3u8 = m3u8;
        this.index = index;
        this.countDownLatch = countDownLatch;
        this.frequency = frequency;
    }

    @Override
    public Boolean call() throws Exception {
        String msg = "";
        File file = new File(this.path);
        if (file.exists() && FileUtils.getFileContentLength(file.getPath()) >= HttpUtils.getHttpFileContentLength(url)) {
            countDownLatch.countDown();
            return true;
        }
        LogUtils.info("开始下载 {}", this.url);
        try (
                // 获得网络输入流
                BufferedInputStream bis = new BufferedInputStream(HttpUtils.getHttpURLConnection(url).getInputStream());
                // 字节数组输出流存储每一个ts片段的字节流
                ByteArrayOutputStream os = new ByteArrayOutputStream();
                // 本地输出流
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        ) {
            byte[] flush = new byte[Constant.BYTE_SIZE];
            int len;
            while ((len = bis.read(flush)) != -1) {
                os.write(flush, 0, len);
            }
            // 得到一个Ts文件的字节流, 然后进行解密操作
            byte [] tsByte = FileUtils.decrypt(os.toByteArray(), m3u8.getMethod(), m3u8.getKey(), m3u8.getIV());

            if (tsByte != null) {
                bos.write(tsByte);
            }
            bos.flush();
            msg = String.format("第 %s 个片段【%s】下载成功！", this.index + 1, file.getName());
            LogUtils.info(msg);
            HomeJFrame.appendMessage(msg);
            countDownLatch.countDown();
        } catch (Exception e) {
            e.printStackTrace();
            if (this.frequency < Constant.TRY_TIME) {
                msg = String.format("【%s】下载失败！开始第%s次重试", file.getName(), this.frequency + 1);
                LogUtils.info(msg);
                HomeJFrame.appendMessage(msg);
                // 重开线程下载文件
                new Thread(() -> {
                    new M3U8DownloaderTask(this.url, this.path, this.m3u8, this.index, countDownLatch, this.frequency + 1);
                }).start();
            }else {
                LogUtils.error("{} 下载失败！", file.getName());
                countDownLatch.countDown();
            }
        }
        return true;
    }

}