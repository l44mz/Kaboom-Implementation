import 'package:flutter_test/flutter_test.dart';
import 'package:get/get.dart';
import 'package:kaboom_app/features/admin/controllers/admin_controller.dart';

import 'helpers/fake_backend.dart';

Map<String, dynamic> product(String id, {bool available = true}) => {
      'id': id,
      'name': 'Party hat',
      'category': 'hat',
      'price': 50,
      'available': available,
      'stock': 0,
    };

void main() {
  tearDown(Get.reset);

  test('the dashboard loads the stats and the products', () async {
    final backend = FakeBackend()
      ..on('GET', '/admin/dashboard/stats',
          {'totalUsers': 12, 'sessionsToday': 5, 'robotsOnline': 2, 'storeProducts': 3})
      ..on('GET', '/admin/products', [product('p-1'), product('p-2')]);
    final admin = AdminController(api: backend.client());

    await admin.loadAll();

    expect(admin.stats.value?.totalUsers, 12);
    expect(admin.stats.value?.robotsOnline, 2);
    expect(admin.products, hasLength(2));
    expect(admin.isLoading.value, isFalse);
  });

  test('a user who is not an admin sees "Only admins can do this."', () async {
    final backend = FakeBackend()
      ..on('GET', '/admin/dashboard/stats', null, status: 403)
      ..on('GET', '/admin/products', null, status: 403);
    final admin = AdminController(api: backend.client());

    await admin.loadAll();

    expect(admin.errorMessage.value, 'Only admins can do this.');
    expect(admin.stats.value, isNull);
  });

  test('adding a product sends the trimmed fields', () async {
    final backend = FakeBackend()..on('POST', '/admin/products', product('p-9'));
    final admin = AdminController(api: backend.client());

    final ok = await admin.saveProduct(
      name: '  Party hat ',
      category: 'hat',
      price: 50,
      description: '',
      available: true,
    );

    expect(ok, isTrue);
    final body = backend.sentTo('POST', '/admin/products').single.body as Map;
    expect(body['name'], 'Party hat');
    expect(body['price'], 50);
    expect(body['description'], isNull);
    // Already visible, so no extra availability call.
    expect(backend.sentTo('PATCH', '/admin/products/p-9/availability'), isEmpty);
  });

  test('adding a hidden product also hides it', () async {
    final backend = FakeBackend()
      ..on('POST', '/admin/products', product('p-9'))
      ..on('PATCH', '/admin/products/p-9/availability', product('p-9', available: false));
    final admin = AdminController(api: backend.client());

    final ok = await admin.saveProduct(
      name: 'Party hat',
      category: 'hat',
      price: 50,
      available: false,
    );

    expect(ok, isTrue);
    expect(backend.sentTo('PATCH', '/admin/products/p-9/availability').single.query,
        {'available': false});
  });

  test('Visible / Hidden switch updates that product in the list', () async {
    final backend = FakeBackend()
      ..on('GET', '/admin/dashboard/stats',
          {'totalUsers': 1, 'sessionsToday': 0, 'robotsOnline': 0, 'storeProducts': 1})
      ..on('GET', '/admin/products', [product('p-1')])
      ..on('PATCH', '/admin/products/p-1/availability', product('p-1', available: false));
    final admin = AdminController(api: backend.client());
    await admin.loadAll();

    final ok = await admin.toggleAvailability(admin.products.single);

    expect(ok, isTrue);
    expect(admin.products.single.available, isFalse);
  });

  test('a failed save returns false and shows the reason', () async {
    final backend = FakeBackend()
      ..on('POST', '/admin/products', {'message': 'Price must be more than 0'}, status: 400);
    final admin = AdminController(api: backend.client());

    final ok = await admin.saveProduct(
      name: 'Party hat',
      category: 'hat',
      price: 50,
      available: true,
    );

    expect(ok, isFalse);
    expect(admin.errorMessage.value, 'Price must be more than 0');
    expect(admin.isSaving.value, isFalse);
  });

  test('the product form checks the name and the price', () {
    expect(AdminController.validateName(''), isNotNull);
    expect(AdminController.validateName('a' * 101), isNotNull);
    expect(AdminController.validateName('Party hat'), isNull);

    expect(AdminController.validatePrice('abc'), isNotNull);
    expect(AdminController.validatePrice('0'), isNotNull);
    expect(AdminController.validatePrice('50'), isNull);
  });
}
