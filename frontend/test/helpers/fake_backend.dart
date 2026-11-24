import 'dart:convert';
import 'dart:typed_data';

import 'package:dio/dio.dart';
import 'package:kaboom_app/core/network/api_client.dart';
import 'package:kaboom_app/core/storage/token_storage.dart';

/// Keeps tokens in memory instead of the phone's secure storage
/// (secure storage does not exist in tests).
class FakeTokenStorage extends TokenStorage {
  String? access;
  String? refresh;

  @override
  Future<void> save({required String access, required String refresh}) async {
    this.access = access;
    this.refresh = refresh;
  }

  @override
  Future<void> saveAccess(String access) async => this.access = access;

  @override
  Future<String?> get accessToken async => access;

  @override
  Future<String?> get refreshToken async => refresh;

  @override
  Future<void> clear() async {
    access = null;
    refresh = null;
  }
}

/// One request the app sent to the fake backend.
class SentRequest {
  SentRequest(this.method, this.path, this.query, this.body);

  final String method;
  final String path;
  final Map<String, dynamic> query;
  final Object? body;
}

/// A pretend Kaboom backend. Tests decide the answer for each route:
///
///   backend.on('GET', '/products', [ {...} ]);
///   backend.on('POST', '/purchases', {'message': 'Not enough coins'}, status: 400);
///
/// Routes nobody set up answer 404. Every request is kept in [requests].
class FakeBackend implements HttpClientAdapter {
  final _routes = <String, (int, Object?)>{};
  final requests = <SentRequest>[];

  void on(String method, String path, Object? body, {int status = 200}) {
    _routes['$method $path'] = (status, body);
  }

  /// Makes an [ApiClient] that talks to this fake instead of the real server.
  ApiClient client([TokenStorage? tokens]) {
    final api = ApiClient(tokens ?? FakeTokenStorage());
    api.dio.httpClientAdapter = this;
    return api;
  }

  /// The requests sent to [path], for example to check what was posted.
  List<SentRequest> sentTo(String method, String path) =>
      requests.where((r) => r.method == method && r.path == path).toList();

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    final method = options.method.toUpperCase();
    requests.add(SentRequest(
      method,
      options.path,
      Map.of(options.queryParameters),
      options.data,
    ));
    final (status, body) = _routes['$method ${options.path}'] ?? (404, null);
    return ResponseBody.fromString(
      body == null ? '' : jsonEncode(body),
      status,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }

  @override
  void close({bool force = false}) {}
}
