import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';
import 'package:get/get.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_error.dart';
import '../../../core/storage/token_storage.dart';
import '../../../shared/controllers/outfit_controller.dart';
import '../../../shared/controllers/user_controller.dart';
import '../models/shop_item.dart';
import '../services/shop_service.dart';

/// Logic and state for the Shop screen.
/// The coins come from the shared [UserController], so they match the side menu.
class ShopController extends GetxController {
  ShopController({required this.users, ApiClient? api})
      : _shop = ShopService(api ?? ApiClient(TokenStorage()));

  final UserController users;
  final ShopService _shop;

  // ---------- state (observable) ----------

  /// RxList: use it like a normal list. Replace it all with assignAll(...).
  final items = <ShopItem>[].obs;
  final filter = 'all'.obs;
  final isLoading = false.obs;
  final errorMessage = RxnString();

  /// Product pictures uploaded by an admin, by product id. Items without one draw Sparky.
  /// RxMap: images[id] = bytes redraws the cards by itself.
  final images = <String, Uint8List>{}.obs;

  /// The item being bought or equipped right now (its card shows a spinner).
  final busyItemId = RxnString();

  // ---------- lifecycle ----------

  /// Called once, right after Get.put creates the controller.
  @override
  void onInit() {
    super.onInit();
    load();
  }

  // ---------- values computed from the state ----------

  /// Reads users.user.value, so an Obx that shows the coins
  /// also redraws when the shared balance changes.
  int? get coins => users.user.value?.coins;

  /// The items for the selected tab: owned items first, then by price.
  List<ShopItem> get visibleItems {
    final list = filter.value == 'all'
        ? [...items]
        : items.where((item) => item.category == filter.value).toList();
    list.sort((a, b) {
      if (a.owned != b.owned) return a.owned ? -1 : 1;
      return a.price.compareTo(b.price);
    });
    return list;
  }

  // ---------- actions ----------

  void setFilter(String value) => filter.value = value;

  /// Loads the products and what the user owns (and the user, if not loaded yet).
  Future<void> load() async {
    isLoading.value = true;
    errorMessage.value = null;
    try {
      final results = await Future.wait<Object?>([
        _shop.getProducts(),
        _shop.getMyItems(),
        if (users.user.value == null) users.load(),
      ]);
      final products = results[0] as List<ShopItem>;
      final owned = {
        for (final item in results[1] as List<OwnedItem>) item.productId: item,
      };
      items.assignAll([
        for (final product in products)
          product.copyWith(
            owned: owned.containsKey(product.id),
            equipped: owned[product.id]?.equipped ?? false,
          ),
      ]);
      _loadImages(products);
    } on DioException catch (e) {
      errorMessage.value = apiErrorMessage(e);
    } finally {
      isLoading.value = false;
    }
  }

  /// Downloads the pictures in the background; each card updates when its picture arrives.
  Future<void> _loadImages(List<ShopItem> products) async {
    final missing = products.where((p) => !images.containsKey(p.id)).toList();
    final bytes = await Future.wait(missing.map((p) => _shop.getImage(p.id)));
    for (var i = 0; i < missing.length; i++) {
      final image = bytes[i];
      if (image != null) images[missing[i].id] = image;
    }
  }

  /// Buys [item] and puts it on Sparky. Returns null on success, or a message to show.
  Future<String?> buy(ShopItem item) async {
    busyItemId.value = item.id;
    try {
      await _shop.buy(item.id);
      _replace(item.copyWith(owned: true));

      // "Sparky will wear it in your next session": equip it right away.
      // If only this step fails, the item is still bought; the user can tap it to wear it.
      try {
        await _shop.setEquipped(item.id, true);
        _markEquipped(item, true);
        _refreshOutfit();
      } on DioException catch (_) {
        // Bought anyway; the card shows "Owned" instead of "Equipped".
      }

      // The coins changed: reload the shared user so every screen shows the new balance.
      await users.reload();
      return null;
    } on DioException catch (e) {
      return _buyError(e);
    } finally {
      busyItemId.value = null;
    }
  }

  /// Puts an owned item on, or takes it off. Returns null on success, or a message.
  Future<String?> toggleEquipped(ShopItem item) async {
    busyItemId.value = item.id;
    try {
      await _shop.setEquipped(item.id, !item.equipped);
      _markEquipped(item, !item.equipped);
      _refreshOutfit();
      return null;
    } on DioException catch (e) {
      return apiErrorMessage(e);
    } finally {
      busyItemId.value = null;
    }
  }

  /// Sparky wears one item per category, like the backend: equipping one takes off the others.
  void _markEquipped(ShopItem item, bool equipped) {
    items.assignAll([
      for (final other in items)
        if (other.id == item.id)
          other.copyWith(equipped: equipped)
        else if (equipped && other.category == item.category && other.equipped)
          other.copyWith(equipped: false)
        else
          other,
    ]);
  }

  /// Tells the shared outfit that something was put on or taken off,
  /// so Sparky on Home is already dressed when you go back.
  void _refreshOutfit() {
    if (Get.isRegistered<OutfitController>()) Get.find<OutfitController>().load();
  }

  void _replace(ShopItem updated) {
    items.assignAll([for (final other in items) other.id == updated.id ? updated : other]);
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