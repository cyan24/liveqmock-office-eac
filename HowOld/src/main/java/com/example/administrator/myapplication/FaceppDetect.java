package com.example.administrator.myapplication;

import android.graphics.Bitmap;
import android.util.Log;

import com.facepp.error.FaceppParseException;
import com.facepp.http.HttpRequests;
import com.facepp.http.PostParameters;

import org.json.JSONObject;

import java.io.ByteArrayOutputStream;

/**
 * Created by Administrator on 2015/6/8.
 */
public class FaceppDetect {

    public interface CallBack{
        void success(JSONObject result);
        void error(FaceppParseException exception);
    }

    public static void detect(final Bitmap bm,final CallBack callBack){

        new Thread(new Runnable() {
            @Override
            public void run() {
                //request

                try {
                    HttpRequests requests=new HttpRequests(Constant.KEY,Constant.SECRET,true,true);

                    Bitmap bmsmall = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight());
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();

                    bmsmall.compress(Bitmap.CompressFormat.JPEG, 100, stream);
                    byte[] arrays = stream.toByteArray();

                    PostParameters postParameters = new PostParameters();
                    postParameters.setImg(arrays);

                    JSONObject jsonObject = requests.detectionDetect(postParameters);
                    Log.e("Tag",jsonObject.toString());
                    if(callBack!=null){
                        callBack.success(jsonObject);
                    }
                } catch (FaceppParseException e) {
                    e.printStackTrace();
                    if(callBack!=null){
                        callBack.error(e);
                    }
                }
            }
        }).start();
    }
}
