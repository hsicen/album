package com.yanzhenjie.album.record;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.PointF;
import android.graphics.Rect;
import android.graphics.YuvImage;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.BottomSheetBehavior;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.yanzhenjie.album.AlbumFile;
import com.yanzhenjie.album.R;
import com.yanzhenjie.album.app.album.VideoPlayActivity;
import com.yanzhenjie.album.util.AlbumUtils;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.Arrays;
import java.util.List;

import cameraview.CameraException;
import cameraview.CameraListener;
import cameraview.CameraLogger;
import cameraview.CameraOptions;
import cameraview.CameraView;
import cameraview.PictureResult;
import cameraview.VideoResult;
import cameraview.controls.Flash;
import cameraview.controls.Mode;
import cameraview.controls.Preview;
import cameraview.frame.Frame;
import cameraview.frame.FrameProcessor;
import cameraview.markers.DefaultAutoFocusMarker;

public class VideoRecordActivity extends AppCompatActivity implements
        View.OnClickListener,
        VideoPlayActivity.VideoCallback,
        OptionView.Callback {

    public static RecordCallback sCallback;
    private final static CameraLogger LOG = CameraLogger.create("DemoApp");
    private final static boolean USE_FRAME_PROCESSOR = false;
    private final static boolean DECODE_BITMAP = true;
    private final static int RECORD_TIME = 30000;

    private CameraView camera;
    private ViewGroup controlPanel;
    private boolean isFlash = false;

    private ImageButton mFlashButton;
    private ImageButton mCloseButton;
    private ImageButton mSwitchButton;
    private TextView mTvRecordHint;
    private CountDownButton mBtnRecord;

    //视频暂停录制   1录制正常停止    2录制异常停止
    private int stopMode = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        hideStatusNavigationBar();
        setContentView(R.layout.activity_video_record);
        CameraLogger.setLogLevel(CameraLogger.LEVEL_VERBOSE);

        camera = findViewById(R.id.camera);
        camera.setLifecycleOwner(getLifecycle());
        camera.addCameraListener(new Listener());
        mFlashButton = findViewById(R.id.ib_light);
        mCloseButton = findViewById(R.id.ib_record_close);
        mSwitchButton = findViewById(R.id.toggleCamera);
        mTvRecordHint = findViewById(R.id.tv_record_hint);
        mBtnRecord = findViewById(R.id.captureVideo);

        //init listener
        mBtnRecord.setOnCountDownListener(new CountDownButton.OnCountDownListener() {
            @Override
            public void onTimeEnd() {
                //录制动画结束
                LOG.w("录制按钮结束录制");
            }
        });


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
                        //noinspection ResultOfMethodCallIgnored
                        bitmap.toString();
                    }
                }
            });
        }

        //findViewById(R.id.edit).setOnClickListener(this);   //参数设置
        findViewById(R.id.captureVideo).setOnClickListener(this);  // 点击拍照监听
        findViewById(R.id.toggleCamera).setOnClickListener(this); // 相机切换监听
        findViewById(R.id.ib_light).setOnClickListener(this);  //闪光灯切换监听
        findViewById(R.id.ib_record_close).setOnClickListener(this); //点击返回监听

        controlPanel = findViewById(R.id.controls);
        ViewGroup group = (ViewGroup) controlPanel.getChildAt(0);

        List<Option<?>> options = Arrays.asList(
                // Layout
                new Option.Width(), new Option.Height(),
                // Engine and preview
                new Option.Mode(), new Option.Engine(), new Option.Preview(),
                // Some controls
                new Option.Flash(), new Option.WhiteBalance(), new Option.Hdr(),
                new Option.PictureMetering(), new Option.PictureSnapshotMetering(),
                // Video recording
                new Option.PreviewFrameRate(), new Option.VideoCodec(), new Option.Audio(),
                // Gestures
                new Option.Pinch(), new Option.HorizontalScroll(), new Option.VerticalScroll(),
                new Option.Tap(), new Option.LongTap(),
                // Other
                new Option.Grid(), new Option.GridColor(), new Option.UseDeviceOrientation()
        );
        List<Boolean> dividers = Arrays.asList(
                // Layout
                false, true,
                // Engine and preview
                false, false, true,
                // Some controls
                false, false, false, false, true,
                // Video recording
                false, false, true,
                // Gestures
                false, false, false, false, true,
                // Watermarks
                false, false, true,
                // Other
                false, false, true
        );
        for (int i = 0; i < options.size(); i++) {
            OptionView view = new OptionView(this);
            //noinspection unchecked
            view.setOption(options.get(i), this);
            view.setHasDivider(dividers.get(i));
            group.addView(view,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        controlPanel.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
                b.setState(BottomSheetBehavior.STATE_HIDDEN);
            }
        });
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
            ViewGroup group = (ViewGroup) controlPanel.getChildAt(0);
            for (int i = 0; i < group.getChildCount(); i++) {
                OptionView view = (OptionView) group.getChildAt(i);
                view.onCameraOpened(camera, options);
            }

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
            /*if (camera.isTakingVideo()) {
                message("Captured while taking video. Size=" + result.getSize(), false);
                return;
            }

            // This can happen if picture was taken with a gesture.
            long callbackTime = System.currentTimeMillis();
            if (mCaptureTime == 0) mCaptureTime = callbackTime - 300;
            LOG.w("onPictureTaken called! Launching activity. Delay:", callbackTime - mCaptureTime);
            PicturePreviewActivity.setPictureResult(result);
            Intent intent = new Intent(CameraActivity.this, PicturePreviewActivity.class);
            intent.putExtra("delay", callbackTime - mCaptureTime);
            startActivity(intent);
            mCaptureTime = 0;
            LOG.w("onPictureTaken called! Launched activity.");*/
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
            if (duration <= 3) {
                Toast.makeText(VideoRecordActivity.this, "视频需录制3s以上", Toast.LENGTH_SHORT).show();
                dealIconStatus(true);
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
            mBtnRecord.startCountDown();
            stopMode = 1;
            dealIconStatus(false);
            LOG.w("onVideoRecordingStart!");
        }

        @Override
        public void onVideoRecordingEnd() {
            super.onVideoRecordingEnd();
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
        if (id == R.id.edit) {
            edit();
        } else if (id == R.id.captureVideo) {
            turnOffAudio(true);
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

    @Override
    public void onBackPressed() {
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        if (b.getState() != BottomSheetBehavior.STATE_HIDDEN) {
            b.setState(BottomSheetBehavior.STATE_HIDDEN);
            return;
        }
        super.onBackPressed();
    }

    private void edit() {
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        b.setState(BottomSheetBehavior.STATE_COLLAPSED);
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

        camera.takeVideo(new File(AlbumUtils.randomMP4Path(this)), RECORD_TIME);
    }

    private void toggleCamera() {
        if (camera.isTakingPicture() || camera.isTakingVideo()) return;
        switch (camera.toggleFacing()) {
            case BACK:
                message("Switched to back camera!", false);
                break;

            case FRONT:
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
    public <T> boolean onValueChanged(@NonNull Option<T> option, @NonNull T value, @NonNull String name) {
        if ((option instanceof Option.Width || option instanceof Option.Height)) {
            Preview preview = camera.getPreview();
            boolean wrapContent = (Integer) value == ViewGroup.LayoutParams.WRAP_CONTENT;
            if (preview == Preview.SURFACE && !wrapContent) {
                message("The SurfaceView preview does not support width or height changes. " +
                        "The view will act as WRAP_CONTENT by default.", true);
                return false;
            }
        }
        option.set(camera, value);
        BottomSheetBehavior b = BottomSheetBehavior.from(controlPanel);
        b.setState(BottomSheetBehavior.STATE_HIDDEN);
        message("Changed " + option.getName() + " to " + name, false);
        return true;
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

    public static void start(Activity mAct) {
        Intent intent = new Intent(mAct, VideoRecordActivity.class);
        mAct.startActivity(intent);
        mAct.overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    private void hideStatusNavigationBar() {
        int uiFlags = View.INVISIBLE | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LOW_PROFILE
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        getWindow().getDecorView().setSystemUiVisibility(uiFlags);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);
    }

    /*** 处理Icon状态*/
    private void dealIconStatus(boolean isShow) {
        if (isShow) {
            mFlashButton.setVisibility(View.VISIBLE);
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

        if (camera.isTakingVideo() || camera.isTakingPicture()) {
            stopMode = 2;
            camera.stopVideo();
            mBtnRecord.stopCountDown();
            Toast.makeText(this, "视频录制失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void turnOffAudio(boolean isTurnOff) {
        ((AudioManager) getSystemService(Context.AUDIO_SERVICE)).setSpeakerphoneOn(false);
    }

    @Override
    public void onVideoBack() {
        finish();
        sCallback.onRecordBack();
    }

    public interface RecordCallback {

        /*** 预览完成回调*/
        void onRecordBack();
    }
}
