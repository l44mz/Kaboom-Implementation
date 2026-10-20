import 'package:dio/dio.dart';

/// Turns any error from an API call into a short message for the user.
String apiErrorMessage(Object error) {
  if (error is! DioException) {
    return 'Something went wrong. Please try again.';
  }

  final response = error.response;
  if (response == null) {
    return "Can't reach the server. Check your connection.";
  }

  final data = response.data;
  switch (response.statusCode) {
    case 400:
      if (data is Map) {
        final fieldErrors = data['fieldErrors'];
        if (fieldErrors is List && fieldErrors.isNotEmpty) {
          final first = fieldErrors.first;
          if (first is Map && first['message'] != null) {
            return first['message'].toString();
          }
        }
        if (data['message'] != null) return data['message'].toString();
      }
      return 'Invalid input.';
    case 401:
      return 'Your session has ended. Please log in again.';
    case 403:
      return "You don't have permission to do that.";
    case 404:
      return 'Not found.';
    case 409:
      if (data is Map && data['message'] != null) {
        return data['message'].toString();
      }
      return 'This already exists.';
    default:
      return 'Something went wrong. Please try again.';
  }
}
