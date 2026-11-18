import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_error.dart';
import '../../../core/storage/token_storage.dart';
import '../../../shared/controllers/user_controller.dart';
import '../models/shop_item.dart';
import '../services/shop_service.dart';

/// Logic and state for the Shop screen.
/// The coins come from the shared [UserController], so they match the side menu.
class ShopController extends ChangeNotifier {
  ShopController({required this.users, ApiClient? api})
      : _shop = ShopService(api ?? ApiClient(TokenStorage()));

  final UserController users;
  final ShopService _shop;

  List<ShopItem> items = [];
  String filter = 'all';
  bool isLoading = false;
  String? errorMessage;

  /// Product pictures uploaded by an admin, by product id. Items without one draw Sparky.
  final Map<String, Uint8List> images = {};

  /// The item being bought or equipped right now (its card shows a spinner).
  String? busyItemId;

  bool _disposed = false;

  @override
  void dispose() {
    _disposed = true;
    super.dispose();
  }

  int? get coins => users.user?.coins;

  /// The items for the selected tab: owned items first, then by price.
  List<ShopItem> get visibleItems {
    final list = filter == 'all'
        ? [...items]
        : items.where((item) => item.category == filter).toList();
    list.sort((a, b) {
      if (a.owned != b.owned) return a.owned ? -1 : 1;
      return a.price.compareTo(b.price);
    });
    return list;
  }

  void setFilter(String value) {
    filter = value;
    notifyListeners();
  }

  /// Loads the products and what the user owns (and the user, if not loaded yet).
  Future<void> load() async {
    isLoading = true;
    errorMessage = null;
    notifyListeners();
    try {
      final results = await Future.wait<Object?>([
        _shop.getProducts(),
        _shop.getMyItems(),
        if (users.user == null) users.load(),
      ]);
      final products = results[0] as List<ShopItem>;
      final owned = {
        for (final item in results[1] as List<OwnedItem>) item.productId: item,
      };
      items = [
        for (final product in products)
          product.copyWith(
            owned: owned.containsKey(product.id),
            equipped: owned[product.id]?.equipped ?? false,
          ),
      ];
      _loadImages(products);
    } on DioException catch (e) {
      errorMessage = apiErrorMessage(e);
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  /// Downloads the pictures in the background; the cards update when they arrive.
  Future<void> _loadImages(List<ShopItem> products) async {
    final missing = products.where((p) => !images.containsKey(p.id)).toList();
    final bytes = await Future.wait(missing.map((p) => _shop.getImage(p.id)));
    var changed = false;
    for (var i = 0; i < missing.length; i++) {
      final image = bytes[i];
      if (image != null) {
        images[missing[i].id] = image;
        changed = true;
      }
    }
    if (changed && !_disposed) notifyListeners();
  }

  /// Buys [item] and puts it on Sparky. Returns null on success, or a message to show.
  Future<String?> buy(ShopItem item) async {
    busyItemId = item.id;
    notifyListeners();
    try {
      await _shop.buy(item.id);
      _replace(item.copyWith(owned: true));

      // "Sparky will wear it in your next session": equip it right away.
      // If only this step fails, the item is still bought; the user can tap it to wear it.
      try {
        await _shop.setEquipped(item.id, true);
        _markEquipped(item, true);
      } on DioException catch (_) {
        // Bought anyway; the card shows "Owned" instead of "Equipped".
      }

      // The coins changed: reload the shared user so every screen shows the new balance.
      await users.refresh();
      return null;
    } on DioException catch (e) {
      return _buyError(e);
    } finally {
      busyItemId = null;
      notifyListeners();
    }
  }

  /// Puts an owned item on, or takes it off. Returns null on success, or a message.
  Future<String?> toggleEquipped(ShopItem item) async {
    busyItemId = item.id;
    notifyListeners();
    try {
      await _shop.setEquipped(item.id, !item.equipped);
      _markEquipped(item, !item.equipped);
      return null;
    } on DioException catch (e) {
      return apiErrorMessage(e);
    } finally {
      busyItemId = null;
      notifyListeners();
    }
  }

  /// Sparky wears one item per category, like the backend: equipping one takes off the others.
  void _markEquipped(ShopItem item, bool equipped) {
    items = [
      for (final other in items)
        if (other.id == item.id)
          other.copyWith(equipped: equipped)
        else if (equipped && other.category == item.category && other.equipped)
          other.copyWith(equipped: false)
        else
          other,
    ];
  }

  void _replace(ShopItem updated) {
    items = [for (final other in items) other.id == updated.id ? updated : other];
  }

  /// The backend's status codes for a purchase (see PurchaseService.buyProduct).
  String _buyError(DioException e) {
    switch (e.response?.statusCode) {
      case 400:
        return 'Not enough coins for this item.';
      case 409:
        return 'You already own this item, or it left the shop.';
      case 404:
        return 'This item is no longer in the shop.';
      default:
        return apiErrorMessage(e);
    }
  }
}
