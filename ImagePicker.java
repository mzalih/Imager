package com.prayerboard.starter.Util;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v7.app.AlertDialog;
import android.widget.ImageButton;
import android.widget.ImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * Created by toobler-mzalih on 14/08/17.
 */

public class ImagePicker {
    public static final int REQUEST_CAMERA =101;
    public static final int SELECT_FILE = 102;

    public static  String TAKE_PHOTO = "Take Photo";
    public static  String CHOOSE_GALLERY = "Choose from Library";
    public static  String CANCEL = "Cancel";

    public static  String SELECT_FILE_TITLE ="Select File";

    Activity activity;
    public static ImagePicker instance;


    public ImagePicker(Activity activity){
        this.activity =activity;
        instance = this;
    }
    private static ImagePicker getInstance(Activity activity){
        if(instance == null){
            instance =  new ImagePicker(activity);
        }
        return instance;
    }

    public static ImagePicker setListener(Activity activity,OnLoadListener listener){
        return getInstance(activity).setOnLoadListener(listener);
    }
    public static ImagePicker setSource(Activity activity,ImageView imageView){
        return getInstance(activity).setSource(imageView);
    }
    public static ImagePicker setSource(Activity activity,ImageButton imageView){
        return getInstance(activity).setSource(imageView);
    }
    public static void selectImage(Activity activity){
        getInstance(activity).selectImage();
    }
    public static void onActivityResult(Activity activity,int requestCode, int resultCode, Intent data){
        getInstance(activity).onActivityResult( requestCode,  resultCode,  data);
    }


    Bitmap image;
    ImageView imageView;
    ImageButton imageButton;
    OnLoadListener listener;
    public ImagePicker setOnLoadListener(OnLoadListener listener){
        this.listener =listener;
        return this;
    }
    public ImagePicker setSource(ImageView imageView){
        this.imageView =imageView;
        return  this;
    }
    public ImagePicker setSource(ImageButton imageButton){
        this.imageButton =imageButton;
        return  this;
    }
    public void selectImage() {

        final CharSequence[] items = {TAKE_PHOTO, CHOOSE_GALLERY,CANCEL};
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Add Photo!");
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int item) {
                if (items[item].equals(TAKE_PHOTO)) {
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    activity.startActivityForResult(intent, REQUEST_CAMERA);
                } else if (items[item].equals(CHOOSE_GALLERY)) {
                    Intent intent = new Intent(
                            Intent.ACTION_PICK,
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                    intent.setType("image/*");
                    activity.startActivityForResult(
                            Intent.createChooser(intent, SELECT_FILE_TITLE),
                            SELECT_FILE);
                } else if (items[item].equals(CANCEL)) {
                    dialog.dismiss();
                }
            }
        });
        builder.show();
    }
    private void getImage(Intent data){
        Uri uri = data.getData();
        try {
            image = MediaStore.Images.Media.getBitmap(activity.getContentResolver(), uri);
            // Log.d(TAG, String.valueOf(bitmap));

        } catch (IOException e) {
            e.printStackTrace();
            loadImage(data);
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CAMERA) {
                Bitmap thumbnail = (Bitmap) data.getExtras().get("data");
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                thumbnail.compress(Bitmap.CompressFormat.JPEG, 90, bytes);
                File destination = new File(Environment.getExternalStorageDirectory(),
                        System.currentTimeMillis() + ".jpg");
                FileOutputStream fo;
                try {
                    destination.createNewFile();
                    fo = new FileOutputStream(destination);
                    fo.write(bytes.toByteArray());
                    fo.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                image = thumbnail;
                setImage();
            } else if (requestCode == SELECT_FILE) {
                getImage(data);
                setImage();
            }
        }
    }

    private void loadImage(Intent data){
        try {
            Uri selectedImageUri = data.getData();
            String[] projection = {MediaStore.MediaColumns.DATA};
            Cursor cursor = activity.getContentResolver().query(selectedImageUri, projection, null, null,
                    null);
            int column_index = cursor.getColumnIndexOrThrow(MediaStore.MediaColumns.DATA);
            cursor.moveToFirst();
            String selectedImagePath = cursor.getString(column_index);
            if (selectedImagePath.startsWith("http://") || selectedImagePath.startsWith("https://")) {
                if (listener != null) {
                    listener.onLoad(selectedImagePath);
                }
                return;
            }
            Bitmap bm;
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(selectedImagePath, options);
            final int REQUIRED_SIZE = 200;
            int scale = 1;
            while (options.outWidth / scale / 2 >= REQUIRED_SIZE
                    && options.outHeight / scale / 2 >= REQUIRED_SIZE)
                scale *= 2;
            options.inSampleSize = scale;
            options.inJustDecodeBounds = false;
            bm = BitmapFactory.decodeFile(selectedImagePath, options);
            image = bm;
        }catch (Exception e){
            e.printStackTrace();
        }

    }

    private void setImage(){
        try{
            if(imageView!= null && image != null ){
                imageView.setImageBitmap(image);
            }
            if(imageButton!= null && image != null ){
                imageButton.setImageBitmap(image);
            }
            if(listener!=null){
                if(image != null){
                    listener.onLoad(image);
                }else{
                    listener.onError();
                }
            }
        }catch (Exception e){}
    }
    public interface OnLoadListener{
        public void onLoad(Bitmap imageBitmap);
        public void onLoad(String url);
        public void onError();
    };

}
