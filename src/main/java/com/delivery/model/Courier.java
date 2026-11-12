package com.delivery.model;

/**
 * Courier profile: work district and shift state.
 *
 * <p>Lives in its own {@code couriers} table linked to {@code users} by
 * {@code id_user} (1:1). The old design stored a "vehicle type" here, which the
 * target schema does not have, so it was dropped.</p>
 *
 * <p><b>Invariants enforced inside this class</b> (encapsulation of state changes):
 * <ul>
 *   <li>a courier can only start a shift if a work district is chosen;</li>
 *   <li>the work district can only be changed while the courier is OFF shift —
 *       reassigning a working courier mid-shift would orphan in-flight deliveries;</li>
 *   <li>ending an already-ended shift is rejected.</li>
 * </ul>
 */
public class Courier {
    /** Assigned by the database on insert; 0 until persisted. */
    private long id;
    /** FK -> users.id: which login this profile belongs to. */
    private final long idUser;
    /** FK -> districts.id, nullable: chosen work district (null = not chosen yet). */
    private Long currentDistrictId;
    private boolean onShift;

    public Courier(long idUser) {
        if (idUser <= 0) {
            throw new IllegalArgumentException("Courier must reference an existing user");
        }
        this.idUser = idUser;
        this.currentDistrictId = null;
        this.onShift = false;
    }

    /** Rehydration constructor used by the repository to restore persisted state. */
    public static Courier rehydrate(long id, long idUser, Long currentDistrictId, boolean onShift) {
        Courier courier = new Courier(idUser);
        courier.id = id;
        courier.currentDistrictId = currentDistrictId;
        courier.onShift = onShift;
        return courier;
    }

    public void assignId(long id) {
        if (this.id != 0) {
            throw new IllegalStateException("Courier already has an id: " + this.id);
        }
        this.id = id;
    }

    /** Chooses the district where the courier will work. Only allowed off-shift. */
    public void changeDistrict(long districtId) {
        if (districtId <= 0) {
            throw new IllegalArgumentException("Invalid district id: " + districtId);
        }
        if (onShift) {
            throw new IllegalStateException(
                    "Cannot change the work district while on shift. End the shift first.");
        }
        this.currentDistrictId = districtId;
    }

    /** Opens a shift. Requires a chosen district. */
    public void startShift() {
        if (onShift) {
            throw new IllegalStateException("Courier is already on shift");
        }
        if (currentDistrictId == null) {
            throw new IllegalStateException(
                    "Cannot start a shift without a chosen work district");
        }
        this.onShift = true;
    }

    /** Closes the current shift. */
    public void endShift() {
        if (!onShift) {
            throw new IllegalStateException("Courier is not on shift");
        }
        this.onShift = false;
    }

    public long getId() {
        return id;
    }

    public long getIdUser() {
        return idUser;
    }

    /** @return the chosen district id, or {@code null} if none was chosen yet. */
    public Long getCurrentDistrictId() {
        return currentDistrictId;
    }

    public boolean isOnShift() {
        return onShift;
    }

    @Override
    public String toString() {
        return "Courier[id=" + id + ", user=" + idUser
                + ", district=" + currentDistrictId + ", onShift=" + onShift + "]";
    }
}
