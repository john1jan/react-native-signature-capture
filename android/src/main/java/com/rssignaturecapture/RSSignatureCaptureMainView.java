package com.rssignaturecapture;

import android.util.Log;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.facebook.react.uimanager.ThemedReactContext;
import com.facebook.react.bridge.WritableNativeMap;

import com.facebook.react.uimanager.events.RCTEventEmitter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ByteArrayOutputStream;

import android.util.Base64;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.os.Environment;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;

import android.app.Activity;
import android.content.pm.ActivityInfo;

public class RSSignatureCaptureMainView extends LinearLayout implements OnClickListener,RSSignatureCaptureView.SignatureCallback {
  LinearLayout buttonsLayout;
  RSSignatureCaptureView signatureView;

  Activity mActivity;
  int mOriginalOrientation;

  public RSSignatureCaptureMainView(Context context, Activity activity) {
    super(context);

    mOriginalOrientation = activity.getRequestedOrientation();
    mActivity = activity;
    activity.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

    this.setOrientation(LinearLayout.VERTICAL);

    this.buttonsLayout = this.buttonsLayout();
    this.signatureView = new RSSignatureCaptureView(context,this);

    // add the buttons and signature views
    this.addView(this.buttonsLayout);
    this.addView(signatureView);

  }

  @Override
  protected void onDetachedFromWindow() {
    mActivity.setRequestedOrientation(mOriginalOrientation);
  }

  private LinearLayout buttonsLayout() {

    // create the UI programatically
    LinearLayout linearLayout = new LinearLayout(this.getContext());
    Button saveBtn = new Button(this.getContext());
    Button clearBtn = new Button(this.getContext());

    // set orientation
    linearLayout.setOrientation(LinearLayout.HORIZONTAL);
    linearLayout.setBackgroundColor(Color.WHITE);

    // set texts, tags and OnClickListener
    saveBtn.setText("Save");
    saveBtn.setTag("Save");
    saveBtn.setOnClickListener(this);

    clearBtn.setText("Reset");
    clearBtn.setTag("Reset");
    clearBtn.setOnClickListener(this);

    linearLayout.addView(saveBtn);
    linearLayout.addView(clearBtn);

    // return the whoe layout
    return linearLayout;
  }

  // the on click listener of 'save' and 'clear' buttons
  @Override
  public void onClick(View v) {
    String tag = v.getTag().toString().trim();

    // save the signature
    if (tag.equalsIgnoreCase("save")) {
      this.saveImage(this.signatureView.getSignature());
    }

    // empty the canvas
    else {
      this.signatureView.clearSignature();
    }

  }

  /**
  * save the signature to an sd card directory
  * @param signature bitmap
  */
  final void saveImage(Bitmap signature) {

    String root = Environment.getExternalStorageDirectory().toString();

    // the directory where the signature will be saved
    File myDir = new File(root + "/saved_signature");

    // make the directory if it does not exist yet
    if (!myDir.exists()) {
      myDir.mkdirs();
    }

    // set the file name of your choice
    String fname = "signature.png";

    // in our case, we delete the previous file, you can remove this
    File file = new File(myDir, fname);
    if (file.exists()) {
      file.delete();
    }

    try {

      // save the signature
      FileOutputStream out = new FileOutputStream(file);
      signature.compress(Bitmap.CompressFormat.PNG, 90, out);
      out.flush();
      out.close();

      ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
      signature.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream);
      byte[] byteArray = byteArrayOutputStream.toByteArray();
      String encoded = Base64.encodeToString(byteArray, Base64.DEFAULT);

      ((ThemedReactContext)this.getContext())
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
            .emit("onSaveEvent", getJSMap(file.getAbsolutePath(), encoded));

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private WritableNativeMap getJSMap(String pathName, String encoded) {
   WritableNativeMap params = new WritableNativeMap();
   params.putString("pathName", pathName);
   params.putString("encoded", encoded);
   return params;
 }

  @Override public void onDragged() {
    WritableMap event = Arguments.createMap();
    event.putBoolean("dragged", true);
    ReactContext reactContext = (ReactContext) getContext();
    reactContext.getJSModule(RCTEventEmitter.class).receiveEvent(getId(), "topChange", event);

  }
}
