package org.tensorflow.photoclassifier.logic;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.tensorflow.photoclassifier.dao.DataBaseOperator;
import org.tensorflow.photoclassifier.dao.SystemDataBaseOperator;
import org.tensorflow.photoclassifier.config.ClassifierConfig;
import org.tensorflow.photoclassifier.datasource.ImagesProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WelcomeLogic extends LogicBase {
    public static final int MSG_SCAN_IMAGE_BEGIN = 0x1;
    public static final int MSG_SCAN_IMAGE_FINISH = 0x2;
    public static final int MSG_RREPARE_TENSORFLOW = 0x3;
    public static final int MSG_CLASSIF_IMAGE_VIA_TF = 0x23;
    public static final int MSG_PREPARE_WORK_FINISH = 0x24;

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
}
