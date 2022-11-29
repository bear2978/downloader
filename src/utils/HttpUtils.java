package utils;

import common.Constant;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

/**
 * http��ع�����
 */
public class HttpUtils {

    /**
     * ��ȡ�����ļ�������
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
            httpURLConnection.connect();
            // ����һ, ����Content-Disposition��Ӧͷ��ȡ�ļ���
            String disposeHead = httpURLConnection.getHeaderField("Content-Disposition");
            if (disposeHead != null && disposeHead.indexOf("=") > 0) {
                fileName = disposeHead.split("=")[1];
                fileName = new String(fileName.getBytes(StandardCharsets.ISO_8859_1), StandardCharsets.UTF_8);
                LogUtils.info("Content-Disposition ��ȡ���ļ����� {}", fileName);
                return fileName;
            }

            // �������� ���� Url ��� getFile() ��ȡ
            String newUrl = httpURLConnection.getURL().getFile();
            if (newUrl != null && newUrl.length() > 0) {
                newUrl = URLDecoder.decode(newUrl, StandardCharsets.UTF_8.toString());
                int pos = newUrl.indexOf('?');
                if (pos > 0) {
                    newUrl = newUrl.substring(0, pos);
                }
                pos = newUrl.lastIndexOf('/');
                fileName = newUrl.substring(pos + 1);
                LogUtils.info("URL.getFile() ��ȡ���ļ����� {}", fileName);
                return fileName;
            }
            // ��ȡMIME-Type
            MIMEType = HttpURLConnection.guessContentTypeFromStream(httpURLConnection.getInputStream());
            throw new IOException("��ȡ�ļ���ʧ�ܣ�");
        } catch (IOException e) {
            // �����쳣, ���ݵ�ǰʱ���MIME-Type�����ļ���
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
     * ��ȡ�����ļ���С
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
     * �ֿ�����
     * @param url      ���ص�ַ
     * @param startPos �����ļ���ʼλ��
     * @param endPos   �����ļ��Ľ���λ��
     * @return
     */
    public static HttpURLConnection getHttpURLConnection(String url, long startPos, long endPos) throws IOException {
        HttpURLConnection httpURLConnection = getHttpURLConnection(url);
        LogUtils.info("���ص������ǣ�{}-{}", startPos, endPos);

        if (endPos != 0) {
            httpURLConnection.setRequestProperty("RANGE", "bytes=" + startPos + "-" + endPos);
        } else {
            httpURLConnection.setRequestProperty("RANGE", "bytes=" + startPos + "-");
        }
        return httpURLConnection;
    }

    /**
     * ��ȡHttpURLConnection���Ӷ���
     * @param url �ļ��ĵ�ַ
     * @return
     */
    public static HttpURLConnection getHttpURLConnection(String url) throws IOException {
        URL httpUrl = new URL(url);
        HttpURLConnection httpURLConnection = (HttpURLConnection) httpUrl.openConnection();
        httpURLConnection.setConnectTimeout(Constant.TIME_OUT);
        //���ļ����ڵķ��������ͱ�ʶ��Ϣ
        httpURLConnection.setRequestProperty("User-Agent", Constant.USER_AGENT);
        return httpURLConnection;
    }

}
