package common;

public interface Constant {

    // �汾
    String VERSION = "1.0";
    // http���ӳ�ʱʱ��
    int TIME_OUT = 10 * 1000;
    // MB��λ����
    double MB = 1024d * 1024d;
    // Buffer��С
    int BYTE_SIZE = 1024 * 10;
    // �����ļ����·��
    String CONFIG_PATH = "./src/resource/config.ini";
    // ����·�� key
    String PATH_NAME_KEY = "savePath";

    // �ļ�Ƭ��ʱ����Ŀ¼
    String TMP_PATH = "/.tmp/";
    // �ļ��з�Ƭ��
    int PART_NUM = 5;
    // �ļ�Ƭ��
    String PART_NAME = "part";

    // ts�ļ�β׺
    String TS_SUFFIX_NAME = ".ts";
    // mp4�ļ�β׺
    String MP4_SUFFIX_NAME = ".mp4";
    // m3u8�ļ�β׺
    String M3U8_SUFFIX_NAME = ".m3u8";
    // HTTP��HTTPSЭ��ǰ׺
    String HTTP_PROTOCOL_PREFIX = "http://";
    String HTTPS_PROTOCOL_PREFIX = "https://";

    // ʱ���ʽ����ʽ
    String DATE_FORMAT_PATTERN = "MM/dd HH:mm:ss";
    // ʱ���ʽ�ļ�����ʽ����ʽ
    String FILE_NAME_DATE_FORMAT_PATTERN = "yyyyMMddHHmmss";

    // User-Agent ����ͷ
    String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.163 Safari/535.1";

}
