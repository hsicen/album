package com.yanzhenjie.album.app.album.data;

import android.os.AsyncTask;

import com.yanzhenjie.album.AlbumFile;

/**
 * <p>作者：hsicen  2019/11/8 17:27
 * <p>邮箱：codinghuang@163.com
 * <p>功能：
 * <p>描述：将文件路径转化为文件
 */
public class PathConvertTask extends AsyncTask<String, Void, AlbumFile> {

    public interface Callback {
        /***开始转化.*/
        void onConvertStart();

        /*** 转化完成*/
        void onConvertCallback(AlbumFile albumFile);
    }

    private PathConversion mConversion;
    private Callback mCallback;

    public PathConvertTask(PathConversion conversion, Callback callback) {
        this.mConversion = conversion;
        this.mCallback = callback;
    }

    @Override
    protected void onPreExecute() {
        mCallback.onConvertStart();
    }

    @Override
    protected AlbumFile doInBackground(String... params) {
        return mConversion.convert(params[0]);
    }

    @Override
    protected void onPostExecute(AlbumFile file) {
        mCallback.onConvertCallback(file);
    }
}