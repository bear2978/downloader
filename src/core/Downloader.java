package core;

import common.Constant;
import ui.HomeJFrame;
import utils.FileUtils;
import utils.HttpUtils;
import utils.LogUtils;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.*;

/**
 * ������
 * @author zyj
 */

public class Downloader {

    /**
     * �ļ�����
     * @param url
     */
    public String downloadFile(String url, String path) {
        // �ļ��������ر���·��
        String fileName = HttpUtils.getHttpFileName(url);
        boolean isM3U8File = false;
        // ����M3U8�ļ���
        if (url.endsWith(Constant.M3U8_SUFFIX_NAME)) {
            isM3U8File = true;
            fileName = fileName.replace(Constant.M3U8_SUFFIX_NAME, Constant.MP4_SUFFIX_NAME);
        }
        File file = new File(path, fileName);

        // ��ȡ�������ļ���С
        long localFileLength = FileUtils.getFileContentLength(file.getPath());
        HttpURLConnection httpURLConnection = null;
        DownloadInfoThread downloadInfoThread = null;
        ThreadPoolExecutor poolExecutor = null;
        M3U8 m3u8 = null;
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        try {
            // ��ȡ���Ӷ���
            httpURLConnection = HttpUtils.getHttpURLConnection(url);
            // ��ȡ�����ļ����ܴ�С
            int contentLength = httpURLConnection.getContentLength();
            if (!isM3U8File && localFileLength >= contentLength) {
                return String.format("�ļ���%s�������أ�������������", fileName);
            }
            // ��Ҫ���ص��ļ����зָ�
            List<FileSequence> fileSequences = new LinkedList<>();
            // ��Ƭ�ļ��ȱ��浽��ʱ�ļ���
            String tmpPath = file.getParent() + Constant.TMP_PATH;
            // �ж��ļ���ʽ
            if (isM3U8File) {
                m3u8 = parseM3U8Url(url);
                List<String> tsList = m3u8.getTsList();
                int segmentNum = tsList.size();
                HomeJFrame.appendMessage("���� m3u8 ���ӳɹ�...");
                HomeJFrame.appendMessage(String.format("���ܷ�ʽ�� %s", m3u8.getMethod()));
                HomeJFrame.appendMessage(String.format("������Կ�� %s", m3u8.getKey()));
                HomeJFrame.appendMessage(String.format("IVƫ������ %s", m3u8.getIV()));
                HomeJFrame.appendMessage(String.format("�� %s ��tsƬ��", segmentNum));
                // ��װ ts �б�
                for (String tsUrl : tsList) {
                    String tsName = tsUrl.substring(tsUrl.lastIndexOf("/") + 1);
                    String desPath = tmpPath + tsName;
                    FileSequence itemSequence = new FileSequence(tsUrl, desPath);
                    fileSequences.add(itemSequence);
                }
            }else if(contentLength < Constant.PART_NUM * Constant.BYTE_SIZE) {
                FileSequence itemSequence = new FileSequence(url, file.getPath());
                fileSequences.add(itemSequence);
            }else {
                // �ļ��ֿ�, �����зֺ���ļ�ÿһ��Ĵ�С
                long size = contentLength / Constant.PART_NUM;
                // ����ÿһ���ļ�
                for (int i = 0; i < Constant.PART_NUM; i++) {
                    //����������ʼλ��
                    long startPos = i * size;
                    long endPos = startPos + size;
                    // ������ǵ�һ�飬��ʼλ��Ҫ +1
                    if (startPos != 0) {
                        startPos += 1;
                    }
                    // �������λ��
                    if (i == Constant.PART_NUM - 1) {
                        //�������һ�飬����ʣ��Ĳ���
                        endPos = 0;
                    }
                    FileSequence itemSequence = new FileSequence(url, tmpPath + fileName + Constant.PART_NAME + i, startPos, endPos);
                    fileSequences.add(itemSequence);
                }
            }
            int partNum = fileSequences.size();
            CountDownLatch countDownLatch = new CountDownLatch(partNum);
            // �����̳߳ض���
            poolExecutor = new ThreadPoolExecutor(partNum, partNum, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(partNum));
            if (! isM3U8File) {
                // ������ȡ������Ϣ���������
                downloadInfoThread = new DownloadInfoThread(contentLength);
                // �����񽻸��߳�ִ�У�ÿ��1��ִ��һ��
                scheduledExecutorService.scheduleAtFixedRate(downloadInfoThread, 1, 1, TimeUnit.SECONDS);
            }
            // �ύ�����̳߳�
            for (int i = 0; i < partNum; i++) {
                FileSequence sequence = fileSequences.get(i);
                if (isM3U8File) {
                    poolExecutor.submit(new M3U8DownloaderTask(sequence.getUrl(), sequence.getLocalPath(), m3u8, i + 1, countDownLatch));
                }else {
                    poolExecutor.submit(new DownloaderTask(sequence.getUrl(), sequence.getLocalPath(),
                            sequence.getStartPos(), sequence.getEndPos(), i, downloadInfoThread, countDownLatch));
                }
            }
            // �ȴ����з�Ƭ�����������
            countDownLatch.await();
            // �ϲ��ļ�
            if (merge(fileSequences, file.getPath())) {
                // clearTemp(filePath);
                LogUtils.info("�ϲ��ļ���{}�����", fileName);
            }
            // �ж��ļ���ʽ
//            if (url.endsWith(Constant.M3U8_SUFFIX_NAME)) {
//                M3U8 m3u8 = parseM3U8Url(url);
//                // ʹ���������ʼ����tsƬ��
//                List<String> list = m3u8.getTsList();
//                int segmentNum = list.size();
//
//                HomeJFrame.appendMessage("���� m3u8 ���ӳɹ�...");
//                LogUtils.info("���ܷ�ʽ�� {}", m3u8.getMethod());
//                LogUtils.info("������Կ�� {}", m3u8.getKey());
//                LogUtils.info("IVƫ������ {}", m3u8.getIV());
//                LogUtils.info("�� {} ��tsƬ��", segmentNum);
//                countDownLatch = new CountDownLatch(segmentNum);
//                poolExecutor = new ThreadPoolExecutor(segmentNum, segmentNum, 0, TimeUnit.SECONDS,
//                        new ArrayBlockingQueue<>(segmentNum));
//                // �ж� .tmp Ŀ¼�Ƿ���ڣ� �������򴴽��ļ���
//                File dir = new File(path + "/.tmp/");
//                // ���������ȴ������ļ���
//                if (!dir.exists()) {
//                    LogUtils.info("�½��ļ���{}", dir.getPath());
//                    dir.mkdirs();
//                }
//                for (int i = 0; i < list.size(); i++) {
//                    String tsUrl = list.get(i);
//                    String tsName = tsUrl.substring(tsUrl.lastIndexOf("/") + 1);
//                    String desPath = dir.getPath() + "/" + tsName;
//                    // ʹ�ö��߳��������е�ts�ļ����ٺϲ��ļ�
//                    poolExecutor.submit(new M3U8DownloaderTask(tsUrl, desPath, m3u8, i, countDownLatch));
//                }
//                // �ȴ���Ƭ�����������
//                countDownLatch.await();
//                // �ϲ��ļ�
//                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file, true));
//                for (int i = 0; i < list.size(); i++) {
//                    String tsUrl = list.get(i);
//                    String tsName = tsUrl.substring(tsUrl.lastIndexOf("/") + 1);
//                    String tsPath = dir.getPath() + "/" + tsName;
//                    BufferedInputStream bis = new BufferedInputStream(new FileInputStream(tsPath));
//                    byte[] flush = new byte[Constant.BYTE_SIZE];
//                    int len = -1;
//                    while ((len = bis.read(flush)) != -1) {
//                        bos.write(flush, 0, len);
//                    }
//                    bos.flush();
//                    bis.close();
//                    HomeJFrame.appendMessage(tsName + "�ϲ��ɹ���");
//                }
//                LogUtils.info("�ϲ��ļ���{}�����", fileName);
//            } else if(contentLength < Constant.PART_NUM * Constant.BYTE_SIZE) {
//                // �ļ���СС��50MBֱ������
//                countDownLatch = new CountDownLatch(1);
//                // �����������
//                DownloaderTask downloaderTask = new DownloaderTask(url, path, 0, -1, -1, downloadInfoThread, countDownLatch);
//                new Thread(new FutureTask<>(downloaderTask)).start();
//                countDownLatch.await();
//            } else {
//                // �ļ��ֿ�, �����зֺ���ļ�ÿһ��Ĵ�С
//                long size = contentLength / Constant.PART_NUM;
//                countDownLatch = new CountDownLatch(Constant.PART_NUM);
//                // �̳߳ض���
//                poolExecutor = new ThreadPoolExecutor(Constant.PART_NUM, Constant.PART_NUM, 0,
//                        TimeUnit.SECONDS, new ArrayBlockingQueue<>(Constant.PART_NUM));
//                // ����ÿһ���ļ�
//                for (int i = 0; i < Constant.PART_NUM; i++) {
//                    //����������ʼλ��
//                    long startPos = i * size;
//                    long endPos = startPos + size;
//                    // ������ǵ�һ�飬��ʼλ��Ҫ +1
//                    if (startPos != 0) {
//                        startPos += 1;
//                    }
//                    // �������λ��
//                    if (i == Constant.PART_NUM - 1) {
//                        //�������һ�飬����ʣ��Ĳ���
//                        endPos = 0;
//                    }
//                    // �����������
//                    DownloaderTask downloaderTask = new DownloaderTask(url, path, startPos, endPos, i, downloadInfoThread, countDownLatch);
//                    // �������ύ���̳߳���
//                    poolExecutor.submit(downloaderTask);
//                }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            HomeJFrame.appendMessage(e.getMessage());
            return "����ʧ��";
        } finally {
            // �ر����Ӷ���
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            // �ر��̳߳�
            if (poolExecutor != null) {
                poolExecutor.shutdown();
            }
            // �ر�
            scheduledExecutorService.shutdownNow();
        }
        HomeJFrame.appendMessage(fileName + "�������");
        return "�������";
    }

