import '../../../core/network/api_client.dart';
import '../models/admin_stats.dart';
import '../models/product.dart';

/// HTTP calls for the admin screens. No UI and no state here.
class AdminService {
  AdminService(this._api);

  final ApiClient _api;

  /// GET /api/v1/admin/dashboard/stats
  Future<AdminStats> getStats() async {
    final response = await _api.dio.get('/admin/dashboard/stats');
    return AdminStats.fromJson(response.data as Map<String, dynamic>);
  }

  /// GET /api/v1/admin/products
  Future<List<Product>> getProducts() async {
    final response = await _api.dio.get('/admin/products');
    final list = response.data as List<dynamic>;
    return list
        .map((item) => Product.fromJson(item as Map<String, dynamic>))
        .toList();
  }

  /// POST /api/v1/admin/products
  /// The backend requires every field, including stock (new products start at 0).
  Future<Product> addProduct({
    required String name,
    required String category,
    required int price,
    String? description,
    int stock = 0,
  }) async {
    final response = await _api.dio.post('/admin/products', data: {
      'name': name,
      'category': category,
      'price': price,
      'description': (description == null || description.isEmpty) ? null : description,
      'stock': stock,
    });
    return Product.fromJson(response.data as Map<String, dynamic>);
  }

  /// PUT /api/v1/admin/products/{id}
  Future<Product> updateProduct(
    String id, {
    required String name,
    required String category,
    required int price,
    String? description,
    required int stock,
  }) async {
    final response = await _api.dio.put('/admin/products/$id', data: {
      'name': name,
      'category': category,
      'price': price,
      'description': (description == null || description.isEmpty) ? null : description,
      'stock': stock,
    });
    return Product.fromJson(response.data as Map<String, dynamic>);
  }

  /// PATCH /api/v1/admin/products/{id}/availability?available=true|false
  Future<Product> setAvailability(String id, bool available) async {
    final response = await _api.dio.patch(
      '/admin/products/$id/availability',
      queryParameters: {'available': available},
    );
    return Product.fromJson(response.data as Map<String, dynamic>);
  }
}