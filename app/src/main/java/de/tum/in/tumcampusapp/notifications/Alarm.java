package de.tum.in.tumcampusapp.notifications;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.NotificationCompat;
import android.util.Base64;

import com.google.gson.Gson;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

import de.tum.in.tumcampusapp.R;
import de.tum.in.tumcampusapp.activities.AlarmActivity;
import de.tum.in.tumcampusapp.auxiliary.Utils;
import de.tum.in.tumcampusapp.models.GCMAlert;
import de.tum.in.tumcampusapp.models.GCMNotification;
import de.tum.in.tumcampusapp.models.TUMCabeClient;

public class Alarm extends GenericNotification {

    /**
     * This is the private key used to sign all messages sent by the alarm system - used to verify that the sent message is correct
     */
    private static final String pubKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAvSukueIrdowjJB/IHR6+tsCbYLF9kmC/2Sa8/kI9Ttq0aUyC0hDt2SBzuDDmp/RwnUap5/0xT/h3z+WgKOjrzWig4lmb7G2+RuuVn8466AErfp3YQVFiovNLGMqwfJzPZ9aV3sZBXCTeEbDkd/CLRp3kBYkAtL8NfIlbNaII9CWKdhS907JyEWRZO2DLiYLm37vK/hwg58eXHwL9jNYY3gFqGUlfWXwGC2a0yTOk9rgJejhUbU9GLWSL3OwiHVXlpPsvTC1Ry0H4kQQeisjCgpkPjOQAnAFRN9zZLtBZlIsssYvL3ohY/C1HfGzDwGTaELjhtzY9qHdFW/4GDZh8swIDAQAB";

    public final GCMAlert alert;
    private GCMNotification info;

    public Alarm(String payload, Context context, int notification) {
        super(context, 3, notification, true); //Let the base class know which id this notification has

        //Check if a payload was passed
        if (payload == null) {
            throw new NullPointerException();
        }

        //Get data from server
        try {
            this.info = TUMCabeClient.getInstance(this.context).getNotification(this.notification);
        } catch (IOException e) {
            Utils.log(e, "Error while fetching alarm data");
        }

        // parse data
        this.alert = (new Gson()).fromJson(payload, GCMAlert.class);
    }

    /**
     * Validates the signature sent to us
     *
     * @param title       the message title
     * @param description the message description
     * @param signature   the message signature
     * @return if the signature is valid
     */
    private static boolean isValidSignature(String title, String description, String signature) {
        String text = title + description;
        PublicKey key = getCabePublicKey();
        if (key == null) {
            return false;
        }

        Signature sig;
        try {
            sig = Signature.getInstance("SHA1WithRSA");
        } catch (NoSuchAlgorithmException e) {
            Utils.log(e);
            return false;
        }

        try {
            sig.initVerify(key);
        } catch (InvalidKeyException e) {
            Utils.log(e);
            return false;
        }

        byte[] textBytes;
        try {
            textBytes = text.getBytes("UTF8");
        } catch (UnsupportedEncodingException e) {
            Utils.log(e);
            return false;
        }
        try {
            sig.update(textBytes);
        } catch (SignatureException e) {
            Utils.log(e);
            return false;
        }
        try {
            return sig.verify(Base64.decode(signature, Base64.DEFAULT));
        } catch (SignatureException | IllegalArgumentException e) {
            Utils.log(e);
            return false;
        }
    }

    /**
     * converts the above base64 representation of the public key to a java object
     *
     * @return the public key of the TUMCabe server
     */
    private static PublicKey getCabePublicKey() {
        // Base64 string -> Bytes
        byte[] keyBytes = Base64.decode(pubKey, Base64.NO_WRAP);
        KeyFactory keyFactory;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        } catch (NoSuchAlgorithmException e) {
            Utils.log(e);
            return null;
        }

        // Bytes -> PublicKey
        try {
            return keyFactory.generatePublic(new X509EncodedKeySpec(keyBytes));
        } catch (InvalidKeySpecException e) {
            Utils.log(e);
            return null;
        }

    }

    @Override
    public Notification getNotification() {
        if (alert.silent || info == null) {
            //Do nothing
            return null;
        }

        if (!isValidSignature(info.getTitle(), info.getDescription(), info.getSignature())) {
            Utils.log("Received an invalid RSA signature");
            return null;
        }


        // GCMNotification sound
        Uri sound = Uri.parse("android.resource://" + context.getPackageName() + "/" + R.raw.message);
        Intent alarm = new Intent(this.context, AlarmActivity.class);
        alarm.putExtra("info", this.info);
        alarm.putExtra("alert", this.alert);
        PendingIntent pending = PendingIntent.getActivity(this.context, 0, alarm, PendingIntent.FLAG_UPDATE_CURRENT);
        String strippedDescription = Utils.stripHtml(info.getDescription()); // Strip any html tags from the description

        return new NotificationCompat.Builder(context)
                .setSmallIcon(this.icon)
                .setContentTitle(info.getTitle())
                .setStyle(new NotificationCompat.BigTextStyle().bigText(strippedDescription))
                .setContentText(strippedDescription)
                .setContentIntent(pending)
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setLights(0xff0000ff, 500, 500)
                .setSound(sound)
                .setAutoCancel(true)
                .build();
    }

    @Override
    public int getNotificationIdentification() {
        return 3;
    }
}
