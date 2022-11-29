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
 * 下载器
 * @author zyj
 */

public class Downloader {

    /**
     * 文件下载
     * @param url
     */
    public String downloadFile(String url, String path) {
        // 文件名和下载保存路径
        String fileName = HttpUtils.getHttpFileName(url);
        boolean isM3U8File = false;
        // 处理M3U8文件名
        if (url.endsWith(Constant.M3U8_SUFFIX_NAME)) {
            isM3U8File = true;
            fileName = fileName.replace(Constant.M3U8_SUFFIX_NAME, Constant.MP4_SUFFIX_NAME);
        }
        File file = new File(path, fileName);

        // 获取已下载文件大小
        long localFileLength = FileUtils.getFileContentLength(file.getPath());
        HttpURLConnection httpURLConnection = null;
        DownloadInfoThread downloadInfoThread = null;
        ThreadPoolExecutor poolExecutor = null;
        M3U8 m3u8 = null;
        ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
        try {
            // 获取连接对象
            httpURLConnection = HttpUtils.getHttpURLConnection(url);
            // 获取下载文件的总大小
            int contentLength = httpURLConnection.getContentLength();
            if (!isM3U8File && localFileLength >= contentLength) {
                return String.format("文件【%s】已下载，无需重新下载", fileName);
            }
            // 对要下载的文件进行分割
            List<FileSequence> fileSequences = new LinkedList<>();
            // 分片文件先保存到临时文件夹
            String tmpPath = file.getParent() + Constant.TMP_PATH;
            // 判断文件格式
            if (isM3U8File) {
                m3u8 = parseM3U8Url(url);
                List<String> tsList = m3u8.getTsList();
                int segmentNum = tsList.size();
                HomeJFrame.appendMessage("解析 m3u8 链接成功...");
                HomeJFrame.appendMessage(String.format("加密方式： %s", m3u8.getMethod()));
                HomeJFrame.appendMessage(String.format("加密密钥： %s", m3u8.getKey()));
                HomeJFrame.appendMessage(String.format("IV偏移量： %s", m3u8.getIV()));
                HomeJFrame.appendMessage(String.format("共 %s 个ts片段", segmentNum));
                // 封装 ts 列表
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
                // 文件分块, 计算切分后的文件每一块的大小
                long size = contentLength / Constant.PART_NUM;
                // 下载每一块文件
                for (int i = 0; i < Constant.PART_NUM; i++) {
                    //计算下载起始位置
                    long startPos = i * size;
                    long endPos = startPos + size;
                    // 如果不是第一块，起始位置要 +1
                    if (startPos != 0) {
                        startPos += 1;
                    }
                    // 计算结束位置
                    if (i == Constant.PART_NUM - 1) {
                        //下载最后一块，下载剩余的部分
                        endPos = 0;
                    }
                    FileSequence itemSequence = new FileSequence(url, tmpPath + fileName + Constant.PART_NAME + i, startPos, endPos);
                    fileSequences.add(itemSequence);
                }
            }
            int partNum = fileSequences.size();
            CountDownLatch countDownLatch = new CountDownLatch(partNum);
            // 创建线程池对象
            poolExecutor = new ThreadPoolExecutor(partNum, partNum, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(partNum));
            if (! isM3U8File) {
                // 创建获取下载信息的任务对象
                downloadInfoThread = new DownloadInfoThread(contentLength);
                // 将任务交给线程执行，每隔1秒执行一次
                scheduledExecutorService.scheduleAtFixedRate(downloadInfoThread, 1, 1, TimeUnit.SECONDS);
            }
            // 提交任务到线程池
            for (int i = 0; i < partNum; i++) {
                FileSequence sequence = fileSequences.get(i);
                if (isM3U8File) {
                    poolExecutor.submit(new M3U8DownloaderTask(sequence.getUrl(), sequence.getLocalPath(), m3u8, i + 1, countDownLatch));
                }else {
                    poolExecutor.submit(new DownloaderTask(sequence.getUrl(), sequence.getLocalPath(),
                            sequence.getStartPos(), sequence.getEndPos(), i, downloadInfoThread, countDownLatch));
                }
            }
            // 等待所有分片任务下载完成
            countDownLatch.await();
            // 合并文件
            if (merge(fileSequences, file.getPath())) {
                // clearTemp(filePath);
                LogUtils.info("合并文件【{}】完成", fileName);
            }
            // 判断文件格式
//            if (url.endsWith(Constant.M3U8_SUFFIX_NAME)) {
//                M3U8 m3u8 = parseM3U8Url(url);
//                // 使用任务对象开始下载ts片段
//                List<String> list = m3u8.getTsList();
//                int segmentNum = list.size();
//
//                HomeJFrame.appendMessage("解析 m3u8 链接成功...");
//                LogUtils.info("加密方式： {}", m3u8.getMethod());
//                LogUtils.info("加密密钥： {}", m3u8.getKey());
//                LogUtils.info("IV偏移量： {}", m3u8.getIV());
//                LogUtils.info("共 {} 个ts片段", segmentNum);
//                countDownLatch = new CountDownLatch(segmentNum);
//                poolExecutor = new ThreadPoolExecutor(segmentNum, segmentNum, 0, TimeUnit.SECONDS,
//                        new ArrayBlockingQueue<>(segmentNum));
//                // 判断 .tmp 目录是否存在， 不存在则创建文件夹
//                File dir = new File(path + "/.tmp/");
//                // 不存在则先创建父文件夹
//                if (!dir.exists()) {
//                    LogUtils.info("新建文件夹{}", dir.getPath());
//                    dir.mkdirs();
//                }
//                for (int i = 0; i < list.size(); i++) {
//                    String tsUrl = list.get(i);
//                    String tsName = tsUrl.substring(tsUrl.lastIndexOf("/") + 1);
//                    String desPath = dir.getPath() + "/" + tsName;
//                    // 使用多线程下载所有的ts文件后，再合并文件
//                    poolExecutor.submit(new M3U8DownloaderTask(tsUrl, desPath, m3u8, i, countDownLatch));
//                }
//                // 等待分片任务下载完毕
//                countDownLatch.await();
//                // 合并文件
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
//                    HomeJFrame.appendMessage(tsName + "合并成功！");
//                }
//                LogUtils.info("合并文件【{}】完成", fileName);
//            } else if(contentLength < Constant.PART_NUM * Constant.BYTE_SIZE) {
//                // 文件大小小于50MB直接下载
//                countDownLatch = new CountDownLatch(1);
//                // 创建任务对象
//                DownloaderTask downloaderTask = new DownloaderTask(url, path, 0, -1, -1, downloadInfoThread, countDownLatch);
//                new Thread(new FutureTask<>(downloaderTask)).start();
//                countDownLatch.await();
//            } else {
//                // 文件分块, 计算切分后的文件每一块的大小
//                long size = contentLength / Constant.PART_NUM;
//                countDownLatch = new CountDownLatch(Constant.PART_NUM);
//                // 线程池对象
//                poolExecutor = new ThreadPoolExecutor(Constant.PART_NUM, Constant.PART_NUM, 0,
//                        TimeUnit.SECONDS, new ArrayBlockingQueue<>(Constant.PART_NUM));
//                // 下载每一块文件
//                for (int i = 0; i < Constant.PART_NUM; i++) {
//                    //计算下载起始位置
//                    long startPos = i * size;
//                    long endPos = startPos + size;
//                    // 如果不是第一块，起始位置要 +1
//                    if (startPos != 0) {
//                        startPos += 1;
//                    }
//                    // 计算结束位置
//                    if (i == Constant.PART_NUM - 1) {
//                        //下载最后一块，下载剩余的部分
//                        endPos = 0;
//                    }
//                    // 创建任务对象
//                    DownloaderTask downloaderTask = new DownloaderTask(url, path, startPos, endPos, i, downloadInfoThread, countDownLatch);
//                    // 将任务提交到线程池中
//                    poolExecutor.submit(downloaderTask);
//                }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            HomeJFrame.appendMessage(e.getMessage());
            return "下载失败";
        } finally {
            // 关闭连接对象
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
            // 关闭线程池
            if (poolExecutor != null) {
                poolExecutor.shutdown();
            }
            // 关闭
            scheduledExecutorService.shutdownNow();
        }
        HomeJFrame.appendMessage(fileName + "下载完成");
        return "下载完成";
    }

