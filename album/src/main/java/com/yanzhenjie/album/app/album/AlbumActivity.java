package com.yanzhenjie.album.app.album;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaMetadataRetriever;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.PopupMenu;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.yanzhenjie.album.Action;
import com.yanzhenjie.album.Album;
import com.yanzhenjie.album.AlbumFile;
import com.yanzhenjie.album.AlbumFolder;
import com.yanzhenjie.album.Filter;
import com.yanzhenjie.album.R;
import com.yanzhenjie.album.api.widget.Widget;
import com.yanzhenjie.album.app.Contract;
import com.yanzhenjie.album.app.album.data.MediaReadTask;
import com.yanzhenjie.album.app.album.data.MediaReader;
import com.yanzhenjie.album.app.album.data.PathConversion;
import com.yanzhenjie.album.app.album.data.PathConvertTask;
import com.yanzhenjie.album.app.album.data.ThumbnailBuildTask;
import com.yanzhenjie.album.impl.OnItemClickListener;
import com.yanzhenjie.album.mvp.BaseActivity;
import com.yanzhenjie.album.util.AlbumUtils;
import com.yanzhenjie.album.widget.LoadingDialog;
import com.yanzhenjie.mediascanner.MediaScanner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>作者：hsicen  2019/11/6 14:49
 * <p>邮箱：codinghuang@163.com
 * <p>功能：
 * <p>描述：图片浏览页面
 */
