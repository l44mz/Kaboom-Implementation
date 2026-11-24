import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';
import 'package:kaboom_app/features/auth/controllers/auth_controller.dart';

import 'helpers/fake_backend.dart';

void main() {
  tearDown(Get.reset);

  test('login returns the user and saves both tokens', () async {
    final tokens = FakeTokenStorage();
    final backend = FakeBackend()
      ..on('POST', '/auth/login', {
        'accessToken': 'access-1',
        'refreshToken': 'refresh-1',
        'expiresInSeconds': 900,
        'user': {'id': 'u-1', 'name': 'Shahd', 'email': 's@kaboom.app', 'coins': 50, 'role': 'user'},
      });
    final auth = AuthController(tokens: tokens, api: backend.client(tokens));

    final user = await auth.login(email: '  s@kaboom.app ', password: 'secret');

    expect(user?.name, 'Shahd');
    expect(tokens.access, 'access-1');
    expect(tokens.refresh, 'refresh-1');
    // The email is trimmed before it is sent.
    expect((backend.requests.single.body as Map)['email'], 's@kaboom.app');
    expect(auth.errorMessage.value, isNull);
    expect(auth.isLoading.value, isFalse);
  });

  test('a wrong password shows "Email or password is incorrect."', () async {
    final tokens = FakeTokenStorage();
    final backend = FakeBackend()..on('POST', '/auth/login', null, status: 401);
    final auth = AuthController(tokens: tokens, api: backend.client(tokens));

    final user = await auth.login(email: 's@kaboom.app', password: 'wrong');

    expect(user, isNull);
    expect(auth.errorMessage.value, 'Email or password is incorrect.');
    expect(tokens.access, isNull);
  });

  test('the server being off shows a connection message', () async {
    final tokens = FakeTokenStorage();
    final auth = AuthController(tokens: tokens, api: FakeBackend().client(tokens));
    // No route set up for /auth/login: the fake answers 404.

    final user = await auth.login(email: 's@kaboom.app', password: 'secret');

    expect(user, isNull);
    expect(auth.errorMessage.value, isNotNull);
  });

  test('signing up with a used email says it is already registered', () async {
    final tokens = FakeTokenStorage();
    final backend = FakeBackend()..on('POST', '/auth/register', null, status: 409);
    final auth = AuthController(tokens: tokens, api: backend.client(tokens));

    final ok = await auth.signUp(name: 'Shahd', email: 's@kaboom.app', password: 'Str0ng!Password');

    expect(ok, isFalse);
    expect(auth.errorMessage.value, 'This email is already registered.');
  });

  test('sign up works with a new email', () async {
    final tokens = FakeTokenStorage();
    final backend = FakeBackend()..on('POST', '/auth/register', null, status: 201);
    final auth = AuthController(tokens: tokens, api: backend.client(tokens));

    final ok = await auth.signUp(name: ' Shahd ', email: 's@kaboom.app', password: 'Str0ng!Password');

    expect(ok, isTrue);
    expect((backend.requests.single.body as Map)['name'], 'Shahd');
  });

  test('the login form needs an email and a password', () {
    expect(AuthController.validateEmail(''), isNotNull);
    expect(AuthController.validateEmail('not-an-email'), isNotNull);
    expect(AuthController.validateEmail('s@kaboom.app'), isNull);

    expect(AuthController.validateLoginPassword(''), isNotNull);
    expect(AuthController.validateLoginPassword('anything'), isNull);
  });
}
