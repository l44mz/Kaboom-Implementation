import 'package:dio/dio.dart';
import 'package:flutter/foundation.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_error.dart';
import '../../../core/storage/token_storage.dart';
import '../models/admin_stats.dart';
import '../models/product.dart';
import '../services/admin_service.dart';

/// Logic and state for the admin screens.
class AdminController extends ChangeNotifier {
  AdminController({TokenStorage? tokens}) : _tokens = tokens ?? TokenStorage() {
    _service = AdminService(ApiClient(_tokens));
  }

  final TokenStorage _tokens;
  late final AdminService _service;

  AdminStats? stats;
  List<Product> products = [];
  bool isLoading = false;
  bool isSaving = false;
  String? errorMessage;

  /// Loads the stats and the product list at the same time.
  Future<void> loadAll() async {
    isLoading = true;
    errorMessage = null;
    notifyListeners();
    try {
      final results = await Future.wait([
        _service.getStats(),
        _service.getProducts(),
      ]);
      stats = results[0] as AdminStats;
      products = results[1] as List<Product>;
    } on DioException catch (e) {
      errorMessage = _message(e);
    } finally {
      isLoading = false;
      notifyListeners();
    }
  }

  /// Creates a product, or updates [existing]. Returns true on success.
  Future<bool> saveProduct({
    Product? existing,
    required String name,
    required String category,
    required int price,
    String? description,
    required bool available,
  }) async {
    isSaving = true;
    errorMessage = null;
    notifyListeners();
    try {
      final saved = existing == null
          ? await _service.addProduct(
              name: name.trim(),
              category: category,
              price: price,
              description: description?.trim(),
            )
          : await _service.updateProduct(
              existing.id,
              name: name.trim(),
              category: category,
              price: price,
              description: description?.trim(),
              stock: existing.stock,
            );
      // Create and update don't change visibility, so set it separately.
      if (saved.available != available) {
        await _service.setAvailability(saved.id, available);
      }
      return true;
    } on DioException catch (e) {
      errorMessage = _message(e);
      return false;
    } finally {
      isSaving = false;
      notifyListeners();
    }
  }

  /// Visible ⇄ Hidden. Returns true on success.
  Future<bool> toggleAvailability(Product product) async {
    try {
      final updated =
          await _service.setAvailability(product.id, !product.available);
      products = [
        for (final p in products) p.id == product.id ? updated : p,
      ];
      notifyListeners();
      return true;
    } on DioException catch (e) {
      errorMessage = _message(e);
      notifyListeners();
      return false;
    }
  }

  Future<void> logout() => _tokens.clear();

  String _message(DioException e) => e.response?.statusCode == 403
      ? 'Only admins can do this.'
      : apiErrorMessage(e);

  // ---------- validation ----------

  static String? validateName(String? value) {
    final name = value?.trim() ?? '';
    if (name.isEmpty) return 'Please enter a name';
    if (name.length > 100) return 'Name must be 100 characters or less';
    return null;
  }

  /// The backend requires a price above 0.
  static String? validatePrice(String? value) {
    final price = int.tryParse(value?.trim() ?? '');
    if (price == null) return 'Enter a whole number';
    if (price <= 0) return 'Price must be more than 0';
    return null;
  }
}