package utils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * 加载图片工具类，实现加载过的图片可以下次直接获取
 * @author zyj
 */
public class ImageUtils {

    // 定义Map集合（键：图片路径， 值 图片控件）
    public static Map<String, ImageIcon> mapImage = new HashMap<>();

    /**
     * 获取图片控件
     *
     * */
    public static ImageIcon getImageIcon(String path) {
        //判断图片是否加载过
        if(mapImage.containsKey(path)) {    //加载过的图片，就直接返回图片控件
            return mapImage.get(path);
        }
        // 没有加载过的图片
        ImageIcon image = new ImageIcon(path);
        mapImage.put(path, image);
        return image;
    }

    /**
     * 获取指定大小的图片控件
     * @param path
     * @param width
     * @param height
     */
    public static ImageIcon getImageIcon(String path, int width, int height) {
        // 调用Image控件的方法
        Image image = getImageIcon(path).getImage();
        Image scaleImage = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        // 图标控件里放图片控件
        return new ImageIcon(scaleImage);
    }

    /**
     * 缩放图片比例后，返回缩放后的图片控件
     * @param image
     * @param scale
     */
    public static ImageIcon getScaleImageIcon(Image image, double scale) {
        // 调用Image控件的方法
        int newWidth = (int) (image.getWidth(null) * scale);
        int newHeigt = (int) (image.getHeight(null) * scale);
        Image scaleImage = image.getScaledInstance(newWidth, newHeigt, Image.SCALE_DEFAULT);
        //图标控件里放图片控件
        return new ImageIcon(scaleImage);
    }

}