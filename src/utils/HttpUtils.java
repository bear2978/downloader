package utils;

import common.Constant;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * http相关工具类
 */
public class HttpUtils {

    private final static List<Integer> redirectCodes = new ArrayList<>();

    static {
        // 300 - 305 code
        redirectCodes.add(HttpURLConnection.HTTP_MULT_CHOICE);
        redirectCodes.add(HttpURLConnection.HTTP_MOVED_PERM);
        redirectCodes.add(HttpURLConnection.HTTP_MOVED_TEMP);
        redirectCodes.add(HttpURLConnection.HTTP_SEE_OTHER);
        redirectCodes.add(HttpURLConnection.HTTP_NOT_MODIFIED);
        redirectCodes.add(HttpURLConnection.HTTP_USE_PROXY);
    }

    /**
     * 获取下载文件的名字
     * @param url
     * @return
     */
    public static String getHttpFileName(String url) {
        String fileName = null;
        String MIMEType = null;
        HttpURLConnection httpURLConnection = null;
        try {
            httpURLConnection = getHttpURLConnection(url);
            httpURLConnection.setRequestMethod("GET");
            httpURLConnection.setInstanceFollowRedirects(false);
            httpURLConnection.connect();
            // 连接不是真实地址，被重定向到另一个地址
            if (redirectCodes.contains(httpURLConnection.getResponseCode())) {
                String location = httpURLConnection.getHeaderField("Location");
                LogUtils.info("Location >> {}", location);
                if (StringUtils.isNotEmpty(location)) {
                    return getHttpFileName(location);
                }
            }

            System.out.println("Headers: \n" + httpURLConnection.getHeaderFields().toString());
            // 方法一, 根据Content-Disposition响应头获取文件名
            String disposeHead = httpURLConnection.getHeaderField("Content-Disposition");
            if (disposeHead != null && disposeHead.indexOf("=") > 0) {
                String[] disposeHeads = disposeHead.split(";");
                for (String dispose : disposeHeads){
                    String key = dispose.split("=")[0];
                    if (key.equals("filename") || key.equals("filename*")) {
                        fileName = dispose.split("=")[1].trim();
                        fileName = URLDecoder.decode(new String(fileName.getBytes(StandardCharsets.ISO_8859_1),
                                StandardCharsets.UTF_8), StandardCharsets.UTF_8.toString());
                        fileName = fileName.replace("UTF-8''", "");
                    }
                }
                LogUtils.info("Content-Disposition 获取到文件名： {}", fileName);
                return fileName;
            }

            // 方法二， 根据 Url 类的 getFile() 获取
            String newUrl = httpURLConnection.getURL().getFile();
            if (newUrl != null && newUrl.length() > 0) {
                newUrl = URLDecoder.decode(newUrl, StandardCharsets.UTF_8.toString());
                int pos = newUrl.indexOf('?');
                if (pos > 0) {
                    newUrl = newUrl.substring(0, pos);
                }
                pos = newUrl.lastIndexOf('/');
                fileName = newUrl.substring(pos + 1);
                LogUtils.info("URL.getFile() 获取到文件名： {}", fileName);
            }
            if (StringUtils.isEmpty(fileName)) {
                // 获取MIME-Type
                MIMEType = HttpURLConnection.guessContentTypeFromStream(httpURLConnection.getInputStream());
                throw new IOException("获取文件名失败！");
            }
        } catch (IOException e) {
            // 出现异常, 根据当前时间和MIME-Type构建文件名
            fileName = StringUtils.formatTime(Constant.FILE_NAME_DATE_FORMAT_PATTERN);
            if (MIMEType != null) {
                fileName += "." + MIMEType.substring(MIMEType.lastIndexOf("/") + 1);
            }
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
        return fileName;
    }

    /**
     * 获取下载文件大小
     * @param url
     * @return
     * @throws IOException
     */
    public static long getHttpFileContentLength(String url) throws IOException {
        int contentLength;
        HttpURLConnection httpURLConnection = null;
        try {
            httpURLConnection = getHttpURLConnection(url);
            contentLength = httpURLConnection.getContentLength();
        } finally {
            if (httpURLConnection != null) {
                httpURLConnection.disconnect();
            }
        }
        return contentLength;
    }

    /**
     * 分块下载
     * @param url      下载地址
     * @param startPos 下载文件起始位置
     * @param endPos   下载文件的结束位置
     * @return
     */
    public static HttpURLConnection getHttpURLConnection(String url, long startPos, long endPos) throws IOException {
        HttpURLConnection httpURLConnection = getHttpURLConnection(url);
        LogUtils.info("下载的区间是：{}-{}", startPos, endPos);

        if (endPos != 0) {
            httpURLConnection.setRequestProperty("RANGE", "bytes=" + startPos + "-" + endPos);
        } else {
            httpURLConnection.setRequestProperty("RANGE", "bytes=" + startPos + "-");
        }
        return httpURLConnection;
    }

    /**
     * 获取HttpURLConnection链接对象
     * @param url 文件的地址
     * @return
     */
    public static HttpURLConnection getHttpURLConnection(String url) throws IOException {
        URL httpUrl = new URL(url);
        HttpURLConnection httpURLConnection = (HttpURLConnection) httpUrl.openConnection();
        httpURLConnection.setConnectTimeout(Constant.TIME_OUT);
        //向文件所在的服务器发送标识信息
        httpURLConnection.setRequestProperty("User-Agent", Constant.USER_AGENT);
        return httpURLConnection;
    }

}
