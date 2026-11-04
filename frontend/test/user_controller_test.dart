import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kaboom_app/core/network/api_client.dart';
import 'package:kaboom_app/core/storage/token_storage.dart';
import 'package:kaboom_app/shared/controllers/user_controller.dart';
import 'package:kaboom_app/shared/models/user.dart';
import 'package:kaboom_app/shared/services/user_service.dart';
import 'package:kaboom_app/shared/widgets/app_drawer.dart';
import 'package:provider/provider.dart';

/// Returns the next coin balance each time, or throws when [fail] is true.
class FakeUserService extends UserService {
  FakeUserService(this.coins) : super(ApiClient(TokenStorage()));

  int coins;
  bool fail = false;

  @override
  Future<User> getMe() async {
    if (fail) throw Exception('offline');
    return User(id: '1', name: 'Shahd', email: 's@kaboom.app', coins: coins, role: 'user');
  }
}

void main() {
  test('load fills the user and tells listeners', () async {
    final controller = UserController(service: FakeUserService(120));
    var notified = 0;
    controller.addListener(() => notified++);

    await controller.load();

    expect(controller.user?.coins, 120);
    expect(notified, 1);
  });

  test('refresh shows the new balance after a session', () async {
    final service = FakeUserService(120);
    final controller = UserController(service: service);
    await controller.load();

    service.coins = 145;
    await controller.refresh();

    expect(controller.user?.coins, 145);
  });

  test('refresh keeps the old balance when the request fails', () async {
    final service = FakeUserService(120);
    final controller = UserController(service: service);
    await controller.load();

    service.fail = true;
    await controller.refresh();

    expect(controller.user?.coins, 120);
  });

  test('clear forgets the user on log out', () async {
    final controller = UserController(service: FakeUserService(120));
    await controller.load();

    controller.clear();

    expect(controller.user, isNull);
  });

  testWidgets('the side menu shows the shared balance and updates it', (tester) async {
    final service = FakeUserService(120);
    final controller = UserController(service: service);
    await controller.load();
    final scaffold = GlobalKey<ScaffoldState>();

    await tester.pumpWidget(
      ChangeNotifierProvider.value(
        value: controller,
        child: MaterialApp(home: Scaffold(key: scaffold, drawer: const AppDrawer())),
      ),
    );
    scaffold.currentState!.openDrawer();
    await tester.pumpAndSettle();
    expect(find.text('120 coins'), findsOneWidget);

    service.coins = 145;
    await controller.refresh();
    await tester.pump();
    expect(find.text('145 coins'), findsOneWidget);
  });

  testWidgets('the side menu still works without the provider', (tester) async {
    final scaffold = GlobalKey<ScaffoldState>();
    await tester.pumpWidget(
      MaterialApp(home: Scaffold(key: scaffold, drawer: const AppDrawer())),
    );
    scaffold.currentState!.openDrawer();
    await tester.pumpAndSettle();

    expect(find.text('-- coins'), findsOneWidget);
  });
}
