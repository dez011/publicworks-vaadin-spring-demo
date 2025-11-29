// src/main/java/net/publicworks/app/frontend/view/HomeView.java
package net.publicworks.app.frontend.layout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import net.publicworks.app.backend.entity.User;
import net.publicworks.app.frontend.CurrentUser;

@Route(value = "app", layout = MainLayout.class)
@PageTitle("Dashboard | Public Works")
public class HomeView extends VerticalLayout implements BeforeEnterObserver {

    public HomeView() {
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);

        User user = CurrentUser.get();

        String greeting = (user != null)
                ? "Welcome, " + user.getEmail()
                : "Welcome";

        String tenant = (user != null && user.getTenantId() != null)
                ? "User: " + user.getTenantId()
                : "No user configured yet";

        H2 title = new H2(greeting);
        Paragraph subtitle = new Paragraph(tenant);

        add(title, subtitle);

        // Responsive cards layout (mobile-first)
        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();
        cards.getStyle().set("flex-wrap", "wrap");
        cards.setSpacing(true);

        cards.add(
                createCard("Open Work Orders",
                        "Track and assign field work for today."),
                createCard("Assets",
                        "View critical infrastructure: mains, valves, hydrants."),
                createCard("Reports",
                        "Export activity and compliance reports.")
        );

        add(cards);
        expand(cards);
    }

    private Div createCard(String title, String body) {
        Div card = new Div();
        card.getStyle()
                .set("border-radius", "8px")
                .set("padding", "1rem")
                .set("box-shadow", "0 1px 4px rgba(0,0,0,0.08)")
                .set("background-color", "var(--lumo-base-color)")
                .set("min-width", "220px")
                .set("flex", "1 1 220px");

        H2 h = new H2(title);
        h.getStyle().set("font-size", "1rem").set("margin", "0 0 0.25rem 0");

        Paragraph p = new Paragraph(body);
        p.getStyle().set("font-size", "0.875rem").set("margin", "0");

        card.add(h, p);
        return card;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        // guard: if not logged in, bounce back to login
        if (!CurrentUser.isLoggedIn()) {
            event.rerouteTo("");
        }
    }
}
