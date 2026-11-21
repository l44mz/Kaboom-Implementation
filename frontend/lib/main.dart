import 'package:flutter/material.dart';
import 'package:get/get.dart';

import 'app.dart';
import 'core/notifications/session_notifications.dart';
import 'shared/controllers/user_controller.dart';

Future<void> main() async {
  // Needed before using a plugin (the notifications) before runApp.
  WidgetsFlutterBinding.ensureInitialized();
  await SessionNotifications.instance.init();

  Get.put(UserController(), permanent: true);// Make the user controller available to the app.
  
  runApp(const KaboomApp());
}
