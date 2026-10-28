import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:go_router/go_router.dart';

import '../../../core/theme/app_colors.dart';
import '../../../core/theme/app_theme.dart';
import '../../../shared/widgets/app_snackbar.dart';
import '../controllers/admin_controller.dart';
import '../models/product.dart';
import '../widgets/dashed_upload_box.dart';

/// Add a new product, or edit [product] when one is passed.
class AddProductScreen extends StatefulWidget {
  const AddProductScreen({super.key, this.product});

  /// null = add a new product; otherwise edit this one.
  final Product? product;

  @override
  State<AddProductScreen> createState() => _AddProductScreenState();
}

class _AddProductScreenState extends State<AddProductScreen> {
  final _formKey = GlobalKey<FormState>();
  final _nameController = TextEditingController();
  final _priceController = TextEditingController();
  final _descriptionController = TextEditingController();
  final _controller = AdminController();

  String _category = Product.categories.first;
  bool _available = true;

  bool get _isEdit => widget.product != null;

  @override
  void initState() {
    super.initState();
    final p = widget.product;
    if (p != null) {
      _nameController.text = p.name;
      _priceController.text = '${p.price}';
      _descriptionController.text = p.description ?? '';
      _category = p.category;
      _available = p.available;
    }
  }

  @override
  void dispose() {
    _nameController.dispose();
    _priceController.dispose();
    _descriptionController.dispose();
    _controller.dispose();
    super.dispose();
  }

  Future<void> _onSubmit() async {
    FocusScope.of(context).unfocus();
    if (!_formKey.currentState!.validate()) return;

    final ok = await _controller.saveProduct(
      existing: widget.product,
      name: _nameController.text,
      category: _category,
      price: int.parse(_priceController.text.trim()),
      description: _descriptionController.text,
      available: _available,
    );
    if (!mounted) return;

    if (ok) {
      context.pop(true); // the dashboard shows a message and reloads
    } else {
      showAppSnackBar(context, _controller.errorMessage!, isError: true);
    }
  }

  Widget _label(String text) => Padding(
        padding: const EdgeInsets.only(bottom: 8),
        child: Text(
          text,
          style: const TextStyle(fontSize: 17, fontWeight: FontWeight.w700),
        ),
      );

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.fromLTRB(24, 8, 24, 32),
          child: Form(
            key: _formKey,
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                // Back
                Align(
                  alignment: Alignment.centerLeft,
                  child: IconButton(
                    tooltip: 'Back',
                    icon: const Icon(Icons.arrow_back),
                    onPressed: () => context.pop(),
                  ),
                ),
                const SizedBox(height: 8),

                // Title
                Text(
                  _isEdit ? 'Edit Product' : 'Add Product',
                  style: const TextStyle(fontSize: 36, fontWeight: FontWeight.w700),
                ),
                const SizedBox(height: 24),

                // Image (not supported by the backend yet)
                DashedUploadBox(
                  onTap: () => showAppSnackBar(
                    context,
                    "Image upload isn't available yet.",
                  ),
                ),
                const SizedBox(height: 24),

                // Name
                _label('Product name'),
                TextFormField(
                  controller: _nameController,
                  decoration: const InputDecoration(hintText: 'e.g. Party Hat'),
                  validator: AdminController.validateName,
                ),
                const SizedBox(height: 20),

                // Category
                _label('Category'),
                Wrap(
                  spacing: 10,
                  runSpacing: 10,
                  children: Product.categories.map((c) {
                    final selected = _category == c;
                    return ChoiceChip(
                      label: Text(Product.categoryLabels[c]!),
                      selected: selected,
                      showCheckmark: false,
                      onSelected: (_) => setState(() => _category = c),
                      selectedColor: AppColors.primary,
                      backgroundColor: Colors.transparent,
                      shape: StadiumBorder(
                        side: BorderSide(
                          color: selected ? AppColors.primary : AppColors.border,
                        ),
                      ),
                      labelStyle: TextStyle(
                        fontSize: 15,
                        fontWeight: FontWeight.w600,
                        color: selected ? Colors.white : AppColors.textMuted,
                      ),
                      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 6),
                    );
                  }).toList(),
                ),
                const SizedBox(height: 20),

                // Price
                _label('Price (coins)'),
                TextFormField(
                  controller: _priceController,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  style: AppTheme.numberStyle.copyWith(
                    fontSize: 16,
                    color: AppColors.textPrimary,
                  ),
                  decoration: const InputDecoration(
                    hintText: '0',
                    prefixIcon: Padding(
                      padding: EdgeInsets.only(left: 16, right: 10),
                      child: Icon(Icons.circle, size: 12, color: AppColors.coin),
                    ),
                    prefixIconConstraints: BoxConstraints(minWidth: 0, minHeight: 0),
                  ),
                  validator: AdminController.validatePrice,
                ),
                const SizedBox(height: 20),

                // Description
                _label('Description'),
                TextFormField(
                  controller: _descriptionController,
                  maxLines: 4,
                  decoration: const InputDecoration(hintText: 'Describe the item...'),
                ),
                const SizedBox(height: 20),

                // Show in store
                Row(
                  children: [
                    const Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Show in store',
                            style: TextStyle(fontSize: 17, fontWeight: FontWeight.w600),
                          ),
                          SizedBox(height: 2),
                          Text(
                            'Users can see and buy it',
                            style: TextStyle(fontSize: 13, color: AppColors.textMuted),
                          ),
                        ],
                      ),
                    ),
                    Switch(
                      value: _available,
                      onChanged: (value) => setState(() => _available = value),
                      activeTrackColor: AppColors.primary,
                      thumbColor: const WidgetStatePropertyAll(Colors.white),
                    ),
                  ],
                ),
                const SizedBox(height: 28),

                // Save
                Center(
                  child: SizedBox(
                    width: 316,
                    child: ListenableBuilder(
                      listenable: _controller,
                      builder: (context, _) => FilledButton(
                        onPressed: _controller.isSaving ? null : _onSubmit,
                        style: FilledButton.styleFrom(
                          backgroundColor: AppColors.secondary,
                          foregroundColor: Colors.white,
                          minimumSize: const Size.fromHeight(60),
                          shape: RoundedRectangleBorder(
                            borderRadius: BorderRadius.circular(16),
                          ),
                          textStyle: const TextStyle(
                            fontSize: 20,
                            fontWeight: FontWeight.w700,
                          ),
                        ),
                        child: _controller.isSaving
                            ? const SizedBox(
                                width: 22,
                                height: 22,
                                child: CircularProgressIndicator(
                                  strokeWidth: 2.5,
                                  color: Colors.white,
                                ),
                              )
                            : const Text('Save Product'),
                      ),
                    ),
                  ),
                ),
                const SizedBox(height: 14),

                // Cancel
                Center(
                  child: SizedBox(
                    width: 316,
                    child: OutlinedButton(
                      onPressed: () => context.pop(),
                      style: OutlinedButton.styleFrom(
                        foregroundColor: Colors.white,
                        side: const BorderSide(color: AppColors.secondary, width: 2),
                        minimumSize: const Size.fromHeight(60),
                        shape: RoundedRectangleBorder(
                          borderRadius: BorderRadius.circular(16),
                        ),
                        textStyle: const TextStyle(
                          fontSize: 20,
                          fontWeight: FontWeight.w700,
                        ),
                      ),
                      child: const Text('Cancel'),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}