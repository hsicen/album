package com.yanzhenjie.album.app.album;

import android.os.Bundle;
import androidx.annotation.Nullable;

import com.yanzhenjie.album.Album;
import com.yanzhenjie.album.AlbumFile;
import com.yanzhenjie.album.R;
import com.yanzhenjie.album.api.widget.Widget;
import com.yanzhenjie.album.app.Contract;
import com.yanzhenjie.album.app.gallery.GalleryView;
import com.yanzhenjie.album.mvp.BaseActivity;
import com.yanzhenjie.album.util.AlbumUtils;

import java.util.ArrayList;

/**
 * <p>作者：hsicen  2019/11/7 11:20
 * <p>邮箱：codinghuang@163.com
 * <p>功能：
 * <p>描述：选择预览
 */
public class GalleryActivity extends BaseActivity implements Contract.GalleryPresenter {

    public static ArrayList<AlbumFile> sAlbumFiles; //所有文件
    public static int sCheckedCount; //选中数量
    public static int sCurrentPosition;  //当前位置

    private Widget mWidget;
    private int mFunction;
    private int mAllowSelectCount;

    public static Callback sCallback;
    private Contract.GalleryView<AlbumFile> mView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.album_activity_gallery);
        mView = new GalleryView<>(this, this);
        Bundle argument = getIntent().getExtras();
        assert argument != null;
        mWidget = argument.getParcelable(Album.KEY_INPUT_WIDGET);
        mFunction = argument.getInt(Album.KEY_INPUT_FUNCTION);
        mAllowSelectCount = argument.getInt(Album.KEY_INPUT_LIMIT_COUNT);

        mView.setupViews(mWidget, true);
        mView.bindData(sAlbumFiles);

        if (sCurrentPosition == 0) {
            onCurrentChanged(sCurrentPosition);
        } else {
            mView.setCurrentItem(sCurrentPosition);
        }
        setCheckedCount();
    }

    private void setCheckedCount() {
        String completeText = getString(R.string.album_menu_finish);
        completeText += "(" + sCheckedCount + " / " + mAllowSelectCount + ")";
        mView.setCompleteText(completeText);
    }

    @Override
    public void clickItem(int position) {
    }

    @Override
    public void longClickItem(int position) {
    }

    @Override
    public void onCurrentChanged(int position) {
        sCurrentPosition = position;
        mView.setTitle(sCurrentPosition + 1 + " / " + sAlbumFiles.size());

        AlbumFile albumFile = sAlbumFiles.get(position);
        mView.setChecked(albumFile.isChecked());
        mView.setLayerDisplay(albumFile.isDisable());

        if (albumFile.getMediaType() == AlbumFile.TYPE_VIDEO) {
            mView.setDuration(AlbumUtils.convertDuration(albumFile.getDuration()));
            mView.setDurationDisplay(true);
        } else {
            mView.setDurationDisplay(false);
        }
    }

    @Override
    public void onCheckedChanged() {
        AlbumFile albumFile = sAlbumFiles.get(sCurrentPosition);
        if (albumFile.isChecked()) {
            albumFile.setChecked(false);
            sCallback.onPreviewChanged(albumFile);
            sCheckedCount--;
        } else {
            if (sCheckedCount >= mAllowSelectCount) {
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
                mView.toast(getResources().getQuantityString(messageRes, mAllowSelectCount, mAllowSelectCount));
                mView.setChecked(false);
            } else {
                albumFile.setChecked(true);
                sCallback.onPreviewChanged(albumFile);
                sCheckedCount++;
            }
        }

        setCheckedCount();
    }

    @Override
    public void complete() {
        if (sCheckedCount == 0) {
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
            sCallback.onPreviewComplete();
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }

    @Override
    public void finish() {
        sAlbumFiles = null;
        sCheckedCount = 0;
        sCurrentPosition = 0;
        sCallback = null;
        super.finish();
    }

    public interface Callback {

        /*** 预览完成回调*/
        void onPreviewComplete();

        /*** 预览改动回调*/
        void onPreviewChanged(AlbumFile albumFile);
    }
}