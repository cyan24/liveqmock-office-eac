package com.example.administrator.myapplication;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.facepp.error.FaceppParseException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.logging.LogRecord;


public class MainActivity extends Activity implements View.OnClickListener {

    private static final int PICK_CODE = 0X110;
    private Button mGetImage;
    private Button mDetect;
    private TextView mTip;
    private ImageView mPhoto;
    private View mWaitting;
    private Bitmap mPhotoImg;
    private String mCurrentPhotoSrc;
    private Paint mPaint;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initViews();
        initEvents();
        mPaint = new Paint();

    }

    private void resizePhoto() {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        double ratio = Math.max(options.outWidth* 1.0d / 1024f,options.outHeight *1.0d/1024f);

        options.inSampleSize  = (int) Math.ceil(ratio);

        options.inJustDecodeBounds= false;

        mPhotoImg = BitmapFactory.decodeFile(mCurrentPhotoSrc,options);


    }


    private void initEvents() {
        mGetImage.setOnClickListener(this);
        mDetect.setOnClickListener(this);
    }

    private void initViews() {
        mDetect = (Button) findViewById(R.id.id_detect);
        mGetImage = (Button)findViewById(R.id.id_getImage);
        mTip = (TextView)findViewById(R.id.id_tip);
        mWaitting = (View)findViewById(R.id.id_waitting);
        mPhoto = (ImageView)findViewById(R.id.id_photo);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        if(requestCode == PICK_CODE){
            if(intent != null){
                Uri uri = intent.getData();
                Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                cursor.moveToFirst();
                int  idx = cursor.getColumnIndex(MediaStore.Images.ImageColumns.DATA);
                mCurrentPhotoSrc = cursor.getString(idx);
                cursor.close();

                resizePhoto();
                mPhoto.setImageBitmap(mPhotoImg);
                mTip.setText("Click Detect ==>");
            }

        }
        super.onActivityResult(requestCode,resultCode,intent);
    }

    private static final int MSG_SUCCESS = 0X111;
    private static final int MSG_ERROR = 0X112;

    private Handler mhandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what){
                case MSG_SUCCESS:
                    mWaitting.setVisibility(View.GONE);
                    JSONObject rs = (JSONObject) msg.obj;
                    //解析结果
                    prepareRsBitmap(rs);
                    mPhoto.setImageBitmap(mPhotoImg);
                    break;
                case MSG_ERROR:
                    mWaitting.setVisibility(View.GONE);
                    String errorMsg =(String) msg.obj;
                    if(TextUtils.isEmpty(errorMsg)){
                        mTip.setText("Error");
                    }else {
                        mTip.setText(errorMsg);
                    }

                    break;
            }

            super.handleMessage(msg);
        }

    };


    private void prepareRsBitmap(JSONObject rs) {
        Bitmap bitmap = Bitmap.createBitmap(mPhotoImg.getWidth(),mPhotoImg.getHeight(),mPhotoImg.getConfig());
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(mPhotoImg,0,0,null);

        try {
            JSONArray faces = rs.getJSONArray("face");
            int faceCount = faces.length();
            mTip.setText("find "+faceCount);
            for (int i=0;i<faceCount;i++){
                //拿到单独FACE对象
                JSONObject face = faces.getJSONObject(i);
                JSONObject position = face.getJSONObject("position");
                float x = (float) position.getJSONObject("center").getDouble("x");
                float y = (float) position.getJSONObject("center").getDouble("y");
                float w = (float) position.getDouble("width");
                float h = (float) position.getDouble("height");

                x = x/100 * bitmap.getWidth();
                y = y/100 * bitmap.getHeight();
                w = w/100 * bitmap.getWidth();
                h = h/100 * bitmap.getHeight();
                mPaint.setColor(0xffffffff);
                mPaint.setStrokeWidth(3);
                //画box

                canvas.drawLine(x-w/2,y-h/2,x-w/2,y+h/2,mPaint);
                canvas.drawLine(x-w/2,y-h/2,x+w/2,y-h/2,mPaint);
                canvas.drawLine(x+w/2,y-h/2,x+w/2,y+h/2,mPaint);
                canvas.drawLine(x-w/2,y+h/2,x+w/2,y+h/2,mPaint);

                //get age and gender
                int age = face.getJSONObject("attribute").getJSONObject("age").getInt("value");
                String gender = face.getJSONObject("attribute").getJSONObject("gender").getString("value");
                //绘制年龄性别气泡图案
                Bitmap ageBitmap = buildAgeBitmap(age, "Male".equals(gender));

                //根据原图缩放气泡图案
                int ageWidth = ageBitmap.getWidth();
                int ageHeight = ageBitmap.getHeight();

                float ratio = Math.max(bitmap.getWidth() * 1.0f/mPhoto.getWidth(),
                        bitmap.getHeight() * 1.0f/mPhoto.getHeight());

                ageBitmap = Bitmap.createScaledBitmap(ageBitmap,(int)(ageWidth * ratio),
                        (int)(ageHeight * ratio),false);

                canvas.drawBitmap(ageBitmap,x-ageBitmap.getWidth()/2,y-h/2-ageBitmap.getHeight(),null);

                mPhotoImg = bitmap;


            }

        } catch (JSONException e) {
            e.printStackTrace();
        }


    }

    private Bitmap buildAgeBitmap(int age, boolean isMale) {
        TextView tv = (TextView)mWaitting.findViewById(R.id.id_ageAndGender);
        tv.setText(age + "");
        if(isMale){
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.male),null,null,null);
        }else{
            tv.setCompoundDrawablesWithIntrinsicBounds(getResources().getDrawable(R.drawable.female),null,null,null);
        }
        tv.setDrawingCacheEnabled(true);
        Bitmap bitmap = Bitmap.createBitmap(tv.getDrawingCache());
        tv.destroyDrawingCache();
        return  bitmap;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()){
            case R.id.id_getImage:
                Intent intent = new Intent(Intent.ACTION_PICK);
                intent.setType("image/*");
                startActivityForResult(intent,PICK_CODE);
                break;
            case R.id.id_detect:

                mWaitting.setVisibility(view.VISIBLE);//滚动等待显示

                //mCurrentPhotoSrc 预防空指针
                if(mCurrentPhotoSrc!=null && !"".equals(mCurrentPhotoSrc.trim())){
                    resizePhoto();
                }else {
                    mPhotoImg = BitmapFactory.decodeResource(getResources(),R.drawable.t4);
                }

                FaceppDetect.detect(mPhotoImg, new FaceppDetect.CallBack() {
                    @Override
                    public void success(JSONObject result) {
                        Message msg = Message.obtain();
                        msg.what = MSG_SUCCESS;
                        msg.obj = result;
                        mhandler.sendMessage(msg);
                    }

                    @Override
                    public void error(FaceppParseException exception) {
                        Message msg = Message.obtain();
                        msg.what = MSG_ERROR;
                        msg.obj = exception;
                        mhandler.sendMessage(msg);
                    }
                });


                break;
        }
    }
}
