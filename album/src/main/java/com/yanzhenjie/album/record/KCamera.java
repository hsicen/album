package com.yanzhenjie.album.record;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.IntRange;

import com.yanzhenjie.album.Action;
import com.yanzhenjie.album.AlbumFile;

/**
 * <p>作者：hsicen  2019/12/10 15:15
 * <p>邮箱：codinghuang@163.com
 * <p>作用：
 * <p>描述：工具类统一入口(拍照和视频录制)
 */
public class KCamera {

    private Context mContext;
    private int mType = 1;  //1拍照   其它录制视频

    private Action<AlbumFile> mResult;
    private Action<String> mCancel;

    private int maxDuration = 30;
    private int minDuration = 3;

    public KCamera(Context mAct) {
        mContext = mAct;
    }

    public KCamera video() {
        mType = 0;
        return this;
    }

    public KCamera picture() {
        mType = 1;
        return this;
    }

    /*** 最大时长
     * @param duration   单位秒
     */
    public KCamera maxDuration(@IntRange(from = 1) int duration) {
        maxDuration = duration <= 0 ? Integer.MAX_VALUE : duration;
        return this;
    }

    /*** 最小时长
     * @param duration   单位秒
     */
    public KCamera minDuration(@IntRange(from = 1) int duration) {
        minDuration = duration < 0 ? 0 : duration;
        return this;
    }

    /*** 取消回调*/
    public KCamera onCancel(Action<String> cancel) {
        mCancel = cancel;
        return this;
    }

    /***成功回调*/
    public KCamera onResult(Action<AlbumFile> result) {
        mResult = result;
        return this;
    }

    /*** 启动相机*/
    public void start() {
        VideoRecordActivity.sResult = mResult;
        VideoRecordActivity.sCancel = mCancel;

        if (maxDuration < minDuration) {
            Toast.makeText(mContext, "最大拍摄时长不能小于最小拍摄时长", Toast.LENGTH_SHORT).show();
        } else {
            VideoRecordActivity.start(mContext, maxDuration, minDuration);
        }
    }

}
