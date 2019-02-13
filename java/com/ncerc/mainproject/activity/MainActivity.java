package com.ncerc.mainproject.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

import com.ncerc.mainproject.R;
import com.ncerc.mainproject.ResultHolder;
import com.otaliastudios.cameraview.CameraListener;
import com.otaliastudios.cameraview.CameraView;
import com.otaliastudios.cameraview.Gesture;
import com.otaliastudios.cameraview.GestureAction;
import com.theartofdev.edmodo.cropper.CropImage;

import org.opencv.android.OpenCVLoader;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class MainActivity extends AppCompatActivity
{
    private static final String TAG = "MainActivity";

    private static final int RESULT_LOAD_IMG = 1;

    // Used to load the 'native-lib' library on application startup.
    static
    {
        System.loadLibrary("native-lib");
    }

    // Used to check openCV is loaded or not
    static
    {
        if (!OpenCVLoader.initDebug())
        {
            Log.d(TAG, "Unable to load OpenCV");
        }
        else
        {
            Log.d(TAG, "OpenCV successfully loaded");
        }
    }

    private CameraView myCamera;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //Set the window fullscreen
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        setContentView(R.layout.activity_main);

        //Request for permissions
        if (checkSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.READ_EXTERNAL_STORAGE},
                    1);
        }

        myCamera = findViewById(R.id.my_camera_view);
        myCamera.mapGesture(Gesture.TAP, GestureAction.FOCUS_WITH_MARKER);
        myCamera.addCameraListener(new CameraListener()
        {
            @Override
            public void onPictureTaken(byte[] jpeg)
            {
                imageCaptured(jpeg);
            }
        });
    }

    // On capture button press
    public void captureImage(View view)
    {
        myCamera.capturePicture();
    }

    // On gallery button press
    public void selectImageFromGallery(View view)
    {
        Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
        photoPickerIntent.setType("image/*");
        startActivityForResult(photoPickerIntent, RESULT_LOAD_IMG);
    }

    // Capture the image and pass it to crop activity
    public void imageCaptured(byte[] jpeg)
    {
        Bitmap bitmap = BitmapFactory.decodeByteArray(jpeg, 0, jpeg.length);
        CropImage.activity(getImageUri(bitmap))
                .start(this);
    }

    public Uri getImageUri(Bitmap bitmap)
    {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
        String path = MediaStore.Images.Media.insertImage(this.getContentResolver(), bitmap, "MathCalc", null);
        return Uri.parse(path);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE)
        {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK)
            {
                Uri resultUri = result.getUri();
                try
                {
                    Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), resultUri);
                    ResultHolder.dispose();
                    ResultHolder.setImage(bitmap);

                    Intent intent = new Intent(this, PreviewActivity.class);
                    startActivity(intent);
                }
                catch (IOException e)
                {
                    Log.d(TAG, e.getMessage());
                }
            }
            else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE)
            {
                Exception error = result.getError();
                Log.d(TAG, error.getMessage());
            }
        }

        if (requestCode == RESULT_LOAD_IMG)
        {
            if (resultCode == RESULT_OK)
            {
                try
                {
                    final Uri imageUri = data.getData();
                    CropImage.activity(imageUri).start(this);
                }
                catch (Exception e)
                {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        myCamera.start();
    }

    @Override
    protected void onPause()
    {
        super.onPause();
        myCamera.stop();
    }

    @Override
    protected void onDestroy()
    {
        myCamera.destroy();
        super.onDestroy();
    }
}
