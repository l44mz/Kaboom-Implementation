import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../../core/network/api_client.dart';
import '../models/shop_item.dart';

/// HTTP calls for the shop. No UI and no state here.
class ShopService {
  ShopService(this._api);

  final ApiClient _api;

  /// GET /api/v1/products: everything in the shop right now.
  Future<List<ShopItem>> getProducts() async {
    final response = await _api.dio.get('/products');
    return (response.data as List)
        .map((json) => ShopItem.fromProductJson(json as Map<String, dynamic>))
        .toList();
  }

  /// GET /api/v1/products/{id}/image: the product's picture (uploaded by an admin),
  /// or null if it has none. The endpoint needs the login token, so the bytes are
  /// downloaded here instead of using Image.network.
  Future<Uint8List?> getImage(String productId) async {
    try {
      final response = await _api.dio.get<List<int>>(
        '/products/$productId/image',
        options: Options(responseType: ResponseType.bytes),
      );
      final bytes = response.data;
      return bytes == null || bytes.isEmpty ? null : Uint8List.fromList(bytes);
    } on DioException {
      return null; // 404 = no picture yet; Sparky is drawn instead.
    }
  }

  /// GET /api/v1/users/me/items: what the user owns, and what is equipped.
  Future<List<OwnedItem>> getMyItems() async {
    final response = await _api.dio.get('/users/me/items');
    return (response.data as List)
        .map((json) => OwnedItem.fromJson(json as Map<String, dynamic>))
        .toList();
  }

  /// POST /api/v1/purchases: buys one item. Returns the coins left.
  Future<int> buy(String productId) async {
    final response = await _api.dio.post(
      '/purchases',
      data: {'productId': productId},
    );
    final data = response.data as Map<String, dynamic>;
    return (data['coinsLeft'] as num).toInt();
  }

  /// PATCH /api/v1/users/me/items/{id}?equipped=true|false
  Future<void> setEquipped(String productId, bool equipped) async {
    await _api.dio.patch(
      '/users/me/items/$productId',
      queryParameters: {'equipped': equipped},
    );
  }
}
