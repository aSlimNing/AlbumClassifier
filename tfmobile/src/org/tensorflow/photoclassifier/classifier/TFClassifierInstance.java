package org.tensorflow.photoclassifier.classifier;

import android.content.Context;
import android.graphics.Bitmap;

import org.tensorflow.photoclassifier.Classifier;
import org.tensorflow.photoclassifier.utils.ImageUtils;

import java.util.List;

import static org.tensorflow.photoclassifier.config.ClassifierConfig.IMAGE_MEAN;
import static org.tensorflow.photoclassifier.config.ClassifierConfig.IMAGE_STD;
import static org.tensorflow.photoclassifier.config.ClassifierConfig.INPUT_NAME;
import static org.tensorflow.photoclassifier.config.ClassifierConfig.INPUT_SIZE;
import static org.tensorflow.photoclassifier.config.ClassifierConfig.LABEL_FILE;
import static org.tensorflow.photoclassifier.config.ClassifierConfig.MODEL_FILE;
import static org.tensorflow.photoclassifier.config.ClassifierConfig.OUTPUT_NAME;

/**
 * 对Classifier的单例封装
 */
public class TFClassifierInstance {
    private static TFClassifierInstance mInstance;

    private Classifier mTensorFlowImageClassifier;

    public static TFClassifierInstance getInstance() {
        if(mInstance == null){
            mInstance = new TFClassifierInstance();
        }
        return mInstance;
    }

    private TFClassifierInstance(){

    }

    public void init(Context context) {
        mTensorFlowImageClassifier = TensorFlowImageClassifier.create(
                context.getAssets(),
                MODEL_FILE,
                LABEL_FILE,
                INPUT_SIZE,
                IMAGE_MEAN,
                IMAGE_STD,
                INPUT_NAME,
                OUTPUT_NAME);
    }

    public List<Classifier.Recognition> recongizeImage(Bitmap bitmap){
        if(mTensorFlowImageClassifier != null){
            Bitmap newbm = ImageUtils.dealImageForTF(bitmap);
            bitmap.recycle();
            try{
                return mTensorFlowImageClassifier.recognizeImage(newbm);
            }catch (Exception e){

            }
        }else {
            //do nothing
        }
        return null;
    }

    public String getStatString(){
        if(mInstance !=null){
            return mInstance.getStatString();
        }else {
            return "";
        }
    }

    public void enableStatLogging(boolean debug){
        if(mInstance !=null){
            mInstance.enableStatLogging(debug);
        }
    }
}
