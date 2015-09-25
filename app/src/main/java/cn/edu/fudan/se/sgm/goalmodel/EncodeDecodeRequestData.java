/**
 * 
 */
package cn.edu.fudan.se.sgm.goalmodel;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * 对<code>RequestData</code>的content的编码、解码操作
 * 
 * @author whh
 * 
 */
public class EncodeDecodeRequestData {

	/**
	 * 读取文件输入流，编码成byte[]格式
	 * 
	 * @param is
	 *            input stream
	 * @return byte[]数据
	 */
	public static byte[] encodeInputStream(InputStream is) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int length = -1;
		try {
			while ((length = is.read(buffer)) != -1) {
				baos.write(buffer, 0, length);
			}
			baos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		byte[] data = baos.toByteArray();
		try {
			is.close();
			baos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return data;
	}

	/**
	 * 将Bitmap转换成byte[]
	 * 
	 * @param bitmap
	 * @return
	 */
	public static byte[] encodeBitmap(Bitmap bitmap) {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
		return baos.toByteArray();
	}

	/**
	 * 将byte[]解码成文本信息
	 * 
	 * @param content
	 *            要解码的内容
	 * @return String
	 */
	public static String decodeToText(byte[] content) {
		return new String(content);
	}

	/**
	 * 将byte[]解码成图片信息
	 * 
	 * @param content
	 *            要解码的内容
	 * @return Bitmap
	 */
	public static Bitmap decodeToBitmap(byte[] content) {
		return BitmapFactory.decodeByteArray(content, 0, content.length);
	}

}
