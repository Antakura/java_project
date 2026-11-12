package com.delivery.model;

/**
 * Client profile: contact phone and default delivery address.
 *
 * <p>Lives in its own {@code clients} table linked to {@code users} by
 * {@code id_user} (1:1). This is the 3NF-friendly replacement for the old
 * "address column on a mixed user table".</p>
 *
 * <p>Mutability policy: phone and address are the only mutable facts of a client
 * (the "Страница пользователя" lets a client change them). Changes go through
 * validating methods, never through public fields.</p>
 */
public class Client extends AbstractEntity {
    /** FK -> users.id: which login this profile belongs to. */
    private final long idUser;
    private String phone;
    private String defaultAddress;

    public Client(long idUser, String phone, String defaultAddress) {
        if (idUser <= 0) {
            throw new IllegalArgumentException("Client must reference an existing user");
        }
        this.idUser = idUser;
        this.phone = normalizePhone(phone);
        this.defaultAddress = normalizeAddress(defaultAddress);
    }

    /** Business rule: a client must always have a reachable phone number. */
    public void updatePhone(String phone) {
        this.phone = normalizePhone(phone);
    }

    /** Business rule: the default address is used to prefill checkout. */
    public void updateDefaultAddress(String address) {
        this.defaultAddress = normalizeAddress(address);
    }

    private static String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) {
            throw new IllegalArgumentException("Client phone must not be blank");
        }
        return phone.trim();
    }

    private static String normalizeAddress(String address) {
        if (address == null || address.isBlank()) {
            throw new IllegalArgumentException("Client default address must not be blank");
        }
        return address.trim();
    }

    public long getIdUser() {
        return idUser;
    }

    public String getPhone() {
        return phone;
    }

    public String getDefaultAddress() {
        return defaultAddress;
    }

    @Override
    public String toString() {
        return "Client[id=" + id + ", user=" + idUser + ", phone=" + phone + "]";
    }
}
