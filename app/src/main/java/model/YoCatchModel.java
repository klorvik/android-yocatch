package model;

import android.graphics.Bitmap;
import android.media.MediaPlayer;

import java.io.Serializable;

/**
 * Created by lorre on 10/08/14.
 */
public class YoCatchModel extends Object implements Serializable{
    private String username;
    private String destination;
    private String yoMessage;
    private String imageUrl;
    private String audioUrl;
    private Bitmap image;

    public YoCatchModel(String destination, String yoMessage){
        this.destination = destination;
        this.yoMessage = yoMessage;
    }

    //Getters/setters

    public String getYoMessage() {
        return yoMessage;
    }

    public void setYoMessage(String yoMessage) {
        this.yoMessage = yoMessage;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getAudioUrl() {
        return audioUrl;
    }

    public void setAudioUrl(String audioUrl) {
        this.audioUrl = audioUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Bitmap getImage() {
        return image;
    }

    public void setImage(Bitmap image) {
        this.image = image;
    }

    public static String defaultYoMessage(){
        return "Yo";
    }

    public String toString(){
        return "Username: " + username + " Yomessage: " + yoMessage;
    }


}
