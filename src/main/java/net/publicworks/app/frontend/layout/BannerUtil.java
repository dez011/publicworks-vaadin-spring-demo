// src/main/java/net/publicworks/app/frontend/util/BannerUtil.java
package net.publicworks.app.frontend.layout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import net.publicworks.app.backend.itf.Result;

public final class BannerUtil {

    private BannerUtil() {}

    public static void show(Result.UiBanner banner) {
        Notification n = new Notification();

        String variant = banner.variant() == null ? "info" : banner.variant().toLowerCase();
        switch (variant) {
            case "success" -> n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
            case "error" -> n.addThemeVariants(NotificationVariant.LUMO_ERROR);
            case "warning" -> n.addThemeVariants(NotificationVariant.LUMO_WARNING);
            default -> n.addThemeVariants(NotificationVariant.LUMO_PRIMARY);
        }

        n.setDuration(banner.durationMs() != null ? banner.durationMs() : 5000);
        n.setPosition(Notification.Position.TOP_END);

        Span text = new Span(
                (banner.title() == null || banner.title().isBlank() ? "" : banner.title() + ": ")
                        + (banner.message() == null ? "" : banner.message())
        );

        FlexLayout actions = new FlexLayout();
        actions.getStyle().set("gap", "0.5rem");

        if (banner.openRoute() != null && !banner.openRoute().isBlank()) {
            String openLabel = banner.openLabel() == null ? "Open" : banner.openLabel();
            actions.add(new Button(openLabel, e -> {
                n.close();
                UI.getCurrent().navigate(banner.openRoute());
            }));
        }

        String dismissLabel = banner.dismissLabel() == null ? "Dismiss" : banner.dismissLabel();
        actions.add(new Button(dismissLabel, e -> n.close()));

        FlexLayout content = new FlexLayout(text, actions);
        content.setFlexGrow(1, text);
        content.setWidthFull();
        content.getStyle().set("align-items", "center").set("gap", "0.75rem");

        n.add(content);
        n.open();
    }
}
