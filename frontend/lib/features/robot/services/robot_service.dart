import '../../../core/network/api_client.dart';
import '../../account/models/robot_info.dart';

/// HTTP calls for the robot. No UI and no state here.
class RobotService {
  RobotService(this._api);

  final ApiClient _api;

  /// POST /api/v1/robots/pair: links the robot that shows this code on its screen
  /// to the logged-in user. Returns the robot (name, connected).
  ///   404 = the code is wrong or expired
  ///   409 = the user already has a different robot
  Future<RobotInfo> pair(String code) async {
    final response = await _api.dio.post(
      '/robots/pair',
      data: {'pairingCode': code},
    );
    return RobotInfo.fromJson(response.data as Map<String, dynamic>);
  }
}