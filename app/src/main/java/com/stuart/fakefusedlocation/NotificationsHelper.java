package com.stuart.fakefusedlocation;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

public class NotificationsHelper {
    private static final String CHANNEL_ID_FOREGROUND = "foreground";

    public static void createNotificationChannels(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;

        NotificationChannel channel =
                new NotificationChannel(CHANNEL_ID_FOREGROUND,
                        context.getString(R.string.notification_foreground_channel_name),
                        NotificationManager.IMPORTANCE_LOW);

        getNotificationManager(context).createNotificationChannel(channel);
    }

    public static Notification buildForegroundNotification(Context context) {
        Intent intent = new Intent(context, MapActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        return new NotificationCompat.Builder(context, CHANNEL_ID_FOREGROUND)
                .setContentTitle(context.getString(R.string.notification_foreground_title))
                .setSmallIcon(R.drawable.ic_notification_arrow)
                .setContentIntent(PendingIntent.getActivity(context, 0, intent, 0))
                .build();
    }

    @NonNull
    @SuppressWarnings("ConstantConditions")
    private static NotificationManager getNotificationManager(@NonNull Context context) {
        return (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    private NotificationsHelper() {
        // No instance
    }
}
