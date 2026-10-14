import 'package:go_router/go_router.dart';

import '../../features/admin/add_product_screen.dart';
import '../../features/admin/admin_dashboard_screen.dart';
import '../../features/auth/login_screen.dart';
import '../../features/auth/signup_screen.dart';
import '../../features/history/history_screen.dart';
import '../../features/home/home_screen.dart';
import '../../features/robot/robot_pairing_screen.dart';
import '../../features/session/focus_session_screen.dart';
import '../../features/session/session_complete_screen.dart';
import '../../features/shop/shop_screen.dart';

class AppRoutes {
  static const login = '/login';
  static const signUp = '/signup';
  static const home = '/home';
  static const session = '/session';
  static const sessionComplete = '/session/complete';
  static const shop = '/shop';
  static const robotPair = '/robot/pair';
  static const history = '/history';
  static const admin = '/admin';
  static const addProduct = '/admin/products/new';
}

final appRouter = GoRouter(
  initialLocation: AppRoutes.signUp,
  routes: [
    GoRoute(path: AppRoutes.login, builder: (context, state) => const LoginScreen()),
    GoRoute(path: AppRoutes.signUp, builder: (context, state) => const SignUpScreen()),
    GoRoute(path: AppRoutes.home, builder: (context, state) => const HomeScreen()),
    GoRoute(path: AppRoutes.session, builder: (context, state) => const FocusSessionScreen()),
    GoRoute(path: AppRoutes.sessionComplete, builder: (context, state) => const SessionCompleteScreen()),
    GoRoute(path: AppRoutes.shop, builder: (context, state) => const ShopScreen()),
    GoRoute(path: AppRoutes.robotPair, builder: (context, state) => const RobotPairingScreen()),
    GoRoute(path: AppRoutes.history, builder: (context, state) => const HistoryScreen()),
    GoRoute(path: AppRoutes.admin, builder: (context, state) => const AdminDashboardScreen()),
    GoRoute(path: AppRoutes.addProduct, builder: (context, state) => const AddProductScreen()),
  ],
);