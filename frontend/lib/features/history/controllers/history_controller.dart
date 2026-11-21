import 'package:dio/dio.dart';
import 'package:get/get.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_error.dart';
import '../../../core/storage/token_storage.dart';
import '../models/history.dart';
import '../services/history_service.dart';


class HistoryController extends GetxController {
  HistoryController({ApiClient? api})
      : _service = HistoryService(api ?? ApiClient(TokenStorage()));

  final HistoryService _service;


  static const previewCount = 4;



  final range = HistoryRange.week.obs; // Rx<HistoryRange>
  final data = Rxn<HistoryData>(); // null until the first load
  final isLoading = false.obs; // RxBool
  final errorMessage = RxnString(); // null = no error
  final showAll = false.obs;


  @override
  void onInit() {
    super.onInit();
    load();
  }

  

  /// The sessions to list: the first few, or all of them after "See all".
  List<PastSession> get visibleSessions {
    final sessions = data.value?.sessions ?? const <PastSession>[];
    return showAll.value ? sessions : sessions.take(previewCount).toList();
  }

  bool get hasMoreSessions => (data.value?.sessions.length ?? 0) > previewCount;

  

  /// Switches the tab (Day / Week / Month / All time) and loads it.
  Future<void> setRange(HistoryRange value) async {
    if (value == range.value && data.value != null) return;
    range.value = value;
    showAll.value = false;
    await load();
  }


  void toggleShowAll() => showAll.toggle();

  Future<void> load() async {
    isLoading.value = true; // the screen shows the spinner right away
    errorMessage.value = null;
    try {
      data.value = await _service.getHistory(range.value);
    } on DioException catch (e) {
      errorMessage.value = apiErrorMessage(e);
    } finally {
      isLoading.value = false;
    }
  }
}