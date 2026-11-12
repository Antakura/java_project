package com.delivery.service;

import com.delivery.model.Courier;
import com.delivery.model.Role;
import com.delivery.model.User;
import com.delivery.repository.CourierRepository;
import com.delivery.repository.DistrictRepository;
import com.delivery.repository.UserRepository;

/**
 * Courier service: work district and shift management.
 *
 * <p>All state changes go through the invariants on {@link Courier} itself
 * (no district change mid-shift, no shift without a district); this service only
 * adds the role check ("only couriers may do courier things") and persistence.</p>
 */
public class CourierService {

    private final UserRepository userRepository;
    private final CourierRepository courierRepository;
    private final DistrictRepository districtRepository;

    public CourierService(UserRepository userRepository,
                          CourierRepository courierRepository,
                          DistrictRepository districtRepository) {
        this.userRepository = userRepository;
        this.courierRepository = courierRepository;
        this.districtRepository = districtRepository;
    }

    /** The courier profile of a user (must be a COURIER). */
    public Courier getProfile(long userId) {
        User user = requireCourierUser(userId);
        return ServiceGuards.requireCourierProfile(courierRepository, user.getId());
    }

    /** Chooses the work district. Rejected while on shift (model invariant). */
    public Courier changeDistrict(long actorUserId, long districtId) {
        User user = requireCourierUser(actorUserId);
        districtRepository.findById(districtId)
                .orElseThrow(() -> new IllegalArgumentException("District not found: " + districtId));
        Courier courier = getProfile(user.getId());
        courier.changeDistrict(districtId);
        courierRepository.save(courier);
        return courier;
    }

    /** Opens a shift. Requires a chosen district (model invariant). */
    public Courier startShift(long actorUserId) {
        User user = requireCourierUser(actorUserId);
        Courier courier = getProfile(user.getId());
        courier.startShift();
        courierRepository.save(courier);
        return courier;
    }

    /** Closes the current shift. */
    public Courier endShift(long actorUserId) {
        User user = requireCourierUser(actorUserId);
        Courier courier = getProfile(user.getId());
        courier.endShift();
        courierRepository.save(courier);
        return courier;
    }

    private User requireCourierUser(long userId) {
        User user = ServiceGuards.requireUser(userRepository, userId);
        if (user.getRole() != Role.COURIER) {
            throw new IllegalStateException(
                    "User " + user.getUsername() + " is not a courier");
        }
        return user;
    }
}
