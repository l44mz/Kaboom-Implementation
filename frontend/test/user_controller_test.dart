import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';
import 'package:kaboom_app/core/network/api_client.dart';
import 'package:kaboom_app/core/storage/token_storage.dart';
import 'package:kaboom_app/shared/controllers/user_controller.dart';
import 'package:kaboom_app/shared/models/user.dart';
import 'package:kaboom_app/shared/services/user_service.dart';
import 'package:kaboom_app/shared/widgets/app_drawer.dart';

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
  // GetX keeps registered controllers in memory between tests: clear them after each one.
  tearDown(Get.reset);

  test('load fills the user', () async {
    final controller = UserController(service: FakeUserService(120));

    await controller.load();

    expect(controller.user.value?.coins, 120);
  });

  test('reload shows the new balance after a session', () async {
    final service = FakeUserService(120);
    final controller = UserController(service: service);
    await controller.load();

    service.coins = 145;
    await controller.reload();

    expect(controller.user.value?.coins, 145);
  });

  test('reload keeps the old balance when the request fails', () async {
    final service = FakeUserService(120);
    final controller = UserController(service: service);
    await controller.load();

    service.fail = true;
    await controller.reload();

    expect(controller.user.value?.coins, 120);
  });

  test('clear forgets the user on log out', () async {
    final controller = UserController(service: FakeUserService(120));
    await controller.load();

    controller.clear();

    expect(controller.user.value, isNull);
  });

  testWidgets('the side menu shows the shared balance and updates it', (tester) async {
    final service = FakeUserService(120);
    final controller = Get.put(UserController(service: service));
    await controller.load();
    final scaffold = GlobalKey<ScaffoldState>();

    await tester.pumpWidget(
      MaterialApp(home: Scaffold(key: scaffold, drawer: const AppDrawer())),
    );
    scaffold.currentState!.openDrawer();
    await tester.pumpAndSettle();
    expect(find.text('120 coins'), findsOneWidget);

    service.coins = 145;
    await controller.reload();
    await tester.pump();
    expect(find.text('145 coins'), findsOneWidget);
  });

  testWidgets('the side menu still works without a UserController', (tester) async {
    final scaffold = GlobalKey<ScaffoldState>();
    await tester.pumpWidget(
      MaterialApp(home: Scaffold(key: scaffold, drawer: const AppDrawer())),
    );
    scaffold.currentState!.openDrawer();
    await tester.pumpAndSettle();

    expect(find.text('-- coins'), findsOneWidget);
  });
}