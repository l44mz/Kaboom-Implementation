import '../models/focus_state.dart';

/// Fake robot states until the US7 backend sends real ones (card: "Uses a mock
/// state stream until the US7 backend is ready").
///
/// Cycles focused, distracted, away, focused, ... every [every].
Stream<FocusState> mockFocusStates({
  Duration every = const Duration(seconds: 8),
}) async* {
  const cycle = [FocusState.focused, FocusState.distracted, FocusState.away];
  var i = 0;
  while (true) {
    yield cycle[i % cycle.length];
    i++;
    await Future<void>.delayed(every);
  }
}
