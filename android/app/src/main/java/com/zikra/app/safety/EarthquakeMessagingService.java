package com.zikra.app.safety;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import java.time.Instant;

public final class EarthquakeMessagingService extends FirebaseMessagingService {
  @Override
  public void onMessageReceived(RemoteMessage message) {
    EarthquakeAlerts.receive(this, message.getData(), Instant.now());
  }

  @Override
  public void onNewToken(String token) {
    // The SDK manages topic membership. Never print or send this token to the AI backend.
    EarthquakeAlerts.sync(getApplicationContext());
  }
}
