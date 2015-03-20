package au.edu.uow.yocatch.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.media.ThumbnailUtils;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Environment;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.*;
import android.widget.*;
import model.YoCatchModel;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import utility.JsonParser;

import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;


/* The YoCatchModels are fetched via a AsyncTask.
 * This AsyncTask starts another AsyncTask, which
 * downloads the images.
 *
 * When the user taps a message, the audiofile will be
 * downloaded using a AsyncTask if the file does not
 * already exist.
 */


public class HistoryActivity extends ActionBarActivity {

    private static final String TAG = "HISTORY";
    private static final String RETRIEVE_URL = "http://li671-166.members.linode.com/yo/retrieveAllEntries.php";
    private static final String DIRECTORY_PATH = Environment.getExternalStorageDirectory().getPath() + "/";

    private ArrayList<YoCatchModel> yoCatchModels;
    private ListView listView;
    private CustomListAdapter adapter;
    private MediaPlayer mediaPlayer;

    private Bitmap currentImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        //Fetch models
        this.yoCatchModels = new ArrayList();
        fetchModels();

        //UI Hookup
        this.listView = (ListView) findViewById(R.id.listView);

        //Listadapter
        this.adapter = new CustomListAdapter(this, yoCatchModels);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                YoCatchModel model = yoCatchModels.get((int) id);
                //Get filename
                String fileName = model.getAudioUrl().split("/")[5];
                File file = new File(DIRECTORY_PATH + fileName);
                // Check if the Music file already exists
                if (file.exists()) {
                    //Play
                    playSound(DIRECTORY_PATH + fileName);
                } else {
                    //Download and play
                    new DownloadAudioAsyncTask().execute(model.getAudioUrl());
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.history, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.action_refresh:
                fetchModels();
                Toast.makeText(getApplicationContext(), "Refreshing...", Toast.LENGTH_SHORT).show();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }


    private void fetchModels(){
        //Fetch JSON
        ConnectivityManager connectivityManager = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            new LoadModelsAsyncTask().execute(RETRIEVE_URL);
        } else {
            Log.v(TAG, "No network connection available.");
            Toast.makeText(getApplicationContext(), "No connection", Toast.LENGTH_SHORT).show();
        }
    }

    /*
     * ASYNC TASKS (LOADMODELS, LOADIMAGES AND DOWNLAODSOUNDFILES).
     */

    private class LoadModelsAsyncTask extends AsyncTask<String, Void, JSONArray> {
        @Override
        protected JSONArray doInBackground(String... urls) {
            return new JsonParser().getJSONFromUrl(urls[0]);
        }

        // onPostExecute adds a new YoCatchModel using data from JSON
        @Override
        protected void onPostExecute(JSONArray result) {
            JSONArray jsonArray = result;
            Log.v(TAG, "Fetched data " + result);
            yoCatchModels.clear();
            for(int i=0; i<jsonArray.length(); i++){
                try {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    YoCatchModel yoCatchModel = new YoCatchModel(jsonObject.getString("yoDestination"), jsonObject.getString("yoMessage"));

                    yoCatchModel.setImageUrl(jsonObject.getString("imageURL"));
                    yoCatchModel.setAudioUrl(jsonObject.getString("audiofileURL"));
                    yoCatchModel.setUsername(jsonObject.getString("username"));

                    yoCatchModels.add(yoCatchModel);
                    new LoadImageAsyncTask().execute(yoCatchModel);
                    adapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private class LoadImageAsyncTask extends AsyncTask<YoCatchModel, String, Bitmap> {
        private Bitmap bitmap;
        private YoCatchModel model;

        protected Bitmap doInBackground(YoCatchModel... args) {
            model = args[0];
            try {
                bitmap = BitmapFactory.decodeStream((InputStream)new URL(model.getImageUrl()).getContent());
            } catch (Exception e) {
                //e.printStackTrace();
                Log.e(TAG, "No image found");
            }
            return bitmap;
        }
        protected void onPostExecute(Bitmap image) {

            if(image != null){
                Bitmap resized = ThumbnailUtils.extractThumbnail(image, 40, 40);
                model.setImage(resized);
            }
            adapter.notifyDataSetChanged();
        }
    }


    // Async Task Class
    private class DownloadAudioAsyncTask extends AsyncTask<String, String, String> {

        private String fileName;

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            Toast.makeText(getApplicationContext(), "File doesn't exist, downloading", Toast.LENGTH_LONG).show();
        }

        // Download Music File from Internet
        @Override
        protected String doInBackground(String... f_url) {
            int count;
            try {
                URL url = new URL(f_url[0]);
                URLConnection urlConnection = url.openConnection();
                urlConnection.connect();
                InputStream input = new BufferedInputStream(url.openStream(),10*1024);
                // Output stream to write file in SD card
                fileName = f_url[0].split("/")[5];
                OutputStream output = new FileOutputStream(Environment.getExternalStorageDirectory().getPath() + "/" + fileName);
                byte data[] = new byte[1024];
                long total = 0;
                while ((count = input.read(data)) != -1) {
                    total += count;
                    // Write data to file
                    output.write(data, 0, count);
                }
                // Flush output
                output.flush();
                // Close streams
                output.close();
                input.close();
            } catch (Exception e) {
                Log.e("Error: ", e.getMessage());
            }
            return null;
        }

        // Once file is downloaded
        @Override
        protected void onPostExecute(String file_url) {
            Toast.makeText(getApplicationContext(), "Download complete", Toast.LENGTH_LONG).show();
            // Play the music
            playSound(DIRECTORY_PATH + fileName);
        }
    }

    private void playSound(String path){
        mediaPlayer = new MediaPlayer();
        try {

            mediaPlayer.setDataSource(path);
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

    //Custom adapter for the ListView
    public class CustomListAdapter extends BaseAdapter {
        private ArrayList<YoCatchModel> listData;

        private LayoutInflater layoutInflater;
        //private ViewHolder holder;
        private  ViewHolder holder;

        public CustomListAdapter(Context context, ArrayList listData) {
            this.listData = listData;
            this.layoutInflater = LayoutInflater.from(context);
        }

        @Override
        public int getCount() {
            return listData.size();
        }

        @Override
        public Object getItem(int position) {
            return listData.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }


        public View getView(int position, View convertView, ViewGroup parent) {
            //ViewHolder holder;
            if (convertView == null) {
                convertView = layoutInflater.inflate(R.layout.list_row_layout_yos, null);
                holder = new ViewHolder();
                holder.usernameView = (TextView) convertView.findViewById(R.id.username);
                holder.yoMessageView = (TextView) convertView.findViewById(R.id.yoMessage);;
                holder.imageView = (ImageView) convertView.findViewById(R.id.image);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }

            holder.usernameView.setText(listData.get(position).getDestination());
            holder.yoMessageView.setText(listData.get(position).getYoMessage());
            if(listData.get(position).getImage() != null){
                holder.imageView.setImageBitmap(listData.get(position).getImage());
            }else{
                holder.imageView.setImageResource(R.drawable.photo_placeholder);
            }
            return convertView;
        }

        class ViewHolder {
            TextView usernameView;
            TextView yoMessageView;
            ImageView imageView;
        }
    }
}
