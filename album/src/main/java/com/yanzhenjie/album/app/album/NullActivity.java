
package com.yanzhenjie.album.app.album;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.yanzhenjie.album.Action;
import com.yanzhenjie.album.Album;
import com.yanzhenjie.album.R;
import com.yanzhenjie.album.api.widget.Widget;
import com.yanzhenjie.album.app.Contract;
import com.yanzhenjie.album.mvp.BaseActivity;

/**
 * <p>作者：hsicen  2019/11/21 9:37
 * <p>邮箱：codinghuang@163.com
 * <p>功能：
 * <p>描述：空界面
 */
public class NullActivity extends BaseActivity implements Contract.NullPresenter {

    private static final String KEY_OUTPUT_IMAGE_PATH = "KEY_OUTPUT_IMAGE_PATH";

    public static String parsePath(Intent intent) {
        return intent.getStringExtra(KEY_OUTPUT_IMAGE_PATH);
    }

    private int mQuality = 1;
    private long mLimitDuration;
    private long mLimitBytes;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.album_activity_null);
        Contract.NullView mView = new NullView(this, this);

        Bundle argument = getIntent().getExtras();
        assert argument != null;
        int function = argument.getInt(Album.KEY_INPUT_FUNCTION);
        boolean hasCamera = argument.getBoolean(Album.KEY_INPUT_ALLOW_CAMERA);

        mQuality = argument.getInt(Album.KEY_INPUT_CAMERA_QUALITY);
        mLimitDuration = argument.getLong(Album.KEY_INPUT_CAMERA_MAX_DURATION);
        mLimitBytes = argument.getLong(Album.KEY_INPUT_CAMERA_BYTES);

        Widget mWidget = argument.getParcelable(Album.KEY_INPUT_WIDGET);
        mView.setupViews(mWidget);

        switch (function) {
            case Album.FUNCTION_CHOICE_IMAGE: {
                mView.setMessage(R.string.album_not_found_image);
                mView.setTitle(getString(R.string.album_all_images));
                mView.setMakeVideoDisplay(false);
                break;
            }
            case Album.FUNCTION_CHOICE_VIDEO: {
                mView.setMessage(R.string.album_not_found_video);
                mView.setTitle(getString(R.string.album_all_videos));
                mView.setMakeImageDisplay(false);
                break;
            }
            case Album.FUNCTION_CHOICE_ALBUM: {
                mView.setMessage(R.string.album_not_found_album);
                mView.setTitle(getString(R.string.album_title));
                break;
            }
            default: {
                throw new AssertionError("This should not be the case.");
            }
        }

        if (!hasCamera) {
            mView.setMakeImageDisplay(false);
            mView.setMakeVideoDisplay(false);
        }
    }

    @Override
    public void takePicture() {
        Album.camera(this)
                .image()
                .onResult(mCameraAction)
                .start();
    }

    @Override
    public void takeVideo() {
        Album.camera(this)
                .video()
                .quality(mQuality)
                .limitDuration(mLimitDuration)
                .limitBytes(mLimitBytes)
                .onResult(mCameraAction)
                .start();
    }

    private Action<String> mCameraAction = new Action<String>() {
        @Override
        public void onAction(@NonNull String result) {
            Intent intent = new Intent();
            intent.putExtra(KEY_OUTPUT_IMAGE_PATH, result);
            setResult(RESULT_OK, intent);
            finish();
        }
    };
}