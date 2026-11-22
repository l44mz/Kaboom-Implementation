import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:get/get.dart';

import '../../core/network/api_client.dart';
import '../../core/storage/token_storage.dart';
import '../../features/shop/services/shop_service.dart';

/// One item Sparky is wearing, with its picture (null if the admin didn't upload one).
class WornItem {
  const WornItem({required this.productId, required this.category, this.image});

  final String productId;
  final String category; // hat, screen or effect
  final Uint8List? image;
}

/// What Sparky is wearing right now: at most one item per category.
///
/// Shared like UserController (registered once in main.dart), because two
/// features need it: Home shows the outfit, and the Store changes it.
class OutfitController extends GetxController {
  OutfitController({ShopService? shop})
      : _shop = shop ?? ShopService(ApiClient(TokenStorage()));

  final ShopService _shop;

  /// category -> the item worn in it. RxMap: Obx redraws when it changes.
  final worn = <String, WornItem>{}.obs;

  /// Pictures already downloaded, by product id, so they aren't downloaded again.
  final _images = <String, Uint8List?>{};

  WornItem? get hat => worn['hat'];
  WornItem? get screen => worn['screen'];
  WornItem? get effect => worn['effect'];

  /// Loads what is equipped (GET /users/me/items) and the pictures of those items.
  /// If it fails, Sparky keeps what he was wearing (or nothing): never an error screen.
  Future<void> load() async {
    try {
      final equipped = (await _shop.getMyItems()).where((item) => item.equipped).toList();

      // Download only the pictures we don't have yet, all at the same time.
      final missing = equipped.where((item) => !_images.containsKey(item.productId)).toList();
      final bytes = await Future.wait(missing.map((item) => _shop.getImage(item.productId)));
      for (var i = 0; i < missing.length; i++) {
        _images[missing[i].productId] = bytes[i];
      }

      // assignAll replaces everything at once: items taken off disappear too.
      worn.assignAll({
        for (final item in equipped)
          item.category: WornItem(
            productId: item.productId,
            category: item.category,
            image: _images[item.productId],
          ),
      });
    } on DioException {
      // Offline or logged out: keep the current outfit.
    }
  }

  /// Forgets the outfit (on log out).
  void clear() => worn.clear();
}