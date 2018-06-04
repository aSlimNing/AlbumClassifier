package org.tensorflow.photoclassifier.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.demo.R;
import org.tensorflow.photoclassifier.dao.DataBaseOperator;
import org.tensorflow.photoclassifier.config.ClassifierConfig;
import org.tensorflow.photoclassifier.logic.WelcomeLogic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import static org.tensorflow.photoclassifier.logic.WelcomeLogic.MSG_CLASSIF_IMAGE_VIA_TF;
import static org.tensorflow.photoclassifier.logic.WelcomeLogic.MSG_PREPARE_WORK_FINISH;
import static org.tensorflow.photoclassifier.logic.WelcomeLogic.MSG_RREPARE_TENSORFLOW;
import static org.tensorflow.photoclassifier.logic.WelcomeLogic.MSG_SCAN_IMAGE_BEGIN;
import static org.tensorflow.photoclassifier.logic.WelcomeLogic.MSG_SCAN_IMAGE_FINISH;

/**
 * Created by hzzhuangning.
 */

public class WelcomeActivity extends AppCompatActivity {
    // for permission
    private static final int PERMISSION_REQUEST_STORAGE = 200;
    public static final int AFTER_ALL_CLASSIFIED = 0;
    public static final int AFTER_SCAN_AND_CLEAR_DB = 1;
    public static final int DEPEND_ON_THE_NUMBER = 2;

    private WelcomeLogic mWelcomeLogic;

    private DataBaseOperator mOperator;

