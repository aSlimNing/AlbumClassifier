package org.tensorflow.photoclassifier.app;

import android.app.Application;

import org.tensorflow.photoclassifier.classifier.TFClassifierInstance;

public class InitialApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        initTFClassifier();
    }

    private void initTFClassifier() {
        TFClassifierInstance.getInstance().init(this);
    }
}
