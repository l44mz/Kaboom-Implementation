import 'package:flutter/material.dart';
import 'package:get/get.dart';

import 'app.dart';
import 'core/notifications/session_notifications.dart';
import 'shared/controllers/focus_mode_controller.dart';
import 'shared/controllers/outfit_controller.dart';
import 'shared/controllers/user_controller.dart';

Future<void> main() async {
  // Needed before using a plugin (the notifications) before runApp.
  WidgetsFlutterBinding.ensureInitialized();
  await SessionNotifications.instance.init();

  // Shared by every screen, so registered once and never deleted (permanent).
  Get.put(UserController(), permanent: true); // the user and the coins
  Get.put(OutfitController(), permanent: true); // what Sparky wears (Home + Store)
  // Robot or phone camera. load() reads the saved choice; not awaited, it is quick.
  Get.put(FocusModeController(), permanent: true).load();

  runApp(const KaboomApp());
}