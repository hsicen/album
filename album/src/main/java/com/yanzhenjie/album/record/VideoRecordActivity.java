package com.yanzhenjie.album.record;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.yanzhenjie.album.AlbumFile;
import com.yanzhenjie.album.R;
import com.yanzhenjie.album.app.album.VideoPlayActivity;
import com.yanzhenjie.album.notchtools.NotchTools;
import com.yanzhenjie.album.util.AlbumUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;

import cameraview.CameraException;
import cameraview.CameraListener;
import cameraview.CameraLogger;
import cameraview.CameraOptions;
import cameraview.CameraView;
import cameraview.PictureResult;
import cameraview.VideoResult;
import cameraview.controls.Facing;
import cameraview.controls.Flash;
import cameraview.controls.Mode;
import cameraview.frame.Frame;
import cameraview.frame.FrameProcessor;
import cameraview.markers.DefaultAutoFocusMarker;

/**
 * <p>作者：hsicen  2019/11/26 10:34
 * <p>邮箱：codinghuang@163.com
 * <p>功能：
 * <p>描述：视频录制界面
 */
public class VideoRecordActivity extends AppCompatActivity implements
        View.OnClickListener,
        VideoPlayActivity.VideoCallback {
    private static final String sMaxTime = "max_record_time";
    private static final String sMinTime = "min_record_time";

    public static RecordCallback sCallback;
    private final static CameraLogger LOG = CameraLogger.create("DemoApp");
    private final static boolean USE_FRAME_PROCESSOR = false;
    private final static boolean DECODE_BITMAP = true;

    //拍摄时间限制(单位秒)
    private int maxDuration = 30;
    private int minDuration = 3;

    private CameraView camera;
    private boolean isFlash = false;

    private ImageButton mFlashButton;
    private ImageButton mCloseButton;
    private ImageButton mSwitchButton;
    private TextView mTvRecordHint;
    private CountDownButton mBtnRecord;

    //视频暂停录制   1录制正常停止    2录制异常停止
    private int stopMode = 1;
    private View mRoot;
    private boolean isRecording = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusNavigationBar();
        setContentView(R.layout.activity_video_record);
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);
        minDuration = getIntent().getIntExtra(sMinTime, minDuration);
        maxDuration = getIntent().getIntExtra(sMaxTime, maxDuration);
        minDuration = (minDuration < 0) ? 0 : minDuration;
        maxDuration = (maxDuration <= 0) ? Integer.MAX_VALUE : maxDuration;

        camera = findViewById(R.id.camera);
        camera.setLifecycleOwner(getLifecycle());
        camera.addCameraListener(new Listener());
        mFlashButton = findViewById(R.id.ib_light);
        mCloseButton = findViewById(R.id.ib_record_close);
        mSwitchButton = findViewById(R.id.toggleCamera);
        mTvRecordHint = findViewById(R.id.tv_record_hint);
        mBtnRecord = findViewById(R.id.captureVideo);
        mRoot = findViewById(R.id.root);
        if (NotchTools.getFullScreenTools().isNotchScreen(getWindow())) {
            mRoot.setFitsSystemWindows(true);
            mRoot.requestApplyInsets();
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        mTvRecordHint.setText(String.format(getString(R.string.album_record_hint), minDuration));
        mBtnRecord.setMaxSeconds(maxDuration);
        mBtnRecord.setMinSeconds(minDuration + 1);

        if (USE_FRAME_PROCESSOR) {
            camera.addFrameProcessor(new FrameProcessor() {
                private long lastTime = System.currentTimeMillis();

                @Override
                public void process(@NonNull Frame frame) {
                    long newTime = frame.getTime();
                    long delay = newTime - lastTime;
                    lastTime = newTime;
                    LOG.e("Frame delayMillis:", delay, "FPS:", 1000 / delay);
                    if (DECODE_BITMAP) {
                        YuvImage yuvImage = new YuvImage(frame.getData(), ImageFormat.NV21,
                                frame.getSize().getWidth(),
                                frame.getSize().getHeight(),
                                null);
                        ByteArrayOutputStream jpegStream = new ByteArrayOutputStream();
                        yuvImage.compressToJpeg(new Rect(0, 0,
                                frame.getSize().getWidth(),
                                frame.getSize().getHeight()), 100, jpegStream);
                        byte[] jpegByteArray = jpegStream.toByteArray();
                        Bitmap bitmap = BitmapFactory.decodeByteArray(jpegByteArray, 0, jpegByteArray.length);
                        bitmap.toString();
                    }
                }
            });
        }

        findViewById(R.id.captureVideo).setOnClickListener(this);  // 点击拍照监听
        findViewById(R.id.toggleCamera).setOnClickListener(this); // 相机切换监听
        findViewById(R.id.ib_light).setOnClickListener(this);  //闪光灯切换监听
        findViewById(R.id.ib_record_close).setOnClickListener(this); //点击返回监听
    }

    private void message(@NonNull String content, boolean important) {
        if (important) {
            LOG.w(content);
            //Toast.makeText(this, content, Toast.LENGTH_LONG).show();
        } else {
            LOG.i(content);
            //Toast.makeText(this, content, Toast.LENGTH_SHORT).show();
        }
    }

    private class Listener extends CameraListener {
        @Override
        public void onCameraOpened(@NonNull CameraOptions options) {
            toggleFlash(true);
            camera.setAutoFocusMarker(new DefaultAutoFocusMarker());
        }

        @Override
        public void onCameraError(@NonNull CameraException exception) {
            super.onCameraError(exception);
            message("Got CameraException #" + exception.getReason(), true);
        }

        @Override
        public void onPictureTaken(@NonNull PictureResult result) {
            super.onPictureTaken(result);
        }

        @Override
        public void onVideoTaken(@NonNull VideoResult result) {
            super.onVideoTaken(result);
            if (2 == stopMode) return;

            //判断时长
            AlbumFile albumFile = new AlbumFile();
            MediaMetadataRetriever media = new MediaMetadataRetriever();
            media.setDataSource(result.getFile().getAbsolutePath());
            long duration = Long.parseLong(media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION));
            albumFile.setDuration(duration);
            duration = AlbumUtils.getSpecificTime(duration);
            if (duration <= minDuration) {
                dealIconStatus(true);
                Toast.makeText(VideoRecordActivity.this, String.format(getString(R.string.album_record_error_finish),
                        minDuration), Toast.LENGTH_SHORT).show();
                if (result.getFile().exists()) result.getFile().delete();
                return;
            }

            //拍照完成
            albumFile.setPath(result.getFile().getAbsolutePath());
            albumFile.setMediaType(AlbumFile.TYPE_VIDEO);

            VideoPlayActivity.sCallback = VideoRecordActivity.this;
            VideoPlayActivity.mSelectFile = albumFile;
            VideoPlayActivity.start(VideoRecordActivity.this, result.getFile().getAbsolutePath(), true);
        }

        @Override
        public void onVideoRecordingStart() {
            super.onVideoRecordingStart();
            isRecording = true;
            mBtnRecord.startCountDown();
            stopMode = 1;
            dealIconStatus(false);
            LOG.w("onVideoRecordingStart!");
        }

        @Override
        public void onVideoRecordingEnd() {
            super.onVideoRecordingEnd();
            isRecording = false;
            mBtnRecord.stopCountDown();
            message("Video taken. Processing...", false);
            LOG.w("onVideoRecordingEnd!");
        }

        @Override
        public void onExposureCorrectionChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
            super.onExposureCorrectionChanged(newValue, bounds, fingers);
            message("Exposure correction:" + newValue, false);
        }

        @Override
        public void onZoomChanged(float newValue, @NonNull float[] bounds, @Nullable PointF[] fingers) {
            super.onZoomChanged(newValue, bounds, fingers);
            message("Zoom:" + newValue, false);
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();
        if (id == R.id.captureVideo) {
            captureVideo();
        } else if (id == R.id.toggleCamera) {
            toggleCamera();
        } else if (id == R.id.ib_record_close) {
            stopMode = 2;
            camera.stopVideo();
            onBackPressed();
        } else if (id == R.id.ib_light) {
            toggleFlash(false);
        }
    }

    /*** 点击录制视频*/
    private void captureVideo() {
        if (camera.getMode() == Mode.PICTURE) {
            message("Can't record HQ videos while in PICTURE mode.", false);
            return;
        }

        if (camera.isTakingPicture() || camera.isTakingVideo()) {
            camera.stopVideo();
            return;
        }

        camera.takeVideoSnapshot(new File(AlbumUtils.randomMP4Path(this)), maxDuration * 1000);
    }

    private void toggleCamera() {
        if (camera.isTakingPicture() || camera.isTakingVideo()) return;
        switch (camera.toggleFacing()) {
            case BACK:
                mFlashButton.setVisibility(View.VISIBLE);
                message("Switched to back camera!", false);
                break;

            case FRONT:
                mFlashButton.setVisibility(View.GONE);
                message("Switched to front camera!", false);
                break;
        }
    }

    /*** 切换闪光灯状态*/
    private void toggleFlash(boolean isInit) {
        if (isInit) isFlash = true;

        if (isFlash) {
            isFlash = false;
            mFlashButton.setImageResource(R.drawable.ic_light_off);
            camera.setFlash(Flash.OFF);
        } else {
            isFlash = true;
            mFlashButton.setImageResource(R.drawable.ic_light_on);
            camera.setFlash(Flash.TORCH);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        boolean valid = true;
        for (int grantResult : grantResults) {
            valid = valid && grantResult == PackageManager.PERMISSION_GRANTED;
        }
        if (valid && !camera.isOpened()) {
            camera.open();
        }
    }

    public static void start(Activity mAct, int maxDuration, int minDuration) {
        Intent intent = new Intent(mAct, VideoRecordActivity.class);
        intent.putExtra(sMaxTime, maxDuration);
        intent.putExtra(sMinTime, minDuration);
        mAct.startActivity(intent);
    }

    private void hideStatusNavigationBar() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    /*** 处理Icon状态*/
    private void dealIconStatus(boolean isShow) {
        if (isShow) {
            if (Facing.FRONT == camera.getFacing()) {
                mFlashButton.setVisibility(View.GONE);
            } else {
                mFlashButton.setVisibility(View.VISIBLE);
            }

            mCloseButton.setVisibility(View.VISIBLE);
            mSwitchButton.setVisibility(View.VISIBLE);
            mTvRecordHint.setVisibility(View.VISIBLE);
        } else {
            mFlashButton.setVisibility(View.GONE);
            mCloseButton.setVisibility(View.GONE);
            mSwitchButton.setVisibility(View.GONE);
            mTvRecordHint.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        dealIconStatus(true);
    }

    @Override
    protected void onPause() {
        super.onPause();
        toggleFlash(true);

        if (isRecording) {
            isRecording = false;
            stopMode = 2;
            camera.stopVideo();
            mBtnRecord.stopCountDown();
            Toast.makeText(this, getString(R.string.album_record_error), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onVideoBack() {
        sCallback.onRecordBack(VideoPlayActivity.mSelectFile.getPath());
        finish();
    }

    public interface RecordCallback {

        /*** 预览完成回调*/
        void onRecordBack(String filePath);
    }
}