    private int i = 0;
    private int size = 0;
    private TextView vWorkProcess = null;
    private TextView vTitle = null;
    private ProgressBar vProgressBar;
    private final String[] actions = {
            "全部进入APP时处理", "全部后台处理", "根据图片数量决定"};

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // scanning images
                case MSG_SCAN_IMAGE_BEGIN:
                    Log.d("MESSAGE", "MSG_SCAN_IMAGE_BEGIN");
                    if (vWorkProcess != null)
                        vWorkProcess.setText("\n正在扫描图片 ");
                    break;
                // scanning images done
                case MSG_SCAN_IMAGE_FINISH:
                    do_afterScanImage();
                    break;
                // classifying image with tf
                //
                case MSG_RREPARE_TENSORFLOW:
                    vWorkProcess.setText("正在准备...");
                    break;
                case MSG_CLASSIF_IMAGE_VIA_TF:
                    i++;
                    if (vWorkProcess != null)
                        vWorkProcess.setText("\n正在处理图片 " + i + "/" + size);
                    break;
                // this activity will be finished
                case MSG_PREPARE_WORK_FINISH:
                    do_finishThisActivity();
                    break;
            }
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setContentView(R.layout.activity_welcome);
        vWorkProcess = (TextView) findViewById(R.id.work_process);
        vTitle = (TextView) findViewById(R.id.app_title);
        vProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        vProgressBar.setVisibility(vProgressBar.GONE);
        prepareLogic();
        setAppName();
        checkPermission();
    }

    private void prepareLogic() {
        if(mWelcomeLogic == null){
            mWelcomeLogic = new WelcomeLogic(this,mHandler);
        }
    }

    private void checkPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            // check permission
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                            != PackageManager.PERMISSION_GRANTED) {
                // require permission for wr
                ActivityCompat.requestPermissions(this, new String[]{
                        Manifest.permission.WRITE_EXTERNAL_STORAGE,
                        Manifest.permission.SYSTEM_ALERT_WINDOW,
                }, PERMISSION_REQUEST_STORAGE);
            } else {
                prepareForApplication();
            }
        } else {
            prepareForApplication();
        }
    }

    /**
     * call back after require permission
     * @param requestCode
     * @param permissions
     * @param grantResults
     */
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        doAfterRequestPermission(requestCode, grantResults);
    }

    /**
     * check permissiont again
     * @param requestCode
     * @param grantResults
     */
    private void doAfterRequestPermission(int requestCode, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_STORAGE) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                vProgressBar.setVisibility(vProgressBar.VISIBLE);
                mHandler.sendEmptyMessage(0x3);
                prepareForApplication();
            } else {
                Toast.makeText(WelcomeActivity.this,
                        "对不起，不能访问存储卡我不能继续工作！",
                        Toast.LENGTH_LONG).show();
                Timer timer = new Timer();
                TimerTask task = new TimerTask() {
                    @Override
                    public void run() {
                        WelcomeActivity.this.finish();
                    }
                };
                timer.schedule(task, 1000 * 2);
            }
        }
    }

    /**
     * to search and handle the images in device
     */
    private void prepareForApplication() {
        new Thread(new Runnable() {
            @Override
            public void run() {
                Looper.prepare();
                mWelcomeLogic.collectImagesAndHandle();
                Looper.loop();
            }
        }).start();
    }

    /**
     * confirm what level you like to use or do something by level
     */
    private void do_afterScanImage() {
        Log.d("MESSAGE", "0x1");
        //todo 将这个存在数据库当中的setting，改为存在sp文件当中，并在application当中初始化
        final DataBaseOperator operator = new DataBaseOperator(WelcomeActivity.this, ClassifierConfig.DB_NAME, ClassifierConfig.dbversion);
        List<Map> findResult = operator.search("Settings");

        try {
            // not first open the application
            String tmp = (String) findResult.get(0).get("notFirstIn");
            int level = Integer.parseInt((String) findResult.get(0).get("updateTime"));
            Log.d("LEVEL", "" + level);
            do_byLevel(level);
        } catch (Exception e) {
            // is first open this application

            AlertDialog.Builder builder = new AlertDialog.Builder(WelcomeActivity.this);
            builder.setTitle("选择图片处理的时间");
            builder.setIcon(R.drawable.things);
            builder.setItems(actions, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    final DataBaseOperator operator = new DataBaseOperator(WelcomeActivity.this, ClassifierConfig.DB_NAME, ClassifierConfig.dbversion);
                    ContentValues values = new ContentValues();
                    values.put("notFirstIn", "true");
                    values.put("updateTime", which);
                    operator.insert("Settings", values);
                    operator.close();
                    Toast.makeText(WelcomeActivity.this, actions[which], Toast.LENGTH_SHORT).show();
                    do_byLevel(which);
                }
            });

            builder.show();
        }
        operator.close();
    }

    /**
     * when to classify images? you have three choices:
     * 1. when open app && when all images have been classified -> goto MainActivity
     * 2. when open app, just only scan picture and clear app'db, classifying images in background
     * 3. if image is not too much, goto MainActivity until new images have been classified, or do
     * it as choice 2
     *
     * @param level 1, 2 or 3
     */
    private void do_byLevel(int level) {
        List<String> notBeClassifiedImages = mWelcomeLogic.getNotBeClassifiedImages();
        if (level == AFTER_ALL_CLASSIFIED) {
            mWelcomeLogic.classifyNewImages();
        } else if (level == AFTER_SCAN_AND_CLEAR_DB) {
            ClassifierConfig.needToBeClassified = notBeClassifiedImages;
            do_finishThisActivity();
        } else if (level == DEPEND_ON_THE_NUMBER) {
            if (notBeClassifiedImages.size() <= ClassifierConfig.imageNumber) {
                mWelcomeLogic.classifyNewImages();
            } else {
                ClassifierConfig.needToBeClassified = new ArrayList<>();
                ClassifierConfig.needToBeClassified.addAll(notBeClassifiedImages.subList(ClassifierConfig.imageNumber, notBeClassifiedImages.size()));
                mWelcomeLogic.classifyNewImages();
            }
        }
    }

    /**
     * when handler get the message that 'tf is end', the do this function to update UI,
     * goto MainActivity and finish this activity
     */
    private void do_finishThisActivity() {
        vProgressBar.setVisibility(View.GONE);
        vWorkProcess.setText("尽情享受吧");
        //setAppName();
//        final Intent it = new Intent(getApplication(), MainActivity.class); //你要转向的Activity
//        Timer timer = new Timer();
//        TimerTask task = new TimerTask() {
//            @Override
//            public void run() {
//                startActivity(it);
//                WelcomeActivity.this.finish();
//            }
//        };
//        timer.schedule(task, 1000 * 2);
    }

    private void setAppName() {
        vTitle.setText("相册分类");
        vTitle.setTextSize(32);
        vTitle.setTextColor(Color.rgb(140, 21, 119));
    }

}