public class AlbumActivity extends BaseActivity implements
        Contract.AlbumPresenter,
        MediaReadTask.Callback,
        GalleryActivity.Callback,
        VideoPlayActivity.VideoCallback,
        PathConvertTask.Callback,
        ThumbnailBuildTask.Callback {

    private static final int CODE_ACTIVITY_NULL = 1;
    private static final int CODE_PERMISSION_STORAGE = 1;

    public static Filter<Long> sSizeFilter;
    public static Filter<String> sMimeFilter;
    public static Filter<Long> sDurationFilter;

    public static Action<ArrayList<AlbumFile>> sResult;
    public static Action<String> sCancel;

    private List<AlbumFolder> mAlbumFolders;
    private int mCurrentFolder;

    private Widget mWidget;
    private int mFunction;
    private int mChoiceMode;
    private int mColumnCount;
    private boolean mHasCamera;
    private int mLimitCount;

    private int mQuality;
    private long mLimitDuration;
    private long mLimitBytes;

    private boolean mFilterVisibility;

    private ArrayList<AlbumFile> mCheckedList;
    private MediaScanner mMediaScanner;

    private Contract.AlbumView mView;
    private FolderDialog mFolderDialog;
    private PopupMenu mCameraPopupMenu;
    private LoadingDialog mLoadingDialog;

    private MediaReadTask mMediaReadTask;
    private Boolean takeBack = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        initializeArgument();
        setContentView(createView());

        mView = new AlbumView(this, this, mFunction);
        mView.setupViews(mWidget, mColumnCount, mHasCamera, mChoiceMode);
        mView.setTitle(mWidget.getTitle());
        mView.setCompleteDisplay(false);
        mView.setLoadingDisplay(true);

        requestPermission(PERMISSION_STORAGE, CODE_PERMISSION_STORAGE);
    }

    private void initializeArgument() {
        Bundle argument = getIntent().getExtras();
        assert argument != null;
        mWidget = argument.getParcelable(Album.KEY_INPUT_WIDGET);
        mFunction = argument.getInt(Album.KEY_INPUT_FUNCTION);
        mChoiceMode = argument.getInt(Album.KEY_INPUT_CHOICE_MODE);
        mColumnCount = argument.getInt(Album.KEY_INPUT_COLUMN_COUNT);
        mHasCamera = argument.getBoolean(Album.KEY_INPUT_ALLOW_CAMERA);
        mLimitCount = argument.getInt(Album.KEY_INPUT_LIMIT_COUNT);
        mQuality = argument.getInt(Album.KEY_INPUT_CAMERA_QUALITY);
        mLimitDuration = argument.getLong(Album.KEY_INPUT_CAMERA_DURATION);
        mLimitBytes = argument.getLong(Album.KEY_INPUT_CAMERA_BYTES);
        mFilterVisibility = argument.getBoolean(Album.KEY_INPUT_FILTER_VISIBILITY);
    }

    /*** 针对不同主题使用不同布局文件*/
    private int createView() {
        switch (mWidget.getUiStyle()) {
            case Widget.STYLE_DARK: {
                return R.layout.album_activity_album_dark;
            }
            case Widget.STYLE_LIGHT: {
                return R.layout.album_activity_album_light;
            }
            default: {
                throw new AssertionError("This should not be the case.");
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        mView.onConfigurationChanged(newConfig);
        if (mFolderDialog != null && !mFolderDialog.isShowing()) mFolderDialog = null;
    }

    @Override
    protected void onPermissionGranted(int code) {
        ArrayList<AlbumFile> checkedList = getIntent().getParcelableArrayListExtra(Album.KEY_INPUT_CHECKED_LIST);
        MediaReader mediaReader = new MediaReader(this, sSizeFilter, sMimeFilter, sDurationFilter, mFilterVisibility);
        mMediaReadTask = new MediaReadTask(mFunction, checkedList, mediaReader, this);
        mMediaReadTask.execute();
    }

    @Override
    protected void onPermissionDenied(int code) {
        new AlertDialog.Builder(this)
                .setCancelable(false)
                .setTitle(R.string.album_title_permission_failed)
                .setMessage(R.string.album_permission_storage_failed_hint)
                .setPositiveButton(R.string.album_ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        callbackCancel();
                    }
                })
                .show();
    }

    @Override
    public void onScanCallback(ArrayList<AlbumFolder> albumFolders, ArrayList<AlbumFile> checkedFiles) {
        mMediaReadTask = null;
        switch (mChoiceMode) {
            case Album.MODE_MULTIPLE: {
                mView.setCompleteDisplay(true);
                break;
            }
            case Album.MODE_SINGLE: {
                mView.setCompleteDisplay(false);
                break;
            }
            default: {
                throw new AssertionError("This should not be the case.");
            }
        }

        mView.setLoadingDisplay(false);
        mAlbumFolders = albumFolders;
        mCheckedList = checkedFiles;

        if (mAlbumFolders.get(0).getAlbumFiles().isEmpty()) {
            Intent intent = new Intent(this, NullActivity.class);
            intent.putExtras(getIntent());
            startActivityForResult(intent, CODE_ACTIVITY_NULL);
        } else {
            showFolderAlbumFiles(0);
            int count = mCheckedList.size();
            mView.setCheckedCount(count);
            mView.setSubTitle(count + "/" + mLimitCount);
        }
    }

    /*** 拍照和录制视频回调处理*/
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case CODE_ACTIVITY_NULL: {
                if (resultCode == RESULT_OK) {
                    String imagePath = NullActivity.parsePath(data);
                    String mimeType = AlbumUtils.getMimeType(imagePath);
                    if (!TextUtils.isEmpty(mimeType)) mCameraAction.onAction(imagePath);
                    Log.d("hsc", "拍照回调：" + System.currentTimeMillis());
                } else {
                    callbackCancel();
                }
                break;
            }
        }
    }

    @Override
    public void clickFolderSwitch() {
        if (mFolderDialog == null) {
            mFolderDialog = new FolderDialog(this, mWidget, mAlbumFolders, new OnItemClickListener() {
                @Override
                public void onItemClick(View view, int position) {
                    mCurrentFolder = position;
                    showFolderAlbumFiles(mCurrentFolder);
                }
            });
        }
        if (!mFolderDialog.isShowing()) mFolderDialog.show();
    }

    /*** 更新数据*/
    private void showFolderAlbumFiles(int position) {
        this.mCurrentFolder = position;
        AlbumFolder albumFolder = mAlbumFolders.get(position);
        mView.bindAlbumFolder(albumFolder);
    }

    /*** 点击拍照或录制视屏处理*/
    @Override
    public void clickCamera(View v) {
        int hasCheckSize = mCheckedList.size();
        if (hasCheckSize >= mLimitCount) {
            int messageRes;
            switch (mFunction) {
                case Album.FUNCTION_CHOICE_IMAGE: {
                    messageRes = R.plurals.album_check_image_limit_camera;
                    break;
                }
                case Album.FUNCTION_CHOICE_VIDEO: {
                    messageRes = R.plurals.album_check_video_limit_camera;
                    break;
                }
                case Album.FUNCTION_CHOICE_ALBUM: {
                    messageRes = R.plurals.album_check_album_limit_camera;
                    break;
                }
                default: {
                    throw new AssertionError("This should not be the case.");
                }
            }
            mView.toast(getResources().getQuantityString(messageRes, mLimitCount, mLimitCount));
        } else {
            switch (mFunction) {
                case Album.FUNCTION_CHOICE_IMAGE: {
                    takePicture();
                    break;
                }
                case Album.FUNCTION_CHOICE_VIDEO: {
                    takeVideo();
                    break;
                }
                case Album.FUNCTION_CHOICE_ALBUM: {
                    if (mCameraPopupMenu == null) {
                        mCameraPopupMenu = new PopupMenu(this, v);
                        mCameraPopupMenu.getMenuInflater().inflate(R.menu.album_menu_item_camera, mCameraPopupMenu.getMenu());
                        mCameraPopupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                int id = item.getItemId();
                                if (id == R.id.album_menu_camera_image) {
                                    takePicture();
                                } else if (id == R.id.album_menu_camera_video) {
                                    takeVideo();
                                }
                                return true;
                            }
                        });
                    }
                    mCameraPopupMenu.show();
                    break;
                }
                default: {
                    throw new AssertionError("This should not be the case.");
                }
            }
        }
    }

    /*** 点击拍照逻辑处理*/
    private void takePicture() {
        String filePath;
        if (mCurrentFolder == 0) {
            filePath = AlbumUtils.randomJPGPath();
        } else {
            File file = new File(mAlbumFolders.get(mCurrentFolder).getAlbumFiles().get(0).getPath());
            filePath = AlbumUtils.randomJPGPath(file.getParentFile());
        }
        Album.camera(this)
                .image()
                .filePath(filePath)
                .onResult(mCameraAction) //拍照回调处理
                .start();
    }

    /*** 点击录制视频逻辑处理*/
    private void takeVideo() {
        String filePath;
        if (mCurrentFolder == 0) {
            filePath = AlbumUtils.randomMP4Path();
        } else {
            File file = new File(mAlbumFolders.get(mCurrentFolder).getAlbumFiles().get(0).getPath());
            filePath = AlbumUtils.randomMP4Path(file.getParentFile());
        }
        Album.camera(this)
                .video()
                .filePath(filePath)
                .quality(mQuality)
                .limitDuration(mLimitDuration)
                .limitBytes(mLimitBytes)
                .onResult(mCameraAction) //拍摄回调处理
                .start();
    }

    /*** 拍照回调处理*/
    private Action<String> mCameraAction = new Action<String>() {
        @Override
        public void onAction(@NonNull String result) {
            Log.d("hsc", "列表页面回调：" + System.currentTimeMillis());
            if (mChoiceMode == Album.MODE_SINGLE && mFunction == Album.FUNCTION_CAMERA_VIDEO) {
                MediaMetadataRetriever media = new MediaMetadataRetriever();
                media.setDataSource(result);
                long duration = Long.parseLong(media.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)) / 1000;
                if (duration > 3 && duration <= 30) {
                    AlbumFile albumFile = new AlbumFile();
                    albumFile.setPath(result);
                    albumFile.setDuration(duration);
                    ArrayList<AlbumFile> tempList = new ArrayList<>();
                    tempList.add(albumFile);
                    onThumbnailCallback(tempList);

                    if (mMediaScanner == null) {
                        mMediaScanner = new MediaScanner(AlbumActivity.this);
                    }
                    mMediaScanner.scan(result);

                } else scanAndConvert(result);
            } else scanAndConvert(result);
        }
    };

    /*** 扫描文件库*/
    private void scanAndConvert(@NonNull String result) {
        if (mMediaScanner == null) {
            mMediaScanner = new MediaScanner(AlbumActivity.this);
        }
        mMediaScanner.scan(result);

        PathConversion conversion = new PathConversion(sSizeFilter, sMimeFilter, sDurationFilter);
        PathConvertTask task = new PathConvertTask(conversion, AlbumActivity.this);
        task.execute(result);
    }

    /*** 拍照返回开始转化*/
    @Override
    public void onConvertStart() {
        //showLoadingDialog();
        //mLoadingDialog.setMessage(R.string.album_converting);
    }

    /*** 转化结果回调*/
    @Override
    public void onConvertCallback(AlbumFile albumFile) {
        Log.d("hsc", "转化处理回调：" + System.currentTimeMillis());
        if (mChoiceMode == Album.MODE_SINGLE) {
            if (mFunction == Album.FUNCTION_CAMERA_VIDEO) {
                long duration = albumFile.getDuration() / 1000;
                if (duration <= 3) {
                    Toast.makeText(this, "请选择3s以上的视频", Toast.LENGTH_SHORT).show();
                    takeBack = true;
                    addFileToList(albumFile);
                    return;
                }

                if (duration > 30) {
                    Toast.makeText(this, "请选择30s以内的视频", Toast.LENGTH_SHORT).show();
                    takeBack = true;
                    addFileToList(albumFile);
                    return;
                }
            }

            ArrayList<AlbumFile> tempList = new ArrayList<>();
            tempList.add(albumFile);
            onThumbnailCallback(tempList);
        } else {
            //多选处理
            albumFile.setChecked(!albumFile.isDisable());
            if (albumFile.isDisable()) {
                if (mFilterVisibility) addFileToList(albumFile);
                else mView.toast(getString(R.string.album_take_file_unavailable));
            } else {
                addFileToList(albumFile);
            }
        }

        //dismissLoadingDialog();
    }

    private void addFileToList(AlbumFile albumFile) {
        if (mCurrentFolder != 0) {
            List<AlbumFile> albumFiles = mAlbumFolders.get(0).getAlbumFiles();
            if (albumFiles.size() > 0) albumFiles.add(0, albumFile);
            else albumFiles.add(albumFile);
        }

        AlbumFolder albumFolder = mAlbumFolders.get(mCurrentFolder);
        List<AlbumFile> albumFiles = albumFolder.getAlbumFiles();
        if (albumFiles.isEmpty()) {
            albumFiles.add(albumFile);
            mView.bindAlbumFolder(albumFolder);
        } else {
            albumFiles.add(0, albumFile);
            mView.notifyInsertItem(mHasCamera ? 1 : 0);
        }

        if (!takeBack) {
            mCheckedList.add(albumFile);
            int count = mCheckedList.size();
            mView.setCheckedCount(count);
            mView.setSubTitle(count + "/" + mLimitCount);
        }

        switch (mChoiceMode) {
            case Album.MODE_SINGLE: {
                if (takeBack) {
                    takeBack = false;
                } else callbackResult();
                break;
            }
            case Album.MODE_MULTIPLE: {
                // Nothing.
                break;
            }
            default: {
                throw new AssertionError("This should not be the case.");
            }
        }
    }

    /***点击选中框处理 */
    @Override
    public void tryCheckItem(CompoundButton button, int position) {
        AlbumFile albumFile = mAlbumFolders.get(mCurrentFolder).getAlbumFiles().get(position);
        if (button.isChecked()) {
            if (mCheckedList.size() >= mLimitCount) {
                int messageRes;
                switch (mFunction) {
                    case Album.FUNCTION_CHOICE_IMAGE: {
                        messageRes = R.plurals.album_check_image_limit;
                        break;
                    }
                    case Album.FUNCTION_CHOICE_VIDEO: {
                        messageRes = R.plurals.album_check_video_limit;
                        break;
                    }
                    case Album.FUNCTION_CHOICE_ALBUM: {
                        messageRes = R.plurals.album_check_album_limit;
                        break;
                    }
                    default: {
                        throw new AssertionError("This should not be the case.");
                    }
                }
                mView.toast(getResources().getQuantityString(messageRes, mLimitCount, mLimitCount));
                button.setChecked(false);
            } else {
                albumFile.setChecked(true);
                mCheckedList.add(albumFile);
                setCheckedCount();
            }
        } else {
            albumFile.setChecked(false);
            mCheckedList.remove(albumFile);
            setCheckedCount();
        }
    }

    private void setCheckedCount() {
        int count = mCheckedList.size();
        mView.setCheckedCount(count);
        mView.setSubTitle(count + "/" + mLimitCount);
    }

    /*** 点击预览处理*/
    @Override
    public void tryPreviewItem(int position) {
        switch (mChoiceMode) {
            case Album.MODE_SINGLE: {
                if (mFunction == Album.FUNCTION_CAMERA_VIDEO) {
                    AlbumFile albumFile = mAlbumFolders.get(mCurrentFolder).getAlbumFiles().get(position);
                    long duration = albumFile.getDuration() / 1000;

                    if (duration <= 3) {
                        Toast.makeText(this, "请选择3s以上的视频", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (duration > 30) {
                        Toast.makeText(this, "请选择30s以内的视频", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (albumFile.getSize() > 300 * 1024 * 1024) {  //B -> KB -> MB
                        Toast.makeText(this, "请选择300M以内的视频", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    VideoPlayActivity.sCallback = this;
                    VideoPlayActivity.mSelectFile = albumFile;
                    VideoPlayActivity.start(this, albumFile.getPath());
                } else {
                    AlbumFile albumFile = mAlbumFolders.get(mCurrentFolder).getAlbumFiles().get(position);
                    mCheckedList.add(albumFile);
                    setCheckedCount();
                    callbackResult();
                }

                break;
            }
            case Album.MODE_MULTIPLE: {
                GalleryActivity.sAlbumFiles = mAlbumFolders.get(mCurrentFolder).getAlbumFiles();
                GalleryActivity.sCheckedCount = mCheckedList.size();
                GalleryActivity.sCurrentPosition = position;
                GalleryActivity.sCallback = this;
                Intent intent = new Intent(this, GalleryActivity.class);
                intent.putExtras(getIntent());
                startActivity(intent);
                break;
            }
            default: {
                throw new AssertionError("This should not be the case.");
            }
        }
    }

    @Override
    public void tryPreviewChecked() {
        if (mCheckedList.size() > 0) {
            GalleryActivity.sAlbumFiles = new ArrayList<>(mCheckedList);
            GalleryActivity.sCheckedCount = mCheckedList.size();
            GalleryActivity.sCurrentPosition = 0;
            GalleryActivity.sCallback = this;
            Intent intent = new Intent(this, GalleryActivity.class);
            intent.putExtras(getIntent());
            startActivity(intent);
        }
    }

    @Override //视频预览回调处理
    public void onVideoBack() {
        ArrayList<AlbumFile> tempList = new ArrayList<>();
        tempList.add(VideoPlayActivity.mSelectFile);
        onThumbnailCallback(tempList);
    }

    @Override //预览图片回调处理
    public void onPreviewComplete() {
        callbackResult();
    }

    @Override
    public void onPreviewChanged(AlbumFile albumFile) {
        ArrayList<AlbumFile> albumFiles = mAlbumFolders.get(mCurrentFolder).getAlbumFiles();
        int position = albumFiles.indexOf(albumFile);
        int notifyPosition = mHasCamera ? position + 1 : position;
        mView.notifyItem(notifyPosition);

        if (albumFile.isChecked()) {
            if (!mCheckedList.contains(albumFile)) mCheckedList.add(albumFile);
        } else {
            if (mCheckedList.contains(albumFile)) mCheckedList.remove(albumFile);
        }
        setCheckedCount();
    }

    @Override
    public void complete() {
        if (mCheckedList.isEmpty()) {
            int messageRes;
            switch (mFunction) {
                case Album.FUNCTION_CHOICE_IMAGE: {
                    messageRes = R.string.album_check_image_little;
                    break;
                }
                case Album.FUNCTION_CHOICE_VIDEO: {
                    messageRes = R.string.album_check_video_little;
                    break;
                }
                case Album.FUNCTION_CHOICE_ALBUM: {
                    messageRes = R.string.album_check_album_little;
                    break;
                }
                default: {
                    throw new AssertionError("This should not be the case.");
                }
            }
            mView.toast(messageRes);
        } else {
            callbackResult();
        }
    }

    @Override
    public void onBackPressed() {
        if (mMediaReadTask != null) mMediaReadTask.cancel(true);
        callbackCancel();
    }

    /*** 选择结果回调.*/
    private void callbackResult() {
        ThumbnailBuildTask task = new ThumbnailBuildTask(this, mCheckedList, this);
        task.execute();
    }

    @Override
    public void onThumbnailStart() {
        //showLoadingDialog();
        //mLoadingDialog.setMessage(R.string.album_thumbnail);
    }

    @Override
    public void onThumbnailCallback(ArrayList<AlbumFile> albumFiles) {
        if (sResult != null) sResult.onAction(albumFiles);  //回调成功给调用层
        //dismissLoadingDialog();
        Log.d("hsc", "最终结束回调：" + System.currentTimeMillis());
        finish();
    }

    /*** 点击取消.*/
    private void callbackCancel() {
        if (sCancel != null) sCancel.onAction("User canceled."); //回调失败给调用层
        finish();
    }

    /*** 显示资源加载弹窗.*/
    private void showLoadingDialog() {
        if (mLoadingDialog == null) {
            mLoadingDialog = new LoadingDialog(this);
            mLoadingDialog.setupViews(mWidget);
        }
        if (!mLoadingDialog.isShowing()) {
            mLoadingDialog.show();
        }
    }

    /***取消资源加载弹窗.*/
    public void dismissLoadingDialog() {
        if (mLoadingDialog != null && mLoadingDialog.isShowing()) {
            mLoadingDialog.dismiss();
        }
    }

    @Override
    public void finish() {
        sSizeFilter = null;
        sMimeFilter = null;
        sDurationFilter = null;
        sResult = null;
        sCancel = null;
        super.finish();
    }
}
