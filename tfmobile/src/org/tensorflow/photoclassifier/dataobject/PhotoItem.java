package org.tensorflow.photoclassifier.dataobject;

public class PhotoItem {
    private int imageId;
    private String data;
    public PhotoItem(String data) {
        this.data = data;
    }
    public String getImageId() {
        return data;
    }
}
