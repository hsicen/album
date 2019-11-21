package com.yanzhenjie.album.app.album;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.yanzhenjie.album.AlbumFile;
import com.yanzhenjie.album.R;
import com.yanzhenjie.album.mvp.BaseActivity;
import com.yanzhenjie.album.util.AlbumUtils;

import java.io.File;

/**
 * <p>作者：hsicen  2019/11/8 9:55
 * <p>邮箱：codinghuang@163.com
 * <p>功能：
 * <p>描述：本地视频播放
 */
public class VideoPlayActivity extends BaseActivity {
    //预览完成回调
    public static VideoCallback sCallback;
    public static AlbumFile mSelectFile;

    private Toolbar mToolbar;
    private String mVideoPath;
    private TextView mTvFinish;
    private VideoView mVideoView;
    private RelativeLayout mLayoutBottom;
    private int videoWidth;
    private int videoHeight;

    private boolean isRecord = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusNavigationBar();
        setContentView(R.layout.album_activity_video_play);
        mToolbar = findViewById(R.id.video_bar);
        mTvFinish = findViewById(R.id.tv_finish);
        mVideoView = findViewById(R.id.video_local);
        mLayoutBottom = findViewById(R.id.layout_bottom);

        mVideoView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                int height = getResources().getDisplayMetrics().heightPixels;
                reFinishSize(height);
            }
        });

        initToolBar();
        initVariable();
        initVideo();
        initListener();
    }

    private void initToolBar() {
        setSupportActionBar(mToolbar);
        mToolbar.setTitle(R.string.album_text_empty);
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isRecord) {
                    File file = new File(mSelectFile.getPath());
                    if (file.exists()) file.delete();
                }

                onBackPressed();
            }
        });
    }

    private void initVariable() {
        mVideoPath = getIntent().getStringExtra("videoPath");
        Log.d("hsc", "视频路径为： " + mVideoPath);
    }

    private void initVideo() {
        mVideoView.setVideoPath(mVideoPath);

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                videoWidth = mp.getVideoWidth();
                videoHeight = mp.getVideoHeight();

                if (0 == videoWidth && 0 == videoHeight) {
                    Toast.makeText(VideoPlayActivity.this, "该格式不支持预览，可发布成功后查看", Toast.LENGTH_SHORT).show();
                    return;
                }

                int height = getResources().getDisplayMetrics().heightPixels;
                reFinishSize(height);
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mVideoView.start();
                mVideoView.requestFocus();
            }
        });

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                Toast.makeText(VideoPlayActivity.this, "该格式不支持预览，可发布成功后查看", Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    /*** 重置Button位置*/
    private void reFinishSize(int height) {
        if (videoHeight > videoWidth && (height - mVideoView.getHeight()) > 0) {
            int tempHeight = height - mVideoView.getHeight();
            //判断导航栏是否存在
            mLayoutBottom.setPadding(0, 0, 0, tempHeight / 2);
        } else {
            mLayoutBottom.setPadding(0, 0, 0, 0);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        mVideoView.start();
        mVideoView.requestFocus();
        mVideoView.setVisibility(View.VISIBLE);
        mLayoutBottom.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onResume() {
        super.onResume();

        hideStatusNavigationBar();
        mVideoView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();

        mVideoView.pause();
    }

    @Override
    protected void onStop() {
        super.onStop();

        mVideoView.stopPlayback();
    }

    private void initListener() {

        mTvFinish.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlbumUtils.updateFileFromDatabase(new File(mSelectFile.getPath()));
                sCallback.onVideoBack();
                finish();
            }
        });
    }

    /*** 跳转视频播放界面
     * @param activity activity
     * @param  fromRecord 是否为录制视频跳转
     * @param path 视频路径 */
    public static void start(Activity activity, String path, boolean fromRecord) {
        Intent intent = new Intent(activity, VideoPlayActivity.class);
        intent.putExtra("videoPath", path);
        intent.putExtra("isRecord", fromRecord);
        activity.startActivity(intent);
        activity.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    /*** 隐藏状态栏和导航栏*/
    private void hideStatusNavigationBar() {
        int uiFlags = View.INVISIBLE | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        getWindow().getDecorView().setSystemUiVisibility(uiFlags);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    public interface VideoCallback {

        /*** 预览完成回调*/
        void onVideoBack();
    }
}