    /**
     * 解析m3u8链接
     * @param url
     * @return
     * @throws IOException
     */
    private static M3U8 parseM3U8Url(String url) throws IOException {
        M3U8 m3u8 = new M3U8();
        // 1.截取url设置根目录
        String basePath = url.substring(0, url.lastIndexOf("/") + 1);
        m3u8.setBasePath(basePath);
        // 2.读取m3u8文件的内容
        BufferedReader br = new BufferedReader(new InputStreamReader(HttpUtils.getHttpURLConnection(url).getInputStream()));
        String line;
        while ((line = br.readLine()) != null) {
            System.out.println(line);
            // 如果该文件含有该字段,则表示该m3u8文件没有资源,资源重定向到被指向的文件
            if (line.endsWith(Constant.M3U8_SUFFIX_NAME)) {
                LogUtils.info("m3u8文件重定向！！！");
                return parseM3U8Url(basePath + line);
            }
            // 包含此字段说明文件是加密的, METHOD参数表示加密方法，URI表示秘钥文件
            if(line.startsWith("#EXT-X-KEY")) {
                line = line.substring(line.indexOf(":") + 1);
                // METHOD=AES-128,URI="key.key"
                String[] params = line.split(",");
                for (String temp : params){
                    // 加密方法字段
                    if(temp.contains("METHOD") || temp.contains("method")){
                        String method = temp.split("=",2)[1];
                        m3u8.setMethod(method);
                    }
                    // 密钥
                    if(temp.contains("URI") || temp.contains("uri")){
                        String uri = temp.split("=",2)[1].replace("\"","");
                        String key;
                        // 获取key
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
                    // IV字段
                    if (temp.contains("IV") || temp.contains("iv")) {
                        String iv = temp.split("=",2)[1].replace("\"","");
                        m3u8.setIV(iv);
                    }
                }
            }
            // ts数据片
            if (line.endsWith(Constant.TS_SUFFIX_NAME)) {
                if (line.startsWith(Constant.HTTP_PROTOCOL_PREFIX) || line.startsWith(Constant.HTTPS_PROTOCOL_PREFIX)) {
                    m3u8.getTsList().add(line);
                }else {
                    m3u8.getTsList().add(m3u8.getBasePath() + line);
                }
            }
        }
        // 关流
        br.close();
        return m3u8;
    }

    /**
     * 连接到指定Url
     * @param url
     * @return
     * @throws IOException
     */
    private static String getKey(String url) throws IOException {
        // 网络输入流
        BufferedReader br = new BufferedReader(new InputStreamReader(HttpUtils.getHttpURLConnection(url).getInputStream()));
        String key = br.readLine();
        // 关闭输入流
        br.close();
        return key;
    }

    /**
     *  文件合并
     * @param fileName
     * @return
     */
    public boolean merge(List<FileSequence> fileSequences, String fileName) {
        // 单个文件无需合并
        if (fileSequences.size() <= 1) {
            return false;
        }
        LogUtils.info("开始合并文件{}", fileName);
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
            LogUtils.info("文件合并完毕{}", fileName);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

}

class FileSequence {
    // 远端路径
    private String url;
    // 本地路径
    private String localPath;
    // 文件片起始位置
    private long startPos;
    // 文件片结束位置
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
    // 存储文件的根路径
    private String basePath;
    // 解密算法名称
    private String method = "AES-128";
    // 密钥
    private String key;
    // 定义一个16byte的初始向量，如果m3u8没有IV值，则设置为0即可
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