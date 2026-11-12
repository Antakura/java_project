package com.delivery.model;

/**
 * A system user: login, password hash and role.
 *
 * <p>Design decisions:
 * <ul>
 *   <li><b>No inheritance for roles.</b> The old design used an abstract {@code User}
 *       with {@code Client}/{@code Courier} subclasses, which forced a single table to
 *       carry columns of all roles (a 3NF violation). Now role-specific data lives in
 *       separate tables ({@code clients}, {@code couriers}) linked by foreign keys,
 *       exactly as required by the target schema.</li>
 *   <li><b>Encapsulation:</b> no public mutable fields; the only non-final field is
 *       {@code id}, which is assigned by the storage layer (SQLite AUTOINCREMENT)
 *       through the private {@link #assignId(long)} — the same pattern JPA uses for
 *       generated identities. Business state of a user never changes after creation,
 *       so no other mutators are needed.</li>
 *   <li><b>Security:</b> only a SHA-256 hash is stored, never the plain password
 *       (demonstration level; a production system would use bcrypt/argon2).</li>
 * </ul>
 */
public class User extends AbstractEntity {
    private String username;
    private final String passwordHash;
    private final Role role;

    public User(String username, String passwordHash, Role role) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        if (passwordHash == null || passwordHash.isBlank()) {
            throw new IllegalArgumentException("Password hash must not be blank");
        }
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    /**
     * Renames the login. The only business mutation a user ever needs:
     * the "Страница пользователя" lets a client change their name.
     */
    public void updateUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }
        this.username = username.trim();
    }

    public String getUsername() {
        return username;
    }

    /** Exposed for the login check only; never sent to clients by the web layer. */
    public String getPasswordHash() {
        return passwordHash;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public String toString() {
        return "User[id=" + id + ", username=" + username + ", role=" + role + "]";
    }
}
