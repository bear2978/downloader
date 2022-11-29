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
 * ��Ƭ��������
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

        // �жϸ�Ƭ�ļ��Ƿ�����
        File file = new File (this.path);
        if (file.exists() && FileUtils.getFileContentLength(this.path) >= endPos - startPos + 1){
            LogUtils.info("�ļ�Ƭ {} �Ѵ���", file.getPath());
            countDownLatch.countDown();
            return true;
        }

        // ��ȡ�ֿ����ص�����
        HttpURLConnection httpURLConnection = HttpUtils.getHttpURLConnection(url, startPos, endPos);

        try (
                InputStream input = httpURLConnection.getInputStream();
                BufferedInputStream bis = new BufferedInputStream(input);
                RandomAccessFile accessFile = new RandomAccessFile(file.getPath(), "rw")
        ) {
            byte[] buffer = new byte[Constant.BYTE_SIZE];
            int len = -1;
            // ѭ����ȡ����
            while ((len = bis.read(buffer)) != -1) {
                // 1������������֮��, ͨ��ԭ������в���
                downloadInfoThread.getDownSize().add(len);
                accessFile.write(buffer,0, len);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            LogUtils.error("�����ļ�������{}", url);
            return false;
        } catch (Exception e) {
            LogUtils.error("{} ����ʧ�ܣ�", file.getName());
            new Thread (() -> {
                new DownloaderTask(this.url, this.path, this.startPos, this.endPos, this.part, downloadInfoThread, countDownLatch);
            }).start();
        } finally {
            httpURLConnection.disconnect();
        }
        return true;
    }

}
