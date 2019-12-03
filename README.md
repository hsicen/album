# 相册和视频拍摄
- 相册参考[这个项目修改](https://github.com/yanzhenjie/Album)，如需使用，请支持原项目
- 视频参考[这个项目修改](https://github.com/natario1/CameraView)，如需使用，请支持原项目

## Download
```groovy
implementation 'com.github.Hsicen:album:latest_version'
```

## Usage
自定义图片加载Loader

```java
public class MediaLoader implements AlbumLoader {

    @Override
    public void load(ImageView imageView, AlbumFile albumFile) {
        load(imageView, albumFile.getPath());
    }

    @Override
    public void load(ImageView imageView, String url) {
        Glide.with(imageView.getContext())
                .load(url)
                .error(R.drawable.placeholder)
                .placeholder(R.drawable.placeholder)
                .crossFade()
                .into(imageView);
    }
}
```
在使用之前先初始化
```java
Album.initialize(AlbumConfig.newBuilder(this)
    .setAlbumLoader(new MediaLoader())
    ...
    .build());
```

### 图片视频混合选择
```java
Album.album(this) // Image and video mix options.
    .multipleChoice() // Multi-Mode, Single-Mode: singleChoice().
    .columnCount() // The number of columns in the page list.
    .selectCount()  // Choose up to a few images.
    .camera() // Whether the camera appears in the Item.
    .cameraVideoQuality(1) // Video quality, [0, 1].
    .checkedList() // To reverse the list.
    .filterSize() // Filter the file size.
    .filterMimeType() // Filter file format.
    .filterDuration() // Filter video duration.
    .afterFilterVisibility() // Show the filtered files, but they are not available.
    .onResult(new Action<ArrayList<AlbumFile>>() {
        @Override
        public void onAction(@NonNull ArrayList<AlbumFile> result) {
            // TODO accept the result.
        }
    })
    .onCancel(new Action<String>() {
        @Override
        public void onAction(@NonNull String result) {
            // The user canceled the operation.
        }
    })
    .start();
```

### 图片选择
```java
Album.image(this) // Image selection.
    .multipleChoice()
    .camera()
    .columnCount()
    .selectCount()
    .checkedList(mAlbumFiles)
    .filterSize() // Filter the file size.
    .filterMimeType() // Filter file format.
    .afterFilterVisibility() // Show the filtered files, but they are not available.
    .onResult(new Action<ArrayList<AlbumFile>>() {
        @Override
        public void onAction(@NonNull ArrayList<AlbumFile> result) {
        }
    })
    .onCancel(new Action<String>() {
        @Override
        public void onAction(@NonNull String result) {
        }
    })
    .start();
```

### 视频选择
```java
Album.video(this) // Video selection.
    .multipleChoice()
    .camera(true)
    .columnCount(2)
    .selectCount(6)
    .checkedList(mAlbumFiles)
    .filterSize()
    .maxDuration(30)  //最大拍摄时长，单位秒
    .minDuration(3)  //最小拍摄时长，单位秒
    .filterMimeType()
    .filterDuration()
    .afterFilterVisibility() // Show the filtered files, but they are not available.
    .onResult(new Action<ArrayList<AlbumFile>>() {
        @Override
        public void onAction(@NonNull ArrayList<AlbumFile> result) {
        }
    })
    .onCancel(new Action<String>() {
        @Override
        public void onAction(@NonNull String result) {
        }
    })
    .start();
```

### 直接调用系统相机拍照
```java
Album.camera(this) // Camera function.
    .image() // Take Picture.
    .filePath() // File save path, not required.
    .onResult(new Action<String>() {
        @Override
        public void onAction(@NonNull String result) {
        }
    })
    .onCancel(new Action<String>() {
        @Override
        public void onAction(@NonNull String result) {
        }
    })
    .start();
```


### 直接调用视频录制
```java
VideoRecordActivity.sCallback = this;  // 实现视频录制回调接口 (后期优化)
VideoRecordActivity.start(this, 30, 10);
```

### 自定义UI样式(只能自定义颜色)
Through `Widget`, developer can configure the title, color of StatusBar, color of NavigationBar and so on.

```java
// Such as image video mix:
 Album.album(this)
    .multipleChoice()
    .widget(...)
    ...

// Image selection:
Album.image(this)
    .multipleChoice()
    .widget(...)
    ...

// Video selection:
Album.video(this)
    .multipleChoice()
    .widget(...)
    ...

// Gallery, preview AlbumFile:
Album.galleryAlbum(this)
    .widget(...)
    ...

// Gallery, preview path:
Album.gallery(this)
    .widget(...)
    ...
```

So we only need to pass in a `Widget` parameter just fine:
```java
// StatusBar is a dark background when building:
Widget.newDarkBuilder(this)
...

// StatusBar is a light background when building:
Widget.newLightBuilder(this)
...

// Such as:
Widget.xxxBuilder(this)
    .title(...) // Title.
    .statusBarColor(Color.WHITE) // StatusBar color.
    .toolBarColor(Color.WHITE) // Toolbar color.
    .navigationBarColor(Color.WHITE) // Virtual NavigationBar color of Android5.0+.
    .mediaItemCheckSelector(Color.BLUE, Color.GREEN) // Image or video selection box.
    .bucketItemCheckSelector(Color.RED, Color.YELLOW) // Select the folder selection box.
    .buttonStyle( // Used to configure the style of button when the image/video is not found.
        Widget.ButtonStyle.newLightBuilder(this) // With Widget's Builder model.
            .setButtonSelector(Color.WHITE, Color.WHITE) // Button selector.
            .build()
    )
    .build()
```

## Contributing
Before submitting pull requests, contributors must abide by the [agreement](CONTRIBUTING.md) .

## Proguard-rules
If you are using ProGuard you might need to add the following options:
```txt
-dontwarn com.yanzhenjie.album.**
-dontwarn com.yanzhenjie.mediascanner.**
```