    /**
     * ����m3u8����
     * @param url
     * @return
     * @throws IOException
     */
    private static M3U8 parseM3U8Url(String url) throws IOException {
        M3U8 m3u8 = new M3U8();
        // 1.��ȡurl���ø�Ŀ¼
        String basePath = url.substring(0, url.lastIndexOf("/") + 1);
        m3u8.setBasePath(basePath);
        // 2.��ȡm3u8�ļ�������
        BufferedReader br = new BufferedReader(new InputStreamReader(HttpUtils.getHttpURLConnection(url).getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
            // ������ļ����и��ֶ�,���ʾ��m3u8�ļ�û����Դ,��Դ�ض��򵽱�ָ����ļ�
            if (line.endsWith(Constant.M3U8_SUFFIX_NAME)) {
                LogUtils.info("m3u8�ļ��ض��򣡣���");
                return parseM3U8Url(basePath + line);
            }
            // �������ֶ�˵���ļ��Ǽ��ܵ�, METHOD������ʾ���ܷ�����URI��ʾ��Կ�ļ�
            if(line.startsWith("#EXT-X-KEY")) {
                line = line.substring(line.indexOf(":") + 1);
                // METHOD=AES-128,URI="key.key"
                String[] params = line.split(",");
                for (String temp : params){
                    // ���ܷ����ֶ�
                    if(temp.contains("METHOD") || temp.contains("method")){
                        String method = temp.split("=",2)[1];
                        m3u8.setMethod(method);
                    }
                    // ��Կ
                    if(temp.contains("URI") || temp.contains("uri")){
                        String uri = temp.split("=",2)[1].replace("\"","");
                        String key;
                        // ��ȡkey
                        try{
                            if(uri.contains(Constant.HTTP_PROTOCOL_PREFIX) || uri.contains(Constant.HTTPS_PROTOCOL_PREFIX)) {
                                key = getKey(uri);
                            }else {
                                key = getKey(basePath + uri);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                            key = getKey(basePath + "key.key");
                        }
                        m3u8.setKey(key);
                    }
                    // IV�ֶ�
                    if (temp.contains("IV") || temp.contains("iv")) {
                        String iv = temp.split("=",2)[1].replace("\"","");
                        m3u8.setIV(iv);
                    }
                }
            }
            // ts����Ƭ
            if (line.endsWith(Constant.TS_SUFFIX_NAME)) {
                if (line.startsWith(Constant.HTTP_PROTOCOL_PREFIX) || line.startsWith(Constant.HTTPS_PROTOCOL_PREFIX)) {
                    m3u8.getTsList().add(line);
                }else {
                    m3u8.getTsList().add(m3u8.getBasePath() + line);
                }
            }
        }
        // ����
        br.close();
        return m3u8;
    }

    /**
     * ���ӵ�ָ��Url
     * @param url
     * @return
     * @throws IOException
     */
    private static String getKey(String url) throws IOException {
        // ����������
        BufferedReader br = new BufferedReader(new InputStreamReader(HttpUtils.getHttpURLConnection(url).getInputStream()));
        String key = br.readLine();
        // �ر�������
        br.close();
        return key;
    }

    /**
     *  �ļ��ϲ�
     * @param fileName
     * @return
     */
    public boolean merge(List<FileSequence> fileSequences, String fileName) {
        // �����ļ�����ϲ�
        if (fileSequences.size() <= 1) {
            return false;
        }
        LogUtils.info("��ʼ�ϲ��ļ�{}", fileName);
        byte[] buffer = new byte[Constant.BYTE_SIZE];
        int len = -1;
        try (RandomAccessFile accessFile = new RandomAccessFile(fileName, "rw")) {
            for (FileSequence sequence : fileSequences) {
                try(BufferedInputStream bis = new BufferedInputStream(new FileInputStream(sequence.getLocalPath()))) {
                    while ((len = bis.read(buffer)) != -1) {
                        accessFile.write(buffer,0, len);
                    }
                }
            }
            LogUtils.info("�ļ��ϲ����{}", fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}

class FileSequence {
    // Զ��·��
    private String url;
    // ����·��
    private String localPath;
    // �ļ�Ƭ��ʼλ��
    private long startPos;
    // �ļ�Ƭ����λ��
    private long endPos;

    public FileSequence(String url, String localPath) {
        this.url = url;
        this.localPath = localPath;
    }

    public FileSequence(String url, String localPath, long startPos, long endPos) {
        this.url = url;
        this.localPath = localPath;
        this.startPos = startPos;
        this.endPos = endPos;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public long getStartPos() {
        return startPos;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setStartPos(long startPos) {
        this.startPos = startPos;
    }

    public long getEndPos() {
        return endPos;
    }

    public void setEndPos(long endPos) {
        this.endPos = endPos;
    }

    @Override
    public String toString() {
        return "FileSequence{" +
                "url='" + url + '\'' +
                ", localPath='" + localPath + '\'' +
                ", startPos=" + startPos +
                ", endPos=" + endPos +
                '}';
    }
}


class M3U8 {
    // �洢�ļ��ĸ�·��
    private String basePath;
    // �����㷨����
    private String method = "AES-128";
    // ��Կ
    private String key;
    // ����һ��16byte�ĳ�ʼ���������m3u8û��IVֵ��������Ϊ0����
    private String IV = "0000000000000000";

    private List<String> tsList = new LinkedList<>();

    public String getBasePath() {
        return basePath;
    }

    public void setBasePath(String basePath) {
        this.basePath = basePath;
    }

    public String getMethod() {
        return method;
    }

    public String getKey() {
        return key;
    }

    public String getIV() {
        return IV;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setIV(String IV) {
        this.IV = IV;
    }

    public void setTsList(List<String> tsList) {
        this.tsList = tsList;
    }

    public List<String> getTsList() {
        return tsList;
    }

    @Override
    public String toString() {
        return "M3U8{" +
                "basePath='" + basePath + '\'' +
                ", method='" + method + '\'' +
                ", key='" + key + '\'' +
                ", IV='" + IV + '\'' +
                ", tsList=" + tsList +
                '}';
    }

}