import 'package:dio/dio.dart';
import 'package:get/get.dart';

import '../../../core/network/api_client.dart';
import '../../../core/network/api_error.dart';
import '../../../core/storage/token_storage.dart';
import '../models/admin_stats.dart';
import '../models/product.dart';
import '../services/admin_service.dart';

/// Logic and state for the admin screens.
/// Used by two screens at the same time (the dashboard, and the form on top of it),
/// so each screen registers its own copy with a tag.
class AdminController extends GetxController {
  /// [api] is for tests (a fake backend); the app leaves it empty.
  AdminController({TokenStorage? tokens, ApiClient? api})
      : _tokens = tokens ?? TokenStorage() {
    _service = AdminService(api ?? ApiClient(_tokens));
  }

  final TokenStorage _tokens;
  late final AdminService _service;

  // ---------- state (observable) ----------

  final stats = Rxn<AdminStats>();
  final products = <Product>[].obs; // RxList
  final isLoading = false.obs;
  final isSaving = false.obs;
  final errorMessage = RxnString();

  // No onInit() here: only the dashboard needs loadAll(), not the form.
  // So the dashboard calls it itself, in initState.

  /// Loads the stats and the product list at the same time.
  Future<void> loadAll() async {
    isLoading.value = true;
    errorMessage.value = null;
    try {
      final results = await Future.wait([
        _service.getStats(),
        _service.getProducts(),
      ]);
      stats.value = results[0] as AdminStats;
      products.assignAll(results[1] as List<Product>);
    } on DioException catch (e) {
      errorMessage.value = _message(e);
    } finally {
      isLoading.value = false;
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
    isSaving.value = true;
    errorMessage.value = null;
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
      errorMessage.value = _message(e);
      return false;
    } finally {
      isSaving.value = false;
    }
  }

  /// Visible ⇄ Hidden. Returns true on success.
  Future<bool> toggleAvailability(Product product) async {
    try {
      final updated =
          await _service.setAvailability(product.id, !product.available);
      products.assignAll([
        for (final p in products) p.id == product.id ? updated : p,
      ]);
      return true;
    } on DioException catch (e) {
      errorMessage.value = _message(e);
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