package com.ufind.andriodntest;

import android.Manifest;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

import com.orhanobut.logger.Logger;
import com.tbruyelle.rxpermissions.RxPermissions;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;

public class MainActivity extends AppCompatActivity {
    private ImageView mImageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mImageView = (ImageView) findViewById(R.id.iv_image);
    }

    public void photo(View view) {
        RxPermissions.getInstance(this).request(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE)
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<Boolean>() {
                    @Override
                    public void call(Boolean aBoolean) {
                        if (aBoolean) {
                            try {
                                takePhoto();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
    }


    public void startPhotoZoom(File srcFile, File output, int requestCode) {

        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(getImageContentUri(this, srcFile), "image/*");
        // crop为true是设置在开启的intent中设置显示的view可以剪裁
        intent.putExtra("crop", "true");

        // aspectX aspectY 是宽高的比例
        intent.putExtra("aspectX", 1);
        intent.putExtra("aspectY", 1);

        // outputX,outputY 是剪裁图片的宽高
        intent.putExtra("outputX", 400);
        intent.putExtra("outputY", 400);

        intent.putExtra("return-data", false);// true:不返回uri，false：返回uri
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(output));
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());

        startActivityForResult(intent, requestCode);
    }

    /**
     * 安卓7.0裁剪根据文件路径获取uri
     */
    public Uri getImageContentUri(Context context, File imageFile) {
        String filePath = imageFile.getAbsolutePath();
        Cursor cursor = context.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                MediaStore.Images.Media.DATA + "=? ",
                new String[]{filePath}, null);

        if (cursor != null && cursor.moveToFirst()) {
            int id = cursor.getInt(cursor
                    .getColumnIndex(MediaStore.MediaColumns._ID));
            Uri baseUri = Uri.parse("content://media/external/images/media");
            cursor.close();
            return Uri.withAppendedPath(baseUri, "" + id);
        } else {
            if (imageFile.exists()) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.DATA, filePath);
                return context.getContentResolver().insert(
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } else {
                return null;
            }
        }
    }


    private void takePhoto() throws IOException {
        // 指定调用相机拍照后照片的储存路径
        File imgFile = createImageFile();
        filePath = imgFile.getAbsolutePath();
        Uri imgUri;
        if (Build.VERSION.SDK_INT >= 24) {
            //如果是7.0或以上，使用getUriForFile()获取文件的Uri
            imgUri = FileProvider.getUriForFile(this, "com.ufind.andriodntest.file_provider", imgFile);
        } else {
            imgUri = Uri.fromFile(imgFile);
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imgUri);
        startActivityForResult(intent, 3333);
    }


    private String zoomResultPath;

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 3333 && resultCode == RESULT_OK) {
            try {
                zoomResultPath = createImageFile().getAbsolutePath();
            } catch (IOException e) {
                e.printStackTrace();
            }
            startPhotoZoom(new File(filePath), new File(zoomResultPath), 4444);
            scanFile();
        } else if (requestCode == 4444 && resultCode == RESULT_OK) {
            Bitmap bitmap = BitmapFactory.decodeFile(zoomResultPath);
            mImageView.setImageBitmap(bitmap);
        }
    }

    //把拍照的图片保存到相处中
    private void scanFile() {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(new File(filePath)));
        sendBroadcast(intent);
    }


    private String filePath;

    //创建保存图片的位置
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.CHINA).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = new File(Environment.getExternalStorageDirectory(), "pictures");
        //创建临时文件,文件前缀不能少于三个字符,后缀如果为空默认未".tmp"
        File image = File.createTempFile(
                imageFileName,  /* 前缀 */
                ".jpg",         /* 后缀 */
                storageDir      /* 文件夹 */
        );
        Logger.d("create file path = " + filePath);
        return image;
    }
}
