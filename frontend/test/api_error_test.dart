import 'package:dio/dio.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:kaboom_app/core/network/api_error.dart';

DioException _errorWith(int status, [Object? data]) {
  final options = RequestOptions(path: '/test');
  return DioException(
    requestOptions: options,
    response: Response(requestOptions: options, statusCode: status, data: data),
  );
}

void main() {
  test('no response means the server is unreachable', () {
    final error = DioException(requestOptions: RequestOptions(path: '/test'));
    expect(
      apiErrorMessage(error),
      "Can't reach the server. Check your connection.",
    );
  });

  test('400 shows the first field error from the API', () {
    final error = _errorWith(400, {
      'fieldErrors': [
        {'field': 'email', 'message': 'must be a valid email'},
      ],
    });
    expect(apiErrorMessage(error), 'must be a valid email');
  });

  test('401 asks the user to log in again', () {
    expect(
      apiErrorMessage(_errorWith(401)),
      'Your session has ended. Please log in again.',
    );
  });
}
