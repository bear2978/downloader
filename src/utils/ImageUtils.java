package utils;

import javax.swing.*;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * ����ͼƬ�����࣬ʵ�ּ��ع���ͼƬ�����´�ֱ�ӻ�ȡ
 * @author zyj
 */
public class ImageUtils {

    // ����Map���ϣ�����ͼƬ·���� ֵ ͼƬ�ؼ���
    public static Map<String, ImageIcon> mapImage = new HashMap<>();

    /**
     * ��ȡͼƬ�ؼ�
     *
     * */
    public static ImageIcon getImageIcon(String path) {
        //�ж�ͼƬ�Ƿ���ع�
        if(mapImage.containsKey(path)) {    //���ع���ͼƬ����ֱ�ӷ���ͼƬ�ؼ�
            return mapImage.get(path);
        }
        // û�м��ع���ͼƬ
        ImageIcon image = new ImageIcon(path);
        mapImage.put(path, image);
        return image;
    }

    /**
     * ��ȡָ����С��ͼƬ�ؼ�
     * @param path
     * @param width
     * @param height
     */
    public static ImageIcon getImageIcon(String path, int width, int height) {
        // ����Image�ؼ��ķ���
        Image image = getImageIcon(path).getImage();
        Image scaleImage = image.getScaledInstance(width, height, Image.SCALE_DEFAULT);
        // ͼ��ؼ����ͼƬ�ؼ�
        return new ImageIcon(scaleImage);
    }

    /**
     * ����ͼƬ�����󣬷������ź��ͼƬ�ؼ�
     * @param image
     * @param scale
     */
    public static ImageIcon getScaleImageIcon(Image image, double scale) {
        // ����Image�ؼ��ķ���
        int newWidth = (int) (image.getWidth(null) * scale);
        int newHigt = (int) (image.getHeight(null) * scale);
        Image scaleImage = image.getScaledInstance(newWidth, newHigt, Image.SCALE_DEFAULT);
        //ͼ��ؼ����ͼƬ�ؼ�
        return new ImageIcon(scaleImage);
    }

}