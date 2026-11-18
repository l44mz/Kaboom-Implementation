import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kaboom_app/features/session/services/session_service.dart';

DioException _errorWith(int status, [Object? data]) {
  final options = RequestOptions(path: '/focus-sessions/start');
  return DioException(
    requestOptions: options,
    response: Response(requestOptions: options, statusCode: status, data: data),
  );
}

void main() {
  test('409 says a session is already running', () {
    final error = _errorWith(409, {
      'message': 'User already has an active or paused session',
    });
    expect(
      startSessionErrorMessage(error),
      'You already have a session running.',
    );
  });

  test('other errors use the shared API message', () {
    expect(
      startSessionErrorMessage(_errorWith(401)),
      'Your session has ended. Please log in again.',
    );
  });
}
