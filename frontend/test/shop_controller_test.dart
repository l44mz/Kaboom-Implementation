import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';
import 'package:kaboom_app/features/shop/controllers/shop_controller.dart';
import 'package:kaboom_app/shared/controllers/user_controller.dart';
import 'package:kaboom_app/shared/services/user_service.dart';

import 'helpers/fake_backend.dart';

Map<String, dynamic> user(int coins) =>
    {'id': 'u-1', 'name': 'Shahd', 'email': 's@kaboom.app', 'coins': coins, 'role': 'user'};

Map<String, dynamic> product(String id, String category, int price) =>
    {'id': id, 'name': 'Item $id', 'category': category, 'price': price};

/// A shop with 2 hats and 1 screen. The user owns hat h-1 and is wearing it.
(ShopController, FakeBackend) makeShop({int coins = 100}) {
  final backend = FakeBackend()
    ..on('GET', '/users/me', user(coins))
    ..on('GET', '/products', [
      product('h-2', 'hat', 60),
      product('s-1', 'screen', 30),
      product('h-1', 'hat', 40),
    ])
    ..on('GET', '/users/me/items', [
      {'productId': 'h-1', 'category': 'hat', 'equipped': true},
    ]);
  final api = backend.client();
  final users = UserController(service: UserService(api));
  return (ShopController(users: users, api: api), backend);
}

void main() {
  // GetX keeps controllers in memory between tests: clear them after each one.
  tearDown(Get.reset);

  test('load marks what the user owns and is wearing', () async {
    final (shop, _) = makeShop();

    await shop.load();

    expect(shop.items, hasLength(3));
    final hat = shop.items.firstWhere((i) => i.id == 'h-1');
    expect(hat.owned, isTrue);
    expect(hat.equipped, isTrue);
    expect(shop.items.firstWhere((i) => i.id == 'h-2').owned, isFalse);
    expect(shop.coins, 100);
    expect(shop.errorMessage.value, isNull);
  });

  test('owned items come first, then the cheapest', () async {
    final (shop, _) = makeShop();
    await shop.load();

    expect(shop.visibleItems.map((i) => i.id), ['h-1', 's-1', 'h-2']);
  });

  test('a category tab shows only that category', () async {
    final (shop, _) = makeShop();
    await shop.load();

    shop.setFilter('hat');

    expect(shop.visibleItems.map((i) => i.id), ['h-1', 'h-2']);
  });

  test('buying puts the item on Sparky and updates the coins', () async {
    final (shop, backend) = makeShop();
    await shop.load();
    backend
      ..on('POST', '/purchases', {'coinsLeft': 40})
      ..on('PATCH', '/users/me/items/h-2', null)
      ..on('GET', '/users/me', user(40));

    final error = await shop.buy(shop.items.firstWhere((i) => i.id == 'h-2'));

    expect(error, isNull);
    expect(backend.sentTo('POST', '/purchases').single.body, {'productId': 'h-2'});
    final bought = shop.items.firstWhere((i) => i.id == 'h-2');
    expect(bought.owned, isTrue);
    expect(bought.equipped, isTrue);
    // One hat at a time: the old hat comes off.
    expect(shop.items.firstWhere((i) => i.id == 'h-1').equipped, isFalse);
    expect(shop.coins, 40);
    expect(shop.busyItemId.value, isNull);
  });

  test('not enough coins shows a clear message and buys nothing', () async {
    final (shop, backend) = makeShop(coins: 10);
    await shop.load();
    backend.on('POST', '/purchases', {'message': 'Not enough coins'}, status: 400);

    final error = await shop.buy(shop.items.firstWhere((i) => i.id == 'h-2'));

    expect(error, 'Not enough coins for this item.');
    expect(shop.items.firstWhere((i) => i.id == 'h-2').owned, isFalse);
    expect(shop.coins, 10);
  });

  test('tapping a worn item takes it off', () async {
    final (shop, backend) = makeShop();
    await shop.load();
    backend.on('PATCH', '/users/me/items/h-1', null);

    final error = await shop.toggleEquipped(shop.items.firstWhere((i) => i.id == 'h-1'));

    expect(error, isNull);
    expect(backend.sentTo('PATCH', '/users/me/items/h-1').single.query, {'equipped': false});
    expect(shop.items.firstWhere((i) => i.id == 'h-1').equipped, isFalse);
  });

  test('a server error shows a message instead of the items', () async {
    final (shop, backend) = makeShop();
    backend.on('GET', '/products', null, status: 500);

    await shop.load();

    expect(shop.items, isEmpty);
    expect(shop.errorMessage.value, isNotNull);
    expect(shop.isLoading.value, isFalse);
  });
}
