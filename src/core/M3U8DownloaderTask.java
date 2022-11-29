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

    public M3U8DownloaderTask(){}

    public M3U8DownloaderTask(String url, String path, M3U8 m3u8, int index, CountDownLatch countDownLatch) {
        this.url = url;
        this.path = path;
        this.m3u8 = m3u8;
        this.index = index;
        this.countDownLatch = countDownLatch;
    }

    @Override
    public Boolean call() throws Exception {
        File file = new File(this.path);
        if (file.exists() && FileUtils.getFileContentLength(file.getPath()) >= HttpUtils.getHttpFileContentLength(url)) {
            countDownLatch.countDown();
            return true;
        }
        LogUtils.info("��ʼ���� {}", this.url);
        try (
            // �������������
            BufferedInputStream bis = new BufferedInputStream(HttpUtils.getHttpURLConnection(url).getInputStream());
            // �ֽ�����������洢ÿһ��tsƬ�ε��ֽ���
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            // ���������
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
        ) {
            byte[] flush = new byte[Constant.BYTE_SIZE];
            int len;
            while ((len = bis.read(flush)) != -1) {
                os.write(flush, 0, len);
            }
            // �õ�һ��Ts�ļ����ֽ���, Ȼ����н��ܲ���
            byte [] tsByte = FileUtils.decrypt(os.toByteArray(), m3u8.getMethod(), m3u8.getKey(), m3u8.getIV());

            if (tsByte != null) {
                bos.write(tsByte);
            }
            bos.flush();
            // ����
            bos.close();
            bis.close();
            countDownLatch.countDown();
            String msg = String.format("�� %s ��Ƭ�Ρ�%s�����سɹ���", this.index, file.getName());
            LogUtils.info(msg);
            HomeJFrame.appendMessage(msg);
        } catch (Exception e) {
            LogUtils.info("{} ����ʧ�ܣ�", file.getName());
            // �ؿ��߳������ļ�
            LogUtils.info("��ʼ�������� " + file.getName() + "��");
            new Thread (() -> {
                new M3U8DownloaderTask(this.url, this.path, this.m3u8, this.index, countDownLatch);
            }).start();
        }
        return true;
    }

}