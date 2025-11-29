// src/main/java/net/publicworks/app/frontend/security/CurrentUser.java
package net.publicworks.app.frontend;

import com.vaadin.flow.server.VaadinSession;
import net.publicworks.app.backend.entity.User;

public final class CurrentUser {

    private static final String KEY = "currentUser";

    private CurrentUser() {}

    public static void set(User user) {
        VaadinSession.getCurrent().setAttribute(KEY, user);
    }

    public static User get() {
        return (User) VaadinSession.getCurrent().getAttribute(KEY);
    }

    public static void clear() {
        VaadinSession.getCurrent().setAttribute(KEY, null);
    }

    public static boolean isLoggedIn() {
        return get() != null;
    }

    public static boolean isAdmin() {
        User u = get();
        return u != null && "ADMIN".equalsIgnoreCase(u.getRole());
    }

    public static String tenantId() {
        User u = get();
        return u != null ? u.getTenantId() : null;
    }
}
