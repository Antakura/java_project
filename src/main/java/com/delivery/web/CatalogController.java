package com.delivery.web;

import com.delivery.model.District;
import com.delivery.model.Product;
import com.delivery.model.Restaurant;
import com.delivery.repository.DistrictRepository;
import com.delivery.repository.RestaurantRepository;
import com.delivery.service.CatalogService;
import com.delivery.web.dto.DistrictDto;
import com.delivery.web.dto.ProductDto;
import com.delivery.web.dto.RestaurantDto;
import com.delivery.web.dto.UpdateProductRequest;
import io.javalin.http.Context;

import java.util.List;
import java.util.Map;

/**
 * Catalog endpoints: restaurants (Главная), menus (Страница ресторана),
 * districts and the admin product table.
 */
public class CatalogController {

    private final CatalogService catalogService;
    private final DistrictRepository districtRepository;
    private final RestaurantRepository restaurantRepository;

    public CatalogController(CatalogService catalogService,
                             DistrictRepository districtRepository,
                             RestaurantRepository restaurantRepository) {
        this.catalogService = catalogService;
        this.districtRepository = districtRepository;
        this.restaurantRepository = restaurantRepository;
    }

    /** GET /api/restaurants — cards for the main page. */
    public void getRestaurants(Context ctx) {
        List<RestaurantDto> restaurants = catalogService.getRestaurants().stream()
                .map(this::toRestaurantDto)
                .toList();
        ctx.json(restaurants);
    }

    /** GET /api/restaurants/{id} — the restaurant page: header + orderable menu. */
    public void getRestaurantMenu(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        Restaurant restaurant = catalogService.getRestaurants().stream()
                .filter(r -> r.getId() == id)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found: " + id));
        List<ProductDto> products = catalogService.getMenu(id).stream()
                .map(p -> toProductDto(p, restaurant.getName()))
                .toList();
        ctx.json(Map.of(
                "restaurant", toRestaurantDto(restaurant),
                "products", products));
    }

    /** GET /api/products — every product (admin table). */
    public void getAllProducts(Context ctx) {
        List<ProductDto> products = catalogService.getAllProducts().stream()
                .map(this::toProductDtoWithRestaurantName)
                .toList();
        ctx.json(products);
    }

    /** GET /api/districts — for the courier's district selector. */
    public void getDistricts(Context ctx) {
        List<DistrictDto> districts = districtRepository.findAll().stream()
                .map(d -> new DistrictDto(d.getId(), d.getName()))
                .toList();
        ctx.json(districts);
    }

    /** PUT /api/products/{id}?actorId=... — admin edit by id (partial update). */
    public void updateProduct(Context ctx) {
        long id = Long.parseLong(ctx.pathParam("id"));
        long actorId = Long.parseLong(ctx.queryParam("actorId"));
        UpdateProductRequest request = ctx.bodyAsClass(UpdateProductRequest.class);
        Product updated = catalogService.updateProductAsAdmin(
                actorId, id, request.name(), request.description(),
                request.price(), request.isAvailable());
        ctx.json(toProductDtoWithRestaurantName(updated));
    }

    private RestaurantDto toRestaurantDto(Restaurant restaurant) {
        String districtName = districtRepository.findById(restaurant.getDistrictId())
                .map(District::getName)
                .orElse("?");
        return new RestaurantDto(restaurant.getId(), restaurant.getName(),
                restaurant.getAddress(), restaurant.getDistrictId(), districtName);
    }

    private ProductDto toProductDto(Product product, String restaurantName) {
        return new ProductDto(product.getId(), product.getName(), product.getDescription(),
                product.getPrice(), product.getImageUrl(), product.isAvailable(),
                product.getRestaurantId(), restaurantName);
    }

    private ProductDto toProductDtoWithRestaurantName(Product product) {
        String restaurantName = restaurantRepository.findById(product.getRestaurantId())
                .map(Restaurant::getName)
                .orElse("?");
        return toProductDto(product, restaurantName);
    }
}
