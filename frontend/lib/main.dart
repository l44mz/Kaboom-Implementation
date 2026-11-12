import 'package:flutter/material.dart';

import 'app.dart';
import 'core/notifications/session_notifications.dart';

Future<void> main() async {
  // Needed before using a plugin (the notifications) before runApp.
  WidgetsFlutterBinding.ensureInitialized();
  await SessionNotifications.instance.init();
  runApp(const KaboomApp());
}
