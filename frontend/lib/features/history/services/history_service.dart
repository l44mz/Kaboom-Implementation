import '../../../core/network/api_client.dart';
import '../models/history.dart';

/// HTTP calls for the History screen. No UI and no state here.
class HistoryService {
  HistoryService(this._api);

  final ApiClient _api;

  /// GET /api/v1/focus-sessions/history?range=day|week|month|all
  Future<HistoryData> getHistory(HistoryRange range) async {
    final response = await _api.dio.get(
      '/focus-sessions/history',
      queryParameters: {'range': range.apiValue},
    );
    return HistoryData.fromJson(response.data as Map<String, dynamic>);
  }
}
