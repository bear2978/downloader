package utils;

import common.Constant;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.security.spec.AlgorithmParameterSpec;
import java.util.Properties;

public class FileUtils {

//    static {
//        // 解决java不支持AES/CBC/PKCS7Padding模式解密
//        Security.addProvider(new BouncyCastleProvider());
//    }

    /**
     * 获取本地文件的大小
     */
    public static long getFileContentLength(String path) {
        File file = new File(path);
        return file.exists() && file.isFile() ? file.length() : 0;
    }

    public static long getFileContentLength(String path, String fileName) {
        File file = new File(path, fileName);
        return getFileContentLength(file.getPath());
    }


    /**
     * 解密ts文件流
     * @param src 加密前的ts文件字节数组
     * @param method 加密方法
     * @param key 密钥
     * @param IV
     * @return 解密后的字节数组
     */
    public static byte[] decrypt(byte[] src, String method, String key, String IV) {
        if (StringUtils.isEmpty(method) && !method.contains("AES")) {
            throw new RuntimeException("未知的算法");
        }
        // key为空表示没有加密
        if (StringUtils.isEmpty(key)) {
            return src;
        }
        // 将key处理为16位
        if (key.length() > 16) {
            key = key.substring(0, 17);
        }
        try {
            // Cipher cipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
            Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
            // 设置编码及解密方式
            SecretKeySpec keySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), method);
            // 如果m3u8有IV标签,那么IvParameterSpec构造函数就把IV标签后的内容转成字节数组传进去
            AlgorithmParameterSpec paramSpec = new IvParameterSpec(IV.getBytes());
            cipher.init(Cipher.DECRYPT_MODE, keySpec, paramSpec);
            return cipher.doFinal(src);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("解密出错");
        }
    }

    /**
     * 删除文件或者一个目录下所有文件
     * @param dir
     */
    public static void deleteDir(File dir) {
        //（不包括隐藏文件）
        if (dir.isDirectory()){
            File[] list = dir.listFiles();
            if(list != null && list.length != 0) {
                // 如果文件夹为空，删除该文件夹
                for (int i = 0; i < list.length; i++) {
                    deleteDir(list[i]);
                }
            }
        } else {
            // 如果文件大小为0则删除（不包括隐藏文件）
            if (dir.isFile()) {
                dir.delete();
            }
        }
    }

    /**
     * 获取配置文件内容
     * @param cfgPath 配置文件路径
     * @param key 键
     */
    public static String getConfig(String cfgPath, String key) {
        try (
                FileInputStream reader = new FileInputStream(cfgPath);
        ) {
            Properties pro = new Properties();
            pro.load(reader);
            return pro.getProperty(key);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 设置配置文件内容
     * @param cfgPath 配置文件路径
     * @param key 键
     * @param value 值
     */
    public static boolean setConfig(String cfgPath, String key, String value) {
        try (
                FileOutputStream reader = new FileOutputStream(cfgPath);
        ) {
            Properties pro = new Properties();
            pro.put(key, value);
            pro.store(reader, "修改保存路径");
            return true;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

}
