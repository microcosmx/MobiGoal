/**
 * 
 */
package cn.edu.fudan.se.mobigoal.support;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

import cn.edu.fudan.se.R;
import cn.edu.fudan.se.agent.main.AideAgentInterface;
import cn.edu.fudan.se.agent.support.ACLMC_DelegateTask;
import cn.edu.fudan.se.mobigoal.initial.SGMApplication;
import cn.edu.fudan.se.mobigoal.maincontainer.MainActivity;
import cn.edu.fudan.se.sgm.goalmodel.EncodeDecodeRequestData;
import cn.edu.fudan.se.sgm.goalmodel.RequestData;

/**
 * 拍照，在用户收到一个<code>UserTakePictureTask</code>任务后，点击上面的camera按钮会跳转到这个activity
 * 
 * @author whh
 * 
 */
public class TakePictureActivity extends Activity {

	private ImageView iv_show_pic;
	private Button bt_take_pic, bt_ok;

	private Bitmap bitmap;

	private AideAgentInterface aideAgentInterface; // agent interface
	private String goalModelName, elementName;
	private String fromAgentName, requestDataName;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_takepicture);

		aideAgentInterface = GetAgent
				.getAideAgentInterface((SGMApplication) this.getApplication());

		Intent intent = getIntent();
		fromAgentName = intent.getStringExtra("fromAgentName");
		goalModelName = intent.getStringExtra("goalmodelname");
		elementName = intent.getStringExtra("elementname");
		requestDataName = intent.getStringExtra("requestDataName");

		iv_show_pic = (ImageView) findViewById(R.id.iv_show_pic);
		bt_take_pic = (Button) findViewById(R.id.bt_take_pic);
		bt_ok = (Button) findViewById(R.id.bt_save_pic);

		bt_take_pic.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
				startActivityForResult(intent, 1);
			}
		});

		bt_ok.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				savePicToLocal(bitmap);
				sendPicToAgent(bitmap);

				Intent intent = new Intent();
				intent.setClass(TakePictureActivity.this, MainActivity.class);
				startActivity(intent);
			}
		});

		bt_ok.setClickable(false);
		bt_ok.setTextColor(getResources().getColor(R.color.unclickable_grey));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (resultCode == Activity.RESULT_OK) {
			Bundle bundle = data.getExtras();
			bitmap = (Bitmap) bundle.get("data");// 获取相机返回的数据，并转换为Bitmap图片格式
			iv_show_pic.setImageBitmap(bitmap);
			bt_ok.setClickable(true);
			bt_ok.setTextColor(getResources().getColor(R.color.clickable_black));
		}
	}

	/**
	 * 把图片保存到本地
	 * 
	 * @param bitmap
	 */
	private void savePicToLocal(Bitmap bitmap) {
		System.out.println("MY_LOG-TakePictureActivity--savePicToLocal()");

		String picName = new SimpleDateFormat("yyyyMMddhhmmss")
				.format(new Date()) + ".jpg";

		File picture = new File(Environment.getExternalStorageDirectory()
				+ "/sgm/pic/" + picName);

		try {
			// 保存图片到本地
			FileOutputStream fos = new FileOutputStream(picture.getPath());
			// 把数据写入文件, 0表示压缩后质量最差，100为最好
			bitmap.compress(Bitmap.CompressFormat.JPEG, 50, fos);
			fos.flush();
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/**
	 * 告诉agent拍照任务做完了，让其通知相关的element machine，同时把Image数据传回去
	 * 
	 * @param bitmap
	 *            图片数据
	 */
	private void sendPicToAgent(Bitmap bitmap) {
		System.out.println("MY_LOG-TakePictureActivity--sendPicToAgent()");

		ACLMC_DelegateTask aclmc_DelegateTask = new ACLMC_DelegateTask(
				ACLMC_DelegateTask.DTHeader.DTBACK, null, fromAgentName,
				goalModelName, elementName);
		aclmc_DelegateTask.setDone(true);
		RequestData requestData = new RequestData(requestDataName, "Image");
		requestData.setContent(EncodeDecodeRequestData.encodeBitmap(bitmap));
		aclmc_DelegateTask.setRetRequestData(requestData);
		aideAgentInterface.sendMesToExternalAgent(aclmc_DelegateTask);
	}

}
