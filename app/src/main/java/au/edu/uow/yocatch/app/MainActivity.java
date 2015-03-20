package au.edu.uow.yocatch.app;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import model.YoCatchModel;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import utility.JsonParser;


import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;


public class MainActivity extends Activity{

    private static final String TAG = "INFO";

    private static final String POST_URL = "http://li671-166.members.linode.com/yo/addEntry.php";
    private static final String USERNAME = "kl103";

    private ArrayList<YoCatchModel> yoCatchModels;
    private EditText yoEditText;
    private EditText usernameEditText;
    private TextView messageTextView;
    private Button showMessageButton;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //GUI hookup
        this.yoEditText = (EditText) findViewById(R.id.yoEditText);
        this.usernameEditText = (EditText) findViewById(R.id.usernameEditText);
        this.messageTextView = (TextView) findViewById(R.id.messageTextView);
        this.showMessageButton = (Button) findViewById(R.id.showMessageButton);

        //Set listener
        showMessageButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View view) {

                changeActivity();
            }
        });

        this.yoCatchModels = new ArrayList();
    }


    private class PostMessageAsyncTask extends AsyncTask <YoCatchModel, Void, String> {

        private YoCatchModel model;
        @Override protected String doInBackground(YoCatchModel... model){

            this.model = model[0];
            String response = "";
            DefaultHttpClient client = new DefaultHttpClient();
            HttpPost httpPost = new HttpPost(POST_URL);
            httpPost.addHeader("Content-Type", "application/x-www-form-urlencoded");

            try {
                List<NameValuePair> params = new ArrayList<NameValuePair>(3);
                params.add(new BasicNameValuePair("yoMessage", model[0].getYoMessage()));
                params.add(new BasicNameValuePair("yoDestination", model[0].getDestination()));
                params.add(new BasicNameValuePair("username", model[0].getUsername()));
                httpPost.setEntity(new UrlEncodedFormEntity(params));

            } catch (Exception e){
                e.printStackTrace();
            }
            try {
                HttpResponse execute = client.execute(httpPost);
                InputStream content = execute.getEntity().getContent();

                BufferedReader buffer = new BufferedReader(new InputStreamReader(content));
                String s = "";
                while ((s = buffer.readLine()) != null){
                    response += s;
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return response;
        }
        protected void onPostExecute(String result){
            super.onPostExecute(result);
            //yoCatchModels.add(0, model);
            Log.d(TAG, result);
        }
    }

    private void playSound(String filename){
        mediaPlayer = new MediaPlayer();
        try {
            AssetFileDescriptor afd = getAssets().openFd(filename);
            mediaPlayer.setDataSource(afd.getFileDescriptor(),afd.getStartOffset(),afd.getLength());
            mediaPlayer.prepare();
            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mediaPlayer) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        mediaPlayer.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if(keyCode == KeyEvent.KEYCODE_ENTER){
            //Create model
            YoCatchModel model = new YoCatchModel(
                    usernameEditText.getText().toString(),
                    yoEditText.getText().toString()
            );
            model.setUsername(USERNAME);

            //Start asynctask
            new PostMessageAsyncTask().execute(model);

            //Update view
            updateView();
        }
        return true;
    }

    private void changeActivity(){
        Intent intent = new Intent(this, HistoryActivity.class);
        intent.putExtra("YoCatchModels", yoCatchModels);
        startActivity(intent);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void updateView(){
        //AUDIO
        try {
            String [] list = getAssets().list("");
            if (list.length > 0) {
                for (String file : list) {
                    if((yoEditText.getText().toString() + ".wav").equalsIgnoreCase(file)){
                        playSound(file);
                    }
                }
            }
        }catch (IOException e){
            System.out.println(e);
        }

        //Set text
        messageTextView.setText(yoEditText.getText().toString() + "\n" + usernameEditText.getText().toString());

        //Change background color
        final View view = findViewById(R.id.mainLayout);
        Random random = new Random();
        int r = random.nextInt(255);
        int g = random.nextInt(255);
        int b = random.nextInt(255);

        ColorDrawable currentColor = (ColorDrawable) view.getBackground();
        ColorDrawable nextColor = new ColorDrawable(Color.rgb(r, g, b));
        ColorDrawable nextColorInverted = new ColorDrawable(Color.rgb(255-r, 255-g, 255-b));

        //Start animation
        view.setBackgroundDrawable(nextColor);
        yoEditText.setTextColor(nextColorInverted.getColor());
        usernameEditText.setTextColor(nextColorInverted.getColor());
        messageTextView.setTextColor(nextColorInverted.getColor());
        showMessageButton.setTextColor(nextColorInverted.getColor());
        //End
        Integer colorFrom = currentColor.getColor();
        Integer colorTo = nextColor.getColor();

        ValueAnimator colorAnimation = ValueAnimator.ofObject(new ArgbEvaluator(), colorFrom, colorTo);
        colorAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {

            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                view.setBackgroundColor((Integer)animator.getAnimatedValue());

            }

        });
        colorAnimation.start();
    }
}
