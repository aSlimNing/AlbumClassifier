package org.tensorflow.photoclassifier.datasource;

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import org.tensorflow.photoclassifier.config.ClassifierConfig;
import org.tensorflow.photoclassifier.dao.DataBaseOperator;
import org.tensorflow.photoclassifier.dao.SystemDataBaseOperator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImagesProvider {


    public interface Callback {
        void onScanImagesInDeviceSucceed(List<String> imagesHasBeenClassifier, List<String> imagesNotClassifier);
    }

    private Context mContext;

    private DataBaseOperator mOperator;

    public ImagesProvider(Context context) {
        this.mContext = context;
        mOperator = new DataBaseOperator(mContext, ClassifierConfig.DB_NAME, ClassifierConfig.dbversion);
    }

    public void scanImagesInDevices(Callback callback) {
        List<Map> imagesInDevice = SystemDataBaseOperator.getExternalImageInfo(mContext);
        String url;
        List<String> notBeClassifiedImages = new ArrayList<>();
        List<String> hasBeenClassifiedImages = new ArrayList<>();
        for (Map imageInfo : imagesInDevice) {
            url = (String) imageInfo.get("_data");
            // test whether had been classified
            List findResult = mOperator.search("TFInformation", "url = '" + url + "'");
            if (findResult.size() == 0) {
                // not be classified
                notBeClassifiedImages.add(url);
                Log.d("TFInformation", "no");
            } else {
                // had been classified
                hasBeenClassifiedImages.add(url);
                Log.d("TFInformation", "yes");
            }
        }
        callback.onScanImagesInDeviceSucceed(hasBeenClassifiedImages,notBeClassifiedImages);
    }

    public void scanImagesInDb() {
        // for every image in db
        List<Map> imagesInDB = mOperator.search("AlbumPhotos");
        for (Map imageInfo : imagesInDB) {
            String url = (String) imageInfo.get("url");
            // test whether had been deleted
            List findResult = mOperator.search("AlbumPhotos", "url = '" + url + "'");
            if (findResult.size() == 0) {
                // had been deleted, erase it in db
                mOperator.erase("AlbumPhotos", "url = ?", new String[]{"'" + url + "'"});
                mOperator.erase("TFInformation", "url = ?", new String[]{"'" + url + "'"});
            } else {
                // not be deleted, do nothing
            }
        }
        // for every album in db
        String album_name;
        List<Map> typeInAlbum = mOperator.search("Album");
        for (Map albumInfo : typeInAlbum) {
            album_name = (String) albumInfo.get("album_name");
            List findResult = mOperator.search("AlbumPhotos", "album_name = '" + album_name + "'");
            if (findResult.size() == 0) {
                // had been deleted, erase it in db
                mOperator.erase("Album", "album_name = ?", new String[]{"'" + album_name + "'"});
            } else {
                // not be deleted, do nothing
            }
        }
    }

    public void insertImageIntoDB(String image, List<classifier.Recognition> results, ContentValues value) {
        if (results == null) return;
        List<Map> findResult;
        if (mOperator == null) {
            mOperator = new DataBaseOperator(mContext, ClassifierConfig.DB_NAME, ClassifierConfig.dbversion);
        }
        for (classifier.Recognition cr : results) {
            String type = cr.getTitle();
            // AlbumPhotos
            value.clear();
            value.put("album_name", type);
            value.put("url", image);
            mOperator.insert("AlbumPhotos", value);
            // Album
            findResult = mOperator.search("Album", "album_name = '" + type + "'");
            if (findResult.size() == 0) {
                value.clear();
                value.put("album_name", type);
                value.put("show_image", image);
                mOperator.insert("Album", value);
            }
            //TFInfromation
            value.clear();
            value.put("url", image);
            value.put("tf_type", type);
            value.put("confidence", cr.getConfidence());
            mOperator.insert("TFInformation", value);
        }
    }

    public void release() {
        mContext = null;
        mOperator.close();
    }
}
