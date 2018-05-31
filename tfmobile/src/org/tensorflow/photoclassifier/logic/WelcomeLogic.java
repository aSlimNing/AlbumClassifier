package org.tensorflow.photoclassifier.logic;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import org.tensorflow.photoclassifier.dao.DataBaseOperator;
import org.tensorflow.photoclassifier.dao.SystemDataBaseOperator;
import org.tensorflow.photoclassifier.config.ClassifierConfig;

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



    public WelcomeLogic(Context context, Handler handler) {
        super(context, handler);
    }

    public void collectImagesAndHandle() {
        // get all image in device
        List<Map> imagesInDevice = SystemDataBaseOperator.getExternalImageInfo(mContextHost.get());
        // mOperator for my db
        DataBaseOperator operator = new DataBaseOperator(mContextHost.get(), ClassifierConfig.DB_NAME, ClassifierConfig.dbversion);

        String url;
        List<Map> findResult;
        // for every image in device
        for (Map imageInfo : imagesInDevice) {
            notifyUI(MSG_SCAN_IMAGE_BEGIN);
            url = (String) imageInfo.get("_data");
            // test whether had been classified
            findResult = operator.search("TFInformation", "url = '" + url + "'");
            if (findResult.size() == 0) {
                // not be classified
                notBeClassifiedImages.add(url);
                Log.d("TFInformation", "no");
            } else {
                // had been classified
                stillInDeviceImages.add(url);
                Log.d("TFInformation", "yes");

            }
        }
        // for every image in db
        List<Map> imagesInDB = operator.search("AlbumPhotos");
        for (Map imageInfo : imagesInDB) {
            url = (String) imageInfo.get("url");
            // test whether had been deleted
            findResult = operator.search("AlbumPhotos", "url = '" + url + "'");
            if (findResult.size() == 0) {
                // had been deleted, erase it in db
                operator.erase("AlbumPhotos", "url = ?", new String[]{"'" + url + "'"});
                operator.erase("TFInformation", "url = ?", new String[]{"'" + url + "'"});
            } else {
                // not be deleted, do nothing
            }
        }
        // for every album in db
        String album_name;
        List<Map> typeInAlbum = operator.search("Album");
        for (Map albumInfo : typeInAlbum) {
            album_name = (String) albumInfo.get("album_name");
            findResult = operator.search("AlbumPhotos", "album_name = '" + album_name + "'");
            if (findResult.size() == 0) {
                // had been deleted, erase it in db
                operator.erase("Album", "album_name = ?", new String[]{"'" + album_name + "'"});
            } else {
                // not be deleted, do nothing
            }
        }
        operator.close();
        notifyUI(MSG_SCAN_IMAGE_FINISH);
    }

    private void notifyUI(int msg){
        if(mHandlerHost.get() != null){
            mHandlerHost.get().sendEmptyMessage(msg);
        }
    }
}
