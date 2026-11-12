package com.delivery.web;

import com.delivery.model.Courier;
import com.delivery.model.District;
import com.delivery.repository.DistrictRepository;
import com.delivery.service.CourierService;
import com.delivery.web.dto.CourierProfileDto;
import com.delivery.web.dto.SetDistrictRequest;
import io.javalin.http.Context;

import static com.delivery.web.Contexts.pathId;

/**
 * Courier endpoints: district choice and shift open/close for the
 * "Страница пользователя" (courier variant).
 */
public class CourierController {

    private final CourierService courierService;
    private final DistrictRepository districtRepository;

    public CourierController(CourierService courierService, DistrictRepository districtRepository) {
        this.courierService = courierService;
        this.districtRepository = districtRepository;
    }

    /** GET /api/couriers/{userId} — the courier profile. */
    public void getProfile(Context ctx) {
        long userId = pathId(ctx, "userId");
        ctx.json(toDto(courierService.getProfile(userId)));
    }

    /** PUT /api/couriers/{userId}/district — choose the work district (off-shift only). */
    public void setDistrict(Context ctx) {
        long userId = pathId(ctx, "userId");
        SetDistrictRequest request = ctx.bodyAsClass(SetDistrictRequest.class);
        ctx.json(toDto(courierService.changeDistrict(userId, request.districtId())));
    }

    /** POST /api/couriers/{userId}/shift/start — open a shift. */
    public void startShift(Context ctx) {
        long userId = pathId(ctx, "userId");
        ctx.json(toDto(courierService.startShift(userId)));
    }

    /** POST /api/couriers/{userId}/shift/end — close the shift. */
    public void endShift(Context ctx) {
        long userId = pathId(ctx, "userId");
        ctx.json(toDto(courierService.endShift(userId)));
    }

    private CourierProfileDto toDto(Courier courier) {
        String districtName = null;
        if (courier.getCurrentDistrictId() != null) {
            districtName = districtRepository.findById(courier.getCurrentDistrictId())
                    .map(District::getName)
                    .orElse("?");
        }
        return new CourierProfileDto(courier.getIdUser(), courier.getId(),
                courier.getCurrentDistrictId(), districtName, courier.isOnShift());
    }
}
