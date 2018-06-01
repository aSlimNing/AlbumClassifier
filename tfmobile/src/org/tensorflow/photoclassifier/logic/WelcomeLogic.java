package org.tensorflow.photoclassifier.logic;

import android.content.ContentValues;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.tensorflow.photoclassifier.Classifier;
import org.tensorflow.photoclassifier.TensorFlowImageClassifier;
import org.tensorflow.photoclassifier.dao.DataBaseOperator;
import org.tensorflow.photoclassifier.dao.SystemDataBaseOperator;
import org.tensorflow.photoclassifier.config.ClassifierConfig;
import org.tensorflow.photoclassifier.datasource.ImagesProvider;
import org.tensorflow.photoclassifier.ui.activity.WelcomeActivity;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;


import static org.tensorflow.photoclassifier.config.ClassifierConfig.IMAGE_MEAN;
import static org.tensorflow.photoclassifier.config.ClassifierConfig.IMAGE_STD;
import static org.tensorflow.photoclassifier.config.ClassifierConfig.INPUT_NAME;
import static org.tensorflow.photoclassifier.config.ClassifierConfig.INPUT_SIZE;
import static org.tensorflow.photoclassifier.config.ClassifierConfig.LABEL_FILE;
import static org.tensorflow.photoclassifier.config.ClassifierConfig.MODEL_FILE;
import static org.tensorflow.photoclassifier.config.ClassifierConfig.OUTPUT_NAME;

public class WelcomeLogic extends LogicBase {
    public static final int MSG_SCAN_IMAGE_BEGIN = 0x1;
    public static final int MSG_SCAN_IMAGE_FINISH = 0x2;
    public static final int MSG_RREPARE_TENSORFLOW = 0x3;
    public static final int MSG_CLASSIF_IMAGE_VIA_TF = 0x23;
    public static final int MSG_PREPARE_WORK_FINISH = 0x24;

    private ContentValues value;

    private Classifier classifier;

    // scan image and save them
    private List<String> stillInDeviceImages = new ArrayList<>();
    private List<String> notBeClassifiedImages = new ArrayList<>();

    //datasource
    private ImagesProvider datasource;

    public WelcomeLogic(Context context, Handler handler) {
        super(context, handler);
        datasource = new ImagesProvider(context);
    }

    public void collectImagesAndHandle() {
        // get all image in device
        // for every image in device
        notifyUI(MSG_SCAN_IMAGE_BEGIN);
        datasource.scanImagesInDevices(new ImagesProvider.Callback() {
            @Override
            public void onScanImagesInDeviceSucceed(List<String> imagesHasBeenClassifier, List<String> imagesNotClassifier) {
                stillInDeviceImages = imagesHasBeenClassifier;
                notBeClassifiedImages = imagesNotClassifier;
            }
        });
        // mOperator for my db
        datasource.scanImagesInDb();
        notifyUI(MSG_SCAN_IMAGE_FINISH);
    }

    private void notifyUI(int msg){
        if(mHandlerHost.get() != null){
            mHandlerHost.get().sendEmptyMessage(msg);
        }
    }

    public void classifyNewImages(){
        new Thread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void run() {
                Looper.prepare();
                // init tensorflow
                if (classifier == null) {
                    // get permission
                    classifier =
                            TensorFlowImageClassifier.create(
                                    mContextHost.get().getAssets(),
                                    MODEL_FILE,
                                    LABEL_FILE,
                                    INPUT_SIZE,
                                    IMAGE_MEAN,
                                    IMAGE_STD,
                                    INPUT_NAME,
                                    OUTPUT_NAME);

                }
                Bitmap bitmap;
                value = new ContentValues();
                datasource.insertImageIntoDb();
                for (String image : notBeClassifiedImages) {
                    notifyUI(MSG_CLASSIF_IMAGE_VIA_TF);
                    BitmapFactory.Options options = new BitmapFactory.Options();
                    options.inPreferredConfig = Bitmap.Config.ARGB_4444;
                    bitmap = BitmapFactory.decodeFile(image, options);
                    datasource.insertImageIntoDb(image, do_tensorflow(bitmap, classifier), mOperator, value);
                }
                notifyUI(MSG_PREPARE_WORK_FINISH);
                Looper.loop();
            }
        }).start();
    }
}
