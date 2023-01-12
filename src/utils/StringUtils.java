package utils;

import common.Constant;

import java.text.SimpleDateFormat;
import java.util.Date;

public class StringUtils {

    // 时间格式化
    private static final SimpleDateFormat simpleDateFormat = new SimpleDateFormat(Constant.DATE_FORMAT_PATTERN);
    // 文件名格式化
    private static final SimpleDateFormat fileNameFormat = new SimpleDateFormat(Constant.FILE_NAME_DATE_FORMAT_PATTERN);

    public static boolean isNotEmpty(String str){
        return !isEmpty(str);
    }

    public static boolean isEmpty(String str) {
        return str == null || "".equals(str.trim());
    }


    public static String formatTime(String pattern) {
        if (Constant.DATE_FORMAT_PATTERN.equals(pattern)) {
            return simpleDateFormat.format(new Date());
        }else if (Constant.FILE_NAME_DATE_FORMAT_PATTERN.equals(pattern)) {
            return fileNameFormat.format(new Date());
        }
        return new SimpleDateFormat(pattern).format(new Date());
    }

}
