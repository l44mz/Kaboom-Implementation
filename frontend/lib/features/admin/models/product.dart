/// A shop item, as returned by /api/v1/admin/products.
class Product {
  const Product({
    required this.id,
    required this.name,
    required this.category,
    required this.price,
    this.description,
    required this.available,
    required this.stock,
  });

  final String id;
  final String name;
  final String category; // hat, screen or effect
  final int price;
  final String? description;
  final bool available;
  final int stock;

  /// The categories the backend accepts, and how the app names them.
  static const categories = ['hat', 'screen', 'effect'];
  static const categoryLabels = {
    'hat': 'Hats',
    'screen': 'Screens',
    'effect': 'Kaboom FX',
  };

  String get categoryLabel => categoryLabels[category] ?? category;

  factory Product.fromJson(Map<String, dynamic> json) => Product(
        id: json['id'] as String,
        name: json['name'] as String,
        category: json['category'] as String,
        price: (json['price'] as num).toInt(),
        description: json['description'] as String?,
        available: (json['available'] as bool?) ?? true,
        stock: (json['stock'] as num?)?.toInt() ?? 0,
      );
}