package common;

public interface Constant {

    // 版本
    String VERSION = "1.0";
    // 页面字体大小
    int FONT_SIZE = 24;
    // http连接超时时间
    int TIME_OUT = 10 * 1000;
    // 文件片下载失败重试次数
    int TRY_TIME = 3;
    // MB单位换算
    double MB = 1024d * 1024d;
    // Buffer大小
    int BYTE_SIZE = 1024 * 10;
    // 配置文件相对路径
    String CONFIG_PATH = "./config.ini";
    // 保存路径 key
    String PATH_NAME_KEY = "savePath";

    // 文件片临时保存目录
    String TMP_PATH = "/.cache/";
    // 文件切分片数
    int PART_NUM = 5;
    // 文件片尾缀名
    String PART_NAME = ".part";
    // ts文件尾缀
    String TS_SUFFIX_NAME = ".ts";
    // mp4文件尾缀
    String MP4_SUFFIX_NAME = ".mp4";
    // m3u8文件尾缀
    String M3U8_SUFFIX_NAME = ".m3u8";
    // HTTP、HTTPS协议前缀
    String HTTP_PROTOCOL_PREFIX = "http://";
    String HTTPS_PROTOCOL_PREFIX = "https://";

    // 时间格式化格式
    String DATE_FORMAT_PATTERN = "MM/dd HH:mm:ss";
    // 时间格式文件名格式化格式
    String FILE_NAME_DATE_FORMAT_PATTERN = "yyyyMMddHHmmss";

    // User-Agent 请求头
    String USER_AGENT = "Mozilla/5.0 (Windows NT 6.1; WOW64) AppleWebKit/535.1 (KHTML, like Gecko) Chrome/14.0.835.163 Safari/535.1";

}
