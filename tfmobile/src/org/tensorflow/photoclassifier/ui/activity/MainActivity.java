package org.tensorflow.photoclassifier.ui.activity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.ClipData;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.ParcelFileDescriptor;
import android.provider.Contacts;
import android.provider.MediaStore;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.tensorflow.demo.R;
import org.tensorflow.photoclassifier.Classifier;
import org.tensorflow.photoclassifier.classifier.TFClassifierInstance;
import org.tensorflow.photoclassifier.classifier.TensorFlowImageClassifier;
import org.tensorflow.photoclassifier.config.ClassifierConfig;
import org.tensorflow.photoclassifier.logic.MainLogic;
import org.tensorflow.photoclassifier.ui.Fragment.AlbumsFragment;
import org.tensorflow.photoclassifier.ui.Fragment.PhotoFragment;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

/**
 * created by dongchangzhang
 */

public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    public static final int SCAN_IMAGES = 0x11;
    //UI Object
    private TextView vTvPhotos;
    private TextView vTvAlbums;

    //Fragment Object
    private PhotoFragment mPhotos;
    private AlbumsFragment mAlbumFragment;
    private FragmentManager mFragmentManager;
    private static final int PERMISSION_REQUEST_CAMERA = 300;

    // for camera to save image
    private Uri contentUri;
    private File newFile;

    // notification
    private int NOTI_CODE_HAVE_NEW = 1;
    private int NOTI_CODE_CLASSIFYING = 2;
    private int NOTI_CODE_FINISHED = 3;
    private int NOTI_CODE_NEW_PHOTO = 4;

    private NotificationManager mNotificationManager;

    public  android.support.v7.app.ActionBar actionBar;

    // fragment
    private FragmentTransaction fTransaction;
    private boolean havaInAlbum = false;

    //Logic
    private MainLogic mMainLogic;


    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                // scanning images
                case SCAN_IMAGES:
                    mAlbumFragment.onRefresh();
                    break;

            }
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        // fragment
        mFragmentManager = getFragmentManager();
        actionBar = getSupportActionBar();
        bindViews();
        vTvPhotos.performClick();
        prepareLogic();
        mMainLogic.classifyImagesAtBackground();
    }

    private void prepareLogic() {
        mMainLogic = new MainLogic(this,mHandler);
    }

    private void showNotification(String title, String message, int code) {
        // show notification about tf information of image
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentTitle(title)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setContentText(message);
        mBuilder.setFullScreenIntent(null, true);
        mBuilder.setAutoCancel(true);

        mNotificationManager.notify(code, mBuilder.build());
    }

    private void showNotification(int now, int sum, int code) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
        builder.setSmallIcon(R.mipmap.ic_launcher);
        builder.setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.camera));
        //禁止用户点击删除按钮删除
        builder.setAutoCancel(true);
        //禁止滑动删除
        builder.setOngoing(false);
        //取消右上角的时间显示
        builder.setShowWhen(true);
        if (now != sum)
            builder.setContentTitle("正在新处理图片...   " + now + "/" + sum);
        else {
            builder.setContentTitle("图片处理完成");
            showNotification("图片处理完成", "享受您的精彩之旅吧！", NOTI_CODE_FINISHED);
        }
        builder.setProgress(sum, now, false);
        //builder.setContentInfo(progress+"%");
        builder.setOngoing(true);
        builder.setShowWhen(false);
        //Intent intent = new Intent(this,DownloadService.class);
        //intent.putExtra("command",1);
        Notification notification = builder.build();
        NotificationManager manger = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        mNotificationManager.notify(code, notification);
    }

    // classify images at background
    // 这里开始才真正的开始对照片进行分类，在@WelComeActivity当中，对已经分类并存入数据库的照片以及系统相册中的照片进行了扫描。
    // 存入config当中。在这里进行具体的分类，打上Tag。
    private void classifyImagesAtBackground() {

    }


    /**
     * ActionBar
     *
     * @param menu
     * @return
     */

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        // search item
//        MenuItem searchItem = menu.findItem(R.id.action_search);
        return super.onCreateOptionsMenu(menu);
    }


    /**
     * 监听菜单栏目的动作，当按下不同的按钮执行相应的动作
     *
     * @param item
     * @return
     */

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // 返回
                System.out.println("title");
                getFragmentManager().popBackStack();
                break;
//            case R.id.action_search:
//                // 搜索
//               System.out.println("search");
//                break;
            case R.id.action_camera:
                // 拍照
                if (Build.VERSION.SDK_INT >= 23) {
                    if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                            != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(this,
                                new String[]{Manifest.permission.CAMERA},
                                PERMISSION_REQUEST_CAMERA);
                    } else {
                        startCamera();
                    }
                } else {
                    startCamera();
                }
                break;
            case R.id.action_voice:
//                Intent intent = new Intent(MainActivity.this, MapActivity.class);

//                startActivity(intent);
                break;
            // 语音
            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }


    /**
     * do it after require permission
     *
     * @param requestCode
     * @param permissions
     * @param grantResults
     */

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        doNext(requestCode, grantResults);
    }


    /**
     * if have permission will do this, or show a toast
     *
     * @param requestCode
     * @param grantResults
     */

    private void doNext(int requestCode, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CAMERA) {
            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startCamera();
            } else {
                Toast.makeText(MainActivity.this,
                        "对不起，我需要相机的权限！",
                        Toast.LENGTH_LONG).show();
            }
        }
    }

/**
     * 打开相机获取图片
     */

    private void startCamera() {
        File imagePath = new File(Environment.getExternalStorageDirectory(), "tmp");
        if (!imagePath.exists()) imagePath.mkdirs();
        newFile = new File(imagePath, "default_image.jpg");
        //第二参数是在manifest.xml定义 provider的authorities属性
        contentUri = FileProvider.getUriForFile(this, "com.fghz.album.fileprovider", newFile);
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        //兼容版本处理，因为 intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION) 只在5.0以上的版本有效
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            ClipData clip = ClipData.newUri(getContentResolver(), "A photo", contentUri);
            intent.setClipData(clip);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
        } else {
            List<ResolveInfo> resInfoList =
                    getPackageManager()
                            .queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
            for (ResolveInfo resolveInfo : resInfoList) {
                String packageName = resolveInfo.activityInfo.packageName;
                grantUriPermission(packageName, contentUri,
                        Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            }
        }
        intent.putExtra(MediaStore.EXTRA_OUTPUT, contentUri);
        startActivityForResult(intent, 1);
    }

    // 接受拍照的结果
    @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            ContentResolver contentProvider = getContentResolver();
            ParcelFileDescriptor mInputPFD;
            try {
                //获取contentProvider图片
                mInputPFD = contentProvider.openFileDescriptor(contentUri, "r");
                final FileDescriptor fileDescriptor = mInputPFD.getFileDescriptor();

                // new thread to deal image by tensorflow
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        mMainLogic.dealPics(fileDescriptor);
                    }
                }).start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    //UI组件初始化与事件绑定
    private void bindViews() {
        // 定位textview
        vTvPhotos = (TextView) findViewById(R.id.all_photos);
        vTvAlbums = (TextView) findViewById(R.id.all_albums);
        // 对其设置监听动作
        vTvPhotos.setOnClickListener(this);
        vTvAlbums.setOnClickListener(this);
    }

    //重置所有文本的选中状态为未点击状态
    private void setSelected() {
        vTvPhotos.setSelected(false);
        vTvAlbums.setSelected(false);
    }

    //隐藏所有Fragment
    private void hideAllFragment(FragmentTransaction fragmentTransaction) {
        if (mPhotos != null) fragmentTransaction.hide(mPhotos);
        if (mAlbumFragment != null) fragmentTransaction.hide(mAlbumFragment);
    }


/**
     * 监听textview的按钮事件
     *
     * @param v
     */


    @Override
    public void onClick(View v) {

        fTransaction = mFragmentManager.beginTransaction();
        hideAllFragment(fTransaction);
        switch (v.getId()) {
            // 照片

            case R.id.all_photos:
                // set ActionBar tile && set no click action
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setTitle("照片");
                setSelected();
                vTvPhotos.setSelected(true);
                // 暂时使用弹出堆栈，以避免从相簿进入相册无法返回
                // 可以使用其他方法，这个方法不好，下面相同
                getFragmentManager().popBackStack();
                if (mPhotos == null) {
                    mPhotos = new PhotoFragment();
                    fTransaction.add(R.id.ly_content, mPhotos);
                } else {
                    fTransaction.show(mPhotos);
                }

                break;

            // 相册
            case R.id.all_albums:

                // same as mPhotos
                actionBar.setDisplayHomeAsUpEnabled(false);
                actionBar.setTitle("相册");

                setSelected();
                vTvAlbums.setSelected(true);
                getFragmentManager().popBackStack();
                if (mAlbumFragment == null) {
                    mAlbumFragment = new AlbumsFragment();
                    fTransaction.add(R.id.ly_content, mAlbumFragment);
                } else {
                    mAlbumFragment.onRefresh();
                    fTransaction.show(mAlbumFragment);
                }
                havaInAlbum = true;
                break;
        }
        fTransaction.commit();
    }
}

