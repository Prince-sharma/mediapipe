// Copyright 2019 The MediaPipe Authors.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.mediapipe.apps.edgedetectiongpu;

import android.graphics.SurfaceTexture;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import com.google.mediapipe.components.CameraHelper;
import com.google.mediapipe.components.CameraXPreviewHelper;
import com.google.mediapipe.components.ExternalTextureConverter;
import com.google.mediapipe.components.FrameProcessor;
import com.google.mediapipe.components.PermissionHelper;
import com.google.mediapipe.framework.AndroidAssetUtil;
import com.google.mediapipe.glutil.EglManager;
import android.graphics.Bitmap;
import android.util.Base64;
import android.content.Intent;
import android.provider.MediaStore;
import java.io.ByteArrayOutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.BufferedReader;


/** Bare-bones main activity. */
public class MainActivity extends AppCompatActivity {

  // Co-ordinates of hand and face
  private float[]  hand_coOrdinates;


  private static final String BINARY_GRAPH_NAME = "edgedetectiongpu.binarypb";
  private static final String INPUT_VIDEO_STREAM_NAME = "input_video";
  private static final String OUTPUT_VIDEO_STREAM_NAME = "output_video";
  private static final CameraHelper.CameraFacing CAMERA_FACING = CameraHelper.CameraFacing.FRONT;
  static final int REQUEST_IMAGE_CAPTURE = 1;
  public static String strBitMap;



  

  // Flips the camera-preview frames vertically before sending them into FrameProcessor to be
  // processed in a MediaPipe graph, and flips the processed frames back when they are displayed.
  // This is needed because OpenGL represents images assuming the image origin is at the bottom-left
  // corner, whereas MediaPipe in general assumes the image origin is at top-left.
  private static final boolean FLIP_FRAMES_VERTICALLY = true;

  static {
    // Load all native libraries needed by the app.
    System.loadLibrary("mediapipe_jni");
    System.loadLibrary("opencv_java4");
  }

  // {@link SurfaceTexture} where the camera-preview frames can be accessed.
  private SurfaceTexture previewFrameTexture;
  // Sends camera-preview frames into a MediaPipe graph for processing, and displays the processed
  // frames onto a {@link Surface}.
  private FrameProcessor processor;
  // {@link SurfaceView} that displays the camera-preview frames processed by a MediaPipe graph.
  private SurfaceView previewDisplayView;

  // Creates and manages an {@link EGLContext}.
  private EglManager eglManager;
  // Converts the GL_TEXTURE_EXTERNAL_OES texture from Android camera into a regular texture to be
  // consumed by {@link FrameProcessor} and the underlying MediaPipe graph.
  private ExternalTextureConverter converter;

  // Handles camera access via the {@link CameraX} Jetpack support library.
  private CameraXPreviewHelper cameraHelper;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    previewDisplayView = new SurfaceView(this);
    setupPreviewDisplayView();

    // Initialize asset manager so that MediaPipe native libraries can access the app assets, e.g.,
    // binary graphs.
    AndroidAssetUtil.initializeNativeAssetManager(this);

    eglManager = new EglManager(null);
    processor =
        new FrameProcessor(
            this,
            eglManager.getNativeContext(),
            BINARY_GRAPH_NAME,
            INPUT_VIDEO_STREAM_NAME,
            OUTPUT_VIDEO_STREAM_NAME);
    processor.getVideoSurfaceOutput().setFlipY(FLIP_FRAMES_VERTICALLY);

     float [] face_Rectangle= processor.get_face_rect();
    for(float x: face_Rectangle) {
      Log.e("face rect values", Float.toString(x));
    }
     float [] hand_Rectangle_landmark = processor.get_hand_rect_landmark();
    for(float x: hand_Rectangle_landmark) {
      Log.e("hand rect values", Float.toString(x));
    }

    if(hand_Rectangle_landmark[0] == 0 && face_Rectangle[0] == 0){

      dispatchTakePictureIntent();

    }



