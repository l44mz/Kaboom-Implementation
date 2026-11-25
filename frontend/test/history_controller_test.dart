import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';
import 'package:kaboom_app/features/history/controllers/history_controller.dart';
import 'package:kaboom_app/features/history/models/history.dart';

import 'helpers/fake_backend.dart';

/// The backend's answer for a range with [count] past sessions.
Map<String, dynamic> history(int count) => {
      'totalFocusSeconds': count * 1500,
      'sessionCount': count,
      'avgFocusPercentage': 82.5,
      'bars': [
        {'label': 'Mon', 'focusMinutes': 25},
        {'label': 'Tue', 'focusMinutes': 50, 'current': true},
      ],
      'sessions': [
        for (var i = 0; i < count; i++)
          {
            'id': 's-$i',
            'startTime': '2026-11-2${i % 9}T10:00:00Z',
            'activeSeconds': 1500,
            'focusedPercentage': 80,
            'distractions': i,
            'rewardEarned': 25,
          },
      ],
    };

void main() {
  tearDown(Get.reset);

  test('load shows the totals and the sessions', () async {
    final backend = FakeBackend()..on('GET', '/focus-sessions/history', history(3));
    final controller = HistoryController(api: backend.client());

    await controller.load();

    expect(controller.data.value?.sessionCount, 3);
    expect(controller.data.value?.bars, hasLength(2));
    expect(controller.visibleSessions, hasLength(3));
    expect(controller.errorMessage.value, isNull);
    // The Week tab is the default.
    expect(backend.requests.single.query, {'range': 'week'});
  });

  test('only 4 sessions show until "See all" is tapped', () async {
    final backend = FakeBackend()..on('GET', '/focus-sessions/history', history(6));
    final controller = HistoryController(api: backend.client());
    await controller.load();

    expect(controller.visibleSessions, hasLength(HistoryController.previewCount));
    expect(controller.hasMoreSessions, isTrue);

    controller.toggleShowAll();

    expect(controller.visibleSessions, hasLength(6));
  });

  test('changing the tab loads that range and closes "See all"', () async {
    final backend = FakeBackend()..on('GET', '/focus-sessions/history', history(6));
    final controller = HistoryController(api: backend.client());
    await controller.load();
    controller.toggleShowAll();

    await controller.setRange(HistoryRange.month);

    expect(backend.requests.last.query, {'range': 'month'});
    expect(controller.range.value, HistoryRange.month);
    expect(controller.showAll.value, isFalse);
  });

  test('tapping the tab that is already open does not reload', () async {
    final backend = FakeBackend()..on('GET', '/focus-sessions/history', history(1));
    final controller = HistoryController(api: backend.client());
    await controller.load();

    await controller.setRange(HistoryRange.week);

    expect(backend.requests, hasLength(1));
  });

  test('no sessions yet: an empty list, not an error', () async {
    final backend = FakeBackend()..on('GET', '/focus-sessions/history', history(0));
    final controller = HistoryController(api: backend.client());

    await controller.load();

    expect(controller.visibleSessions, isEmpty);
    expect(controller.hasMoreSessions, isFalse);
    expect(controller.errorMessage.value, isNull);
  });

  test('a server error shows a message and stops the spinner', () async {
    final backend = FakeBackend()..on('GET', '/focus-sessions/history', null, status: 500);
    final controller = HistoryController(api: backend.client());

    await controller.load();

    expect(controller.errorMessage.value, isNotNull);
    expect(controller.isLoading.value, isFalse);
  });
}
