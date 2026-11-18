import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_error.dart';
import '../../../core/storage/token_storage.dart';
import '../models/history.dart';
import '../services/history_service.dart';

/// Logic and state for the History screen.
class HistoryController extends ChangeNotifier {
  HistoryController({ApiClient? api})
      : _service = HistoryService(api ?? ApiClient(TokenStorage()));

  final HistoryService _service;

  /// How many sessions show before "See all" is tapped.
  static const previewCount = 4;

  HistoryRange range = HistoryRange.week;
  HistoryData? data;
  bool isLoading = false;
  String? errorMessage;
  bool showAll = false;

  /// The sessions to list: the first few, or all of them after "See all".
  List<PastSession> get visibleSessions {
    final sessions = data?.sessions ?? const [];
    return showAll ? sessions : sessions.take(previewCount).toList();
  }

  bool get hasMoreSessions => (data?.sessions.length ?? 0) > previewCount;

  /// Switches the tab (Day / Week / Month / All time) and loads it.
  Future<void> setRange(HistoryRange value) async {
    if (value == range && data != null) return;
    range = value;
    showAll = false;
    await load();
  }

  void toggleShowAll() {
    showAll = !showAll;
    notifyListeners();
  }

  Future<void> load() async {
    isLoading = true;
    errorMessage = null;
    notifyListeners();
    try {
      data = await _service.getHistory(range);
    } on DioException catch (e) {
      errorMessage = apiErrorMessage(e);
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }
}
