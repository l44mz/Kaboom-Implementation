import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart' hide Response;
import 'package:kaboom_app/core/network/api_client.dart';
import 'package:kaboom_app/core/storage/token_storage.dart';
import 'package:kaboom_app/features/account/models/robot_info.dart';
import 'package:kaboom_app/features/robot/controllers/pairing_controller.dart';
import 'package:kaboom_app/features/robot/screens/robot_pairing_screen.dart';
import 'package:kaboom_app/features/robot/services/robot_service.dart';

/// Accepts only 482913, like a robot showing that code. Anything else is "not found".
class FakeRobotService extends RobotService {
  FakeRobotService() : super(ApiClient(TokenStorage()));

  final codes = <String>[];

  @override
  Future<RobotInfo> pair(String code) async {
    codes.add(code);
    if (code == '482913') {
      return const RobotInfo(id: 'r-1', name: 'FocusRobot - ABCD', connected: false);
    }
    final request = RequestOptions(path: '/robots/pair');
    throw DioException(
      requestOptions: request,
      response: Response(requestOptions: request, statusCode: 404),
    );
  }
}

Future<void> settle(WidgetTester tester) async {
  // The Lottie animations repeat forever, so pumpAndSettle would never finish.
  await tester.pump();
  await tester.pump(const Duration(milliseconds: 300));
}

void main() {
  tearDown(Get.reset);

  test('the code must be 6 digits', () {
    expect(PairingController.validateCode(''), isNotNull);
    expect(PairingController.validateCode('12345'), isNotNull);
    expect(PairingController.validateCode('12a456'), isNotNull);
    expect(PairingController.validateCode('482913'), isNull);
  });

  testWidgets('a wrong code shows an error and empties the boxes', (tester) async {
    final service = FakeRobotService();
    await tester.pumpWidget(MaterialApp(home: RobotPairingScreen(service: service)));
    await settle(tester);

    await tester.enterText(find.byKey(const Key('pairing-code-field')), '111111');
    await settle(tester);

    expect(service.codes, ['111111']);
    expect(find.textContaining('wrong or expired'), findsOneWidget);
    expect(find.text('Sparky is yours!'), findsNothing);
  });

  testWidgets('the right code pairs the robot', (tester) async {
    final service = FakeRobotService();
    await tester.pumpWidget(MaterialApp(home: RobotPairingScreen(service: service)));
    await settle(tester);

    // Typing the 6th digit sends the code by itself.
    await tester.enterText(find.byKey(const Key('pairing-code-field')), '482913');
    await settle(tester);

    expect(service.codes, ['482913']);
    expect(find.text('Sparky is yours!'), findsOneWidget);
    expect(find.text('FocusRobot - ABCD'), findsOneWidget);
  });
}