package org.tensorflow.photoclassifier.logic;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.RequiresApi;
import android.util.Log;

import org.tensorflow.photoclassifier.Classifier;
import org.tensorflow.photoclassifier.classifier.TFClassifierInstance;
import org.tensorflow.photoclassifier.classifier.TensorFlowImageClassifier;
import org.tensorflow.photoclassifier.config.ClassifierConfig;
import org.tensorflow.photoclassifier.dao.DataBaseOperator;
import org.tensorflow.photoclassifier.dao.DatabaseHelper;
import org.tensorflow.photoclassifier.datasource.ImagesProvider;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

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
    // deal image by tensorflow
    public void dealPics(FileDescriptor fileDescriptor) {
        Bitmap bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor);
        // init tensorflow
        // resize bitmap
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        float scaleWidth = ((float) ClassifierConfig.INPUT_SIZE) / width;
        float scaleHeight = ((float) ClassifierConfig.INPUT_SIZE) / height;
        Matrix matrix = new Matrix();
        matrix.postScale(scaleWidth, scaleHeight);
        Bitmap newbm = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix, true);
        // get classifier information
        List<Classifier.Recognition> results = TFClassifierInstance.getInstance().recongizeImage(newbm);
        for (final TensorFlowImageClassifier.Recognition result : results) {
            System.out.println("Result: " + result.getTitle());
        }
        // call function to save image
        String url = saveImage("", bitmap);
        Log.d("Detected = ", String.valueOf(results));
        // update db
        DatabaseHelper databaseHelper = new DatabaseHelper(mContextHost.get(), "Album.db", null, ClassifierConfig.dbversion);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        ContentValues values_ablum = new ContentValues();
        ContentValues values = new ContentValues();
        String album_type;
        Cursor cursor_album = null;
        for (TensorFlowImageClassifier.Recognition cr : results) {
            int i;
            for (i = 0; i < ClassifierConfig.tf_type_times; ++i) {
                if (ClassifierConfig.tf_type_name[i].equals(cr.getTitle())) {
                    break;
                }
            }
            album_type = ClassifierConfig.album_type_name[i];
            cursor_album = db.query("Album", null, "album_name ='" + album_type + "'", null, null, null, null);
            if (!cursor_album.moveToFirst()) {
                values_ablum.put("album_name", album_type);
                values_ablum.put("show_image", url);
                db.insert("Album", null, values_ablum);
                values_ablum.clear();
            }

            values.put("album_name", album_type);
            values.put("url", url);
            db.insert("AlbumPhotos", null, values);
            values.clear();
        }
        db.close();

//        notifyUi("新的图片", String.valueOf(results), NOTI_CODE_NEW_PHOTO);
    }

    private String saveImage(String type, Bitmap bitmap) {
        FileOutputStream b = null;
        // save images to this location
        File file = new File(ClassifierConfig.location);
        // 创建文件夹 @ ClassifierConfig.location
        file.mkdirs();
        String str = null;
        Date date = null;
        // 获取当前时间，进一步转化为字符串
        SimpleDateFormat format = new SimpleDateFormat("yyyyMMddHHmmss");
        date = new Date();
        str = format.format(date);
        String fileName = ClassifierConfig.location + str + ".jpg";

        try {
            b = new FileOutputStream(fileName);
            // 把数据写入文件
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, b);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                assert b != null;
                b.flush();
                b.close();
                // reflash the fragment of Photos
                ClassifierConfig.workdone = false;
//                mPhotos.refresh(fileName);TODO 通知PhotoFragment刷新UI
                ClassifierConfig.workdone = false;
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                //TODO 删除临时文件
//                File imagePath = new File(Environment.getExternalStorageDirectory(), "tmp");
//                newFile = new File(imagePath, "default_image.jpg");
//                newFile.delete();
                Log.d("Delete", "True");
            } catch (Exception e) {
                Log.e("Delete", "Error");
            }
        }
        return fileName;
    }
}
