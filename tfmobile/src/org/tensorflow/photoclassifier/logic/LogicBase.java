package org.tensorflow.photoclassifier.logic;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.List;

/**
 * 逻辑控制基类
 *
 * @author hzsunjianshun
 */
public abstract class LogicBase implements ILogic {
    private final static String TAG = "LogicBase";

    // Handler的弱引用
    protected WeakReference<Handler> mHandlerHost;
    protected WeakReference<Context> mContextHost;
    private HashSet<Integer> mReqIds = new HashSet<Integer>();
    private boolean mIsReleased = true;

    public LogicBase(Context context, Handler handler) {
        mIsReleased = false;
        mContextHost = new WeakReference<Context>(context);
        mHandlerHost = new WeakReference<Handler>(handler);
    }

    protected void notifyUi(int what) {
        Handler handler = mHandlerHost.get();
        if (!mIsReleased && handler != null) {
            handler.sendEmptyMessage(what);
        }
    }

    protected void notifyUi(Message msg) {
        if (mIsReleased) {
            return;
        }
        Handler handler = mHandlerHost.get();
        if (handler != null) {
            handler.sendMessage(msg);
        }
    }

    protected void notifyUiDelay(int what, long delayMillis) {
        if (mIsReleased) {
            return;
        }
        Handler handler = mHandlerHost.get();
        if (handler != null) {
            handler.sendEmptyMessageDelayed(what, delayMillis);
        }
    }

    protected boolean isReleased() {
        return mIsReleased;
    }

    protected void toastErrorMessage(String msg) {
        if (mHandlerHost.get() == null) {
            return;
        }
        Toast.makeText(mContextHost.get(), msg,Toast.LENGTH_SHORT).show();
    }

    protected void addReqId(int id) {
        mReqIds.add(id);
    }

    protected void removeId(int id) {
//		mReqIds.remove(id);
    }

    protected void addReqIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }
        mReqIds.addAll(ids);
    }

    protected void removeIds(List<Integer> ids) {
//		if (ids == null || ids.isEmpty()) return;
//		mReqIds.remove(ids);
    }

    protected void removeMessages() {
    }

    @Override
    public void release() {
        mIsReleased = true;
        if (mHandlerHost.get() == null) {
            return;
        }
        removeMessages();
        mHandlerHost.clear();
    }
}
