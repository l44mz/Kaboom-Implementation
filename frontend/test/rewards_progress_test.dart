import 'package:flutter_test/flutter_test.dart';
import 'package:kaboom_app/features/history/models/history.dart';
import 'package:kaboom_app/features/shop/models/shop_item.dart';
import 'package:kaboom_app/shared/widgets/mini_robot.dart';

void main() {
  group('HistoryFormat', () {
    test('duration shows hours and minutes', () {
      expect(HistoryFormat.duration(18000), '5h 00m');
      expect(HistoryFormat.duration(2700), '45m');
      expect(HistoryFormat.duration(0), '0m');
    });

    test('session title says Today, Yesterday, or the date', () {
      final now = DateTime(2026, 11, 3, 18);
      expect(HistoryFormat.sessionTitle(DateTime(2026, 11, 3, 16, 30), now: now),
          'Today · 4:30 PM');
      expect(HistoryFormat.sessionTitle(DateTime(2026, 11, 2, 20, 15), now: now),
          'Yesterday · 8:15 PM');
      expect(HistoryFormat.sessionTitle(DateTime(2026, 10, 2, 18), now: now),
          'Fri, Oct 2 · 6:00 PM');
    });

    test('midnight and noon', () {
      expect(HistoryFormat.time(DateTime(2026, 1, 1, 0, 5)), '12:05 AM');
      expect(HistoryFormat.time(DateTime(2026, 1, 1, 12)), '12:00 PM');
    });
  });

  test('PastSession summary', () {
    final session = PastSession.fromJson({
      'id': 'a',
      'startTime': '2026-11-03T16:30:00',
      'activeSeconds': 3000,
      'focusedSeconds': 2820,
      'focusedPercentage': 94.0,
      'distractions': 1,
      'rewardEarned': 20,
    });
    expect(session.summary, '50 min · 1 distraction');
  });

  test('HistoryData reads the backend JSON', () {
    final data = HistoryData.fromJson({
      'range': 'week',
      'totalFocusSeconds': 18000,
      'sessionCount': 7,
      'avgFocusPercentage': 86.4,
      'bars': [
        {'label': 'M', 'focusMinutes': 25, 'current': false},
        {'label': 'S', 'focusMinutes': 50, 'current': true},
      ],
      'sessions': [],
    });
    expect(data.sessionCount, 7);
    expect(data.bars.last.current, isTrue);
  });

  test('ShopItem reads a product and keeps owned/equipped on copy', () {
    final item = ShopItem.fromProductJson({
      'id': 'p1',
      'name': 'Party Hat',
      'category': 'hat',
      'price': 60,
      'available': true,
      'stock': 0,
    });
    final owned = item.copyWith(owned: true, equipped: true);
    expect(owned.price, 60);
    expect(owned.owned && owned.equipped, isTrue);
  });

  test('RobotLook picks the accessory from the item name', () {
    expect(RobotLook.forItem('Gold Crown', 'hat').accessory, RobotAccessory.crown);
    expect(RobotLook.forItem('Party Hat', 'hat').accessory, RobotAccessory.cap);
    expect(RobotLook.forItem('Ice Screen', 'screen').accessory, RobotAccessory.none);
    expect(RobotLook.forItem('Kaboom Sparkle', 'effect').glowColor, isNotNull);
  });
}
