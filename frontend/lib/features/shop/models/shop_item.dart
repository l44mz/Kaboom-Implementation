/// A product in the shop, plus whether the logged-in user owns it and wears it.
class ShopItem {
  const ShopItem({
    required this.id,
    required this.name,
    required this.category,
    required this.price,
    this.description,
    this.owned = false,
    this.equipped = false,
  });

  final String id;
  final String name;
  final String category; // hat, screen or effect
  final int price;
  final String? description;
  final bool owned;
  final bool equipped;

  /// The shop's filter tabs: category → label ("all" shows everything).
  static const filters = {
    'all': 'All',
    'hat': 'Hats',
    'screen': 'Screens',
    'effect': 'Kaboom FX',
  };

  /// Builds an item from GET /products (the Product JSON).
  factory ShopItem.fromProductJson(Map<String, dynamic> json) => ShopItem(
        id: json['id'] as String,
        name: json['name'] as String,
        category: json['category'] as String,
        price: (json['price'] as num).toInt(),
        description: json['description'] as String?,
      );

  ShopItem copyWith({bool? owned, bool? equipped}) => ShopItem(
        id: id,
        name: name,
        category: category,
        price: price,
        description: description,
        owned: owned ?? this.owned,
        equipped: equipped ?? this.equipped,
      );
}

/// One entry of GET /users/me/items: an item the user owns.
class OwnedItem {
  const OwnedItem({
    required this.productId,
    required this.category,
    required this.equipped,
  });

  final String productId;
  final String category;
  final bool equipped;

  factory OwnedItem.fromJson(Map<String, dynamic> json) => OwnedItem(
        productId: json['productId'] as String,
        category: json['category'] as String,
        equipped: (json['equipped'] as bool?) ?? false,
      );
}
