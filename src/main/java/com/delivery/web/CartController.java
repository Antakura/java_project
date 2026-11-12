package com.delivery.web;

import com.delivery.model.CartLine;
import com.delivery.model.Product;
import com.delivery.repository.ProductRepository;
import com.delivery.service.CartService;
import com.delivery.web.dto.AddToCartRequest;
import com.delivery.web.dto.CartLineDto;
import io.javalin.http.Context;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Cart endpoints. The GET response is grouped by restaurant id — that grouping
 * IS the "two baskets" feature: the frontend renders one section per key and
 * each section has its own checkout button.
 */
public class CartController {

    private final CartService cartService;
    private final ProductRepository productRepository;

    public CartController(CartService cartService, ProductRepository productRepository) {
        this.cartService = cartService;
        this.productRepository = productRepository;
    }

    /** GET /api/cart/{userId} — Map&lt;restaurantId, List&lt;CartLineDto&gt;&gt;. */
    public void getCart(Context ctx) {
        long userId = Long.parseLong(ctx.pathParam("userId"));
        Map<Long, List<CartLine>> split = cartService.getSplitByRestaurant(userId);

        Map<Long, List<CartLineDto>> result = new LinkedHashMap<>();
        split.forEach((restaurantId, lines) ->
                result.put(restaurantId, lines.stream().map(this::toDto).toList()));
        ctx.json(result);
    }

    /** POST /api/cart/{userId}/add — one more unit of a product. */
    public void addToCart(Context ctx) {
        long userId = Long.parseLong(ctx.pathParam("userId"));
        AddToCartRequest request = ctx.bodyAsClass(AddToCartRequest.class);
        cartService.addToCart(userId, request.productId());
        ctx.status(204).result();
    }

    /** DELETE /api/cart/{userId}/item/{productId} — remove a whole line. */
    public void removeItem(Context ctx) {
        long userId = Long.parseLong(ctx.pathParam("userId"));
        long productId = Long.parseLong(ctx.pathParam("productId"));
        cartService.removeItem(userId, productId);
        ctx.status(204).result();
    }

    /** DELETE /api/cart/{userId} — empty the whole cart. */
    public void clearCart(Context ctx) {
        long userId = Long.parseLong(ctx.pathParam("userId"));
        cartService.clearCart(userId);
        ctx.status(204).result();
    }

    private CartLineDto toDto(CartLine line) {
        Product product = productRepository.findById(line.getProductId())
                .orElseThrow(() -> new IllegalStateException(
                        "Product disappeared from the catalog: " + line.getProductId()));
        double lineTotal = Math.round(product.getPrice() * line.getQuantity() * 100.0) / 100.0;
        return new CartLineDto(product.getId(), product.getName(), product.getPrice(),
                line.getQuantity(), lineTotal);
    }
}
