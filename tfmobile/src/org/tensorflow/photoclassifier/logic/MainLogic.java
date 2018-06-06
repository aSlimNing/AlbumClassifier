package org.tensorflow.photoclassifier.logic;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.tensorflow.photoclassifier.classifier.TFClassifierInstance;
import org.tensorflow.photoclassifier.config.ClassifierConfig;
import org.tensorflow.photoclassifier.dao.DataBaseOperator;
import org.tensorflow.photoclassifier.datasource.ImagesProvider;
import org.tensorflow.photoclassifier.utils.ImageUtils;

import static org.tensorflow.photoclassifier.ui.activity.MainActivity.SCAN_IMAGES;

public class MainLogic extends LogicBase {

    private final ImagesProvider mDatasource;
    private classifyImagesNotify mClassifyImagesNotify;

    // notification
    private int NOTI_CODE_HAVE_NEW = 1;
    private int NOTI_CODE_CLASSIFYING = 2;
    private int NOTI_CODE_FINISHED = 3;
    private int NOTI_CODE_NEW_PHOTO = 4;

    public MainLogic(Context context, Handler handler) {
        super(context, handler);
        mDatasource = new ImagesProvider(context);
    }

    public interface classifyImagesNotify {
        void onStartClassify(String title, String message, int code);

        void onItemClassified(int now, int sum, int code);
    }

    public void classifyImagesAtBackground() {
        if (ClassifierConfig.needToBeClassified == null) {
            // do nothing
            Log.d("Main-TF", "null");
        } else if (ClassifierConfig.needToBeClassified.size() == 0) {
            // do nothing
            Log.d("Main-TF", "0");
        } else {
            new Thread(new Runnable() {
                @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
                @Override
                public void run() {
                    Looper.prepare();
                    Bitmap bitmap;
                    // init tensorflow
                    ContentValues value = new ContentValues();
                    DataBaseOperator myoperator = new DataBaseOperator(mContextHost.get(),
                            ClassifierConfig.DB_NAME, ClassifierConfig.dbversion);
                    int now = 1;
                    mClassifyImagesNotify.onStartClassify("正在为您处理" + ClassifierConfig.needToBeClassified.size()
                            + "张新的图片", "您可以到通知中心查看处理进度", NOTI_CODE_HAVE_NEW);
                    for (String image : ClassifierConfig.needToBeClassified) {
                        Log.d("classifyImages", image);
                        mClassifyImagesNotify.onItemClassified(now++, ClassifierConfig.needToBeClassified.size(), NOTI_CODE_CLASSIFYING);
                        BitmapFactory.Options options = new BitmapFactory.Options();
                        options.inPreferredConfig = Bitmap.Config.ARGB_4444;
                        bitmap = BitmapFactory.decodeFile(image, options);
                        mDatasource.insertImageIntoDB(image, TFClassifierInstance.getInstance().recongizeImage(bitmap), value);
//                        if (havaInAlbum) {
//                            notifyUi(SCAN_IMAGES);
//                        }

                    }
                    ClassifierConfig.needToBeClassified = null;
                    myoperator.close();
                    Looper.loop();
                }
            }).start();
        }
    }

    @Override
    protected void notifyUi(Message msg) {
        super.notifyUi(msg);
    }
}
