import 'package:go_router/go_router.dart';

import '../../features/account/screens/account_screen.dart';
import '../../features/admin/models/product.dart';
import '../../features/admin/screens/add_product_screen.dart';
import '../../features/admin/screens/admin_dashboard_screen.dart';
import '../../features/auth/screens/login_screen.dart';
import '../../features/auth/screens/signup_screen.dart';
import '../../features/camera/screens/camera_test_screen.dart';
import '../../features/focus_mode/screens/focus_mode_screen.dart';
import '../../features/history/screens/history_screen.dart';
import '../../features/home/screens/home_screen.dart';
import '../../features/robot/screens/robot_pairing_screen.dart';
import '../../features/session/models/focus_session.dart';
import '../../features/session/models/session_result.dart';
import '../../features/session/screens/focus_session_screen.dart';
import '../../features/session/screens/session_complete_screen.dart';
import '../../features/shop/screens/shop_screen.dart';
import '../../features/splash/screens/splash_screen.dart';

class AppRoutes {
  static const splash = '/splash';
  static const login = '/login';
  static const signUp = '/signup';
  static const home = '/home';
  static const session = '/session';
  static const sessionComplete = '/session/complete';
  static const shop = '/shop';
  static const robotPair = '/robot/pair';
  static const focusMode = '/focus-mode';
  static const cameraTest = '/focus-mode/camera-test';
  static const history = '/history';
  static const admin = '/admin';
  static const addProduct = '/admin/products/new';
  static const account = '/account';
}

final appRouter = GoRouter(
  // The app opens on the splash screen; it moves on to Sign Up by itself.
  initialLocation: AppRoutes.splash,
  routes: [
    GoRoute(path: AppRoutes.splash, builder: (context, state) => const SplashScreen()),
    GoRoute(path: AppRoutes.login, builder: (context, state) => const LoginScreen()),
    GoRoute(path: AppRoutes.signUp, builder: (context, state) => const SignUpScreen()),
    GoRoute(path: AppRoutes.home, builder: (context, state) => const HomeScreen()),
    GoRoute(
      path: AppRoutes.session,
      builder: (context, state) =>
          FocusSessionScreen(session: state.extra as FocusSession?),
    ),
    GoRoute(
      path: AppRoutes.sessionComplete,
      builder: (context, state) =>
          SessionCompleteScreen(result: state.extra as SessionResult?),
    ),
    GoRoute(path: AppRoutes.shop, builder: (context, state) => const ShopScreen()),
    GoRoute(path: AppRoutes.robotPair, builder: (context, state) => const RobotPairingScreen()),
    GoRoute(path: AppRoutes.focusMode, builder: (context, state) => const FocusModeScreen()),
    GoRoute(path: AppRoutes.cameraTest, builder: (context, state) => const CameraTestScreen()),
    GoRoute(path: AppRoutes.history, builder: (context, state) => const HistoryScreen()),
    GoRoute(path: AppRoutes.account, builder: (context, state) => const AccountScreen()),
    GoRoute(path: AppRoutes.admin, builder: (context, state) => const AdminDashboardScreen()),
    GoRoute(
      path: AppRoutes.addProduct,
      // extra = the product to edit, or null to add a new one
      builder: (context, state) => AddProductScreen(product: state.extra as Product?),
    ),
  ],
);