    PermissionHelper.checkAndRequestCameraPermissions(this);
  }

  @Override
  protected void onResume() {
    super.onResume();
    converter = new ExternalTextureConverter(eglManager.getContext());
    converter.setFlipY(FLIP_FRAMES_VERTICALLY);
    converter.setConsumer(processor);
    if (PermissionHelper.cameraPermissionsGranted(this)) {
      startCamera();
    }

    private void dispatchTakePictureIntent() {
    Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
    if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
      startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
    }
  }

  @Override
  public void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
      Bundle extras = data.getExtras();
      Bitmap imageBitmap = (Bitmap) extras.get("data");
      String strBitMap = BitMapToString(imageBitmap);
      Log.v("image base64", strBitMap );
     // imageView.setImageBitmap(imageBitmap);
    }
  }

  public String BitMapToString(Bitmap bitmap) {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
    byte[] b = baos.toByteArray();
    String strBitMap = Base64.encodeToString(b, Base64.DEFAULT);
    return strBitMap;
  }

  }

  public static void main(String[] args) throws IOException {

    //GetAndPost.MyGETRequest();

    MyPOSTRequest();
  ;

}

public static void MyPOSTRequest() throws IOException {

    URL urlForPostRequest = new URL("https://er50bch4c1.execute-api.ap-south-1.amazonaws.com/beta/identify");

    String readLine = null;

    HttpURLConnection conection = (HttpURLConnection) urlForPostRequest.openConnection();

    conection.setRequestMethod("POST");

    conection.setRequestProperty("image_base_64", strBitMap); // set userId its a sample here

    int responseCode = conection.getResponseCode();

    if (responseCode == HttpURLConnection.HTTP_OK) {

        BufferedReader in = new BufferedReader(

            new InputStreamReader(conection.getInputStream()));

        StringBuffer response = new StringBuffer();

        while ((readLine = in .readLine()) != null) {

            response.append(readLine);

        } in .close();

        // print result

        System.out.println("JSON String Result " + response.toString());

        //GetAndPost.POSTRequest(response.toString());

    } else {

        System.out.println("GET NOT WORKED");

    }

}


  @Override
  protected void onPause() {
    super.onPause();
    converter.close();
  }

  @Override
  public void onRequestPermissionsResult(
      int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    PermissionHelper.onRequestPermissionsResult(requestCode, permissions, grantResults);
  }

  public void startCamera() {
    cameraHelper = new CameraXPreviewHelper();
    cameraHelper.setOnCameraStartedListener(
        surfaceTexture -> {
          previewFrameTexture = surfaceTexture;
          // Make the display view visible to start showing the preview. This triggers the
          // SurfaceHolder.Callback added to (the holder of) previewDisplayView.
          previewDisplayView.setVisibility(View.VISIBLE);
        });
    cameraHelper.startCamera(this, CAMERA_FACING, /*surfaceTexture=*/ null);
  }

  private void setupPreviewDisplayView() {
    previewDisplayView.setVisibility(View.GONE);
    ViewGroup viewGroup = findViewById(R.id.preview_display_layout);
    viewGroup.addView(previewDisplayView);
    
      
    previewDisplayView
        .getHolder()
        .addCallback(
            new SurfaceHolder.Callback() {
              @Override
              public void surfaceCreated(SurfaceHolder holder) {
                processor.getVideoSurfaceOutput().setSurface(holder.getSurface());
              }

              @Override
              public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
                // (Re-)Compute the ideal size of the camera-preview display (the area that the
                // camera-preview frames get rendered onto, potentially with scaling and rotation)
                // based on the size of the SurfaceView that contains the display.
                Size viewSize = new Size(width, height);
                Size displaySize = cameraHelper.computeDisplaySizeFromViewSize(viewSize);

                // Connect the converter to the camera-preview frames as its input (via
                // previewFrameTexture), and configure the output width and height as the computed
                // display size.
                converter.setSurfaceTextureAndAttachToGLContext(
                    previewFrameTexture, displaySize.getWidth(), displaySize.getHeight());
              }

              @Override
              public void surfaceDestroyed(SurfaceHolder holder) {
                processor.getVideoSurfaceOutput().setSurface(null);
              }
            });
  }
}


