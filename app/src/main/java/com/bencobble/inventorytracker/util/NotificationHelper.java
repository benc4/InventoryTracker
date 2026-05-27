package com.bencobble.inventorytracker.util;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.bencobble.inventorytracker.R;

// NotificationHelper utility class
// Handles creating and sending notifications for low stock alerts
// when an item's quantity is changed to 0
public class NotificationHelper {
    // Notification channel ID and channel name
    private static final String CHANNEL_ID = "low_stock_channel";
    private static final String CHANNEL_NAME = "Low Stock Alerts";

    // Incrementing counter for notification IDs
    private static int notificationId = 0;

    // createNotificationChannel method
    // Creates a notification channel for low stock alerts if the device is running Android 8+
    // Version check is needed because NotificationChannel does not exist on older versions
    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) { // 0 is Oreo or Android 8
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT);
            channel.setDescription("Alerts when inventory items reach zero stock");

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    // sendLowStockNotification method
    // Takes a context and item name as parameters
    // Builds a notification using the item name and then deploys it
    public static void sendLowStockNotification(Context context, String itemName) {
        createNotificationChannel(context); // Ensures notification channel exists

        // Builds the notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Low Stock Alert")
                .setContentText(itemName + " is out of stock")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT) // Uses normal sound/vibration alert
                .setAutoCancel(true); // Dismisses the notification when it is tapped

        // Gets the notification manager and deploys the notification
        // Increments notificationId for the next notification
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        manager.notify(notificationId++, builder.build());
    }
}
