package org.tensorflow.photoclassifier.ui.fragment;


import android.annotation.SuppressLint;
import android.app.Fragment;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.GridView;

import org.tensorflow.demo.R;
import org.tensorflow.photoclassifier.config.ClassifierConfig;
import org.tensorflow.photoclassifier.dataobject.PhotoItem;
import org.tensorflow.photoclassifier.ui.activity.MainActivity;
import org.tensorflow.photoclassifier.ui.activity.PhotoDetailActivity;
import org.tensorflow.photoclassifier.ui.adapter.PhotoAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.tensorflow.photoclassifier.utils.ImageUtils.getAlbumPhotos;
import static org.tensorflow.photoclassifier.utils.ImageUtils.getMediaImageInfo;
import static org.tensorflow.photoclassifier.utils.ImageUtils.updateGallery;

public class PhotoFragment extends Fragment {
    // show 所有照片  或者  某个相册
    private String type = null;
    private List<PhotoItem> photoList = new ArrayList<PhotoItem>();
    // 声明一个gridview
    private GridView gridView;
    private boolean isInit = false;
    private boolean scoll = false;
    private PhotoAdapter adapter;

    List<Map> result;
    // 空的构造函数
    public PhotoFragment() {
        this.type = null;
        Log.d("in this album", "null constructer");
    }
    @SuppressLint("ValidFragment")
    public PhotoFragment(String type) {
        this.type = type;
        Log.d("in this album", "type is not null");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (getContext() == null) Log.d("getContext", "Null");
        }

    }
    // 重写创建fregement方法
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fg_photos,container,false);
        gridView = (GridView) view.findViewById(R.id.gridView1);
        if (type != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                this.result = getAlbumPhotos(getContext(), this.type);
            }
        }
        // 获得照片数据
        initPhoto();
        // 获得gridview
        gridView = (GridView) view.findViewById(R.id.gridView1);
        // 讲相片元素与相片数组用适配器组合
        adapter = new PhotoAdapter(getActivity(), R.layout.photo_item, photoList);
        gridView.setAdapter(adapter);

        // 正在滑动 或者 静止
        if (!scoll) {
            ;
        } else {
            ;
        }

        // 设定点击事件，当点击某一个相片，返回照片在list的位置
        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View v,
                                    int position, long id) {
                System.out.println(position+ " " +id);
                // 进入查看相片细节的activity
                // 注意这个是activity不是fregment
                Intent intent = new Intent(getActivity(), PhotoDetailActivity.class);
                Log.d("Position", ""+position);
                // send value
                intent.putExtra("position", position);
                intent.putExtra("type", type);
                intent.putExtra("url", photoList.get(position).getImageId());
                // start
                startActivity(intent);
            }
        });
        return view;
    }
    // refresh datas
    public void refresh(String fileName) {
        // in this class we can update UI
        new UpdateGridView(fileName).execute();

    }
    // 初始化照片数组
    private void initPhoto() {
        PhotoItem photo;
        if (type == null) {
            final List<Map> mediaImageInfo;
            mediaImageInfo = getMediaImageInfo(getActivity().getApplicationContext());

            for (Map<String, String> map : mediaImageInfo) {
                // in this map, the key of url is _data
                String url = map.get("_data");
                if (url != null) {
                    photo = new PhotoItem(url);
                    photoList.add(photo);
                }
            }
        }
        else {
            for (Map<String, String> map : result) {
                // in this map, the key of url is _data
                String url = map.get("url");
                if (url != null) {
                    photo = new PhotoItem(url);
                    photoList.add(photo);
                }
            }
        }
    }
    // update gridview
    @SuppressLint("StaticFieldLeak")
    class UpdateGridView extends AsyncTask<String, String, String>
    {
        // get url of new image
        private String fileName;
        UpdateGridView(String fileName) {
            this.fileName = fileName;
        }
        @Override
        protected String doInBackground(String... params) {
            try {
                // update db


                updateGallery(getActivity().getApplicationContext(), fileName);

                // we don't know the time when update db is end (it works in another thread)
                // so now i set a time to wait it finished (it is a bad way)
                while (!ClassifierConfig.workdone) {
                    Thread.sleep(10);
                }
                // clear list
                photoList.clear();
                // get photo
                initPhoto();

                Log.d("rescan image: ", "finished");

            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            return null;
        }
        @Override
        protected void onPostExecute(String result) {
            // TODO Auto-generated method stub
            Log.d("update gridview: ", "start");
            // update photo list
            adapter.notifyDataSetChanged();
        }
    }

}
