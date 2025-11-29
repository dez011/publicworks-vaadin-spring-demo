package net.publicworks.app.frontend.layout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.RouterLayout;
import com.vaadin.flow.router.RouterLink;
import net.publicworks.app.backend.entity.User;
import net.publicworks.app.frontend.CurrentUser;


public class MainLayout extends AppLayout implements RouterLayout {
    public MainLayout() {
        createHeader();
        createDrawer();
        setPrimarySection(Section.DRAWER); // mobile: drawer is primary
    }

    private void createHeader() {
        DrawerToggle toggle = new DrawerToggle();

        H1 title = new H1("Public Works");
        title.getStyle()
                .set("font-size", "1.25rem")
                .set("margin", "0");

        User user = CurrentUser.get();
        String userLabel = user != null
                ? user.getEmail() + " (" + user.getRole() + ")"
                : "Not signed in";

        Span userSpan = new Span(userLabel);
        userSpan.getStyle().set("font-size", "0.875rem");

        Button logout = new Button("Logout", new Icon(VaadinIcon.SIGN_OUT));
        logout.getStyle().set("font-size", "0.75rem");
        logout.addClickListener(e -> {
            CurrentUser.clear();
            UI.getCurrent().navigate("");
        });

        HorizontalLayout userBox = new HorizontalLayout(userSpan, logout);
        userBox.setSpacing(true);
        userBox.setAlignItems(FlexComponent.Alignment.CENTER);

        HorizontalLayout header = new HorizontalLayout(toggle, title, userBox);
        header.setWidthFull();
        header.setPadding(true);
        header.setSpacing(true);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.expand(title); // title takes remaining space

        addToNavbar(header);
    }

    private void createDrawer() {
        VerticalLayout nav = new VerticalLayout();
        nav.setPadding(false);
        nav.setSpacing(false);
        nav.setSizeFull();

        nav.getStyle().set("padding-top", "var(--lumo-space-m)");
        Span sectionTitle = new Span("Navigation");
        sectionTitle.getStyle()
                .set("font-size", "0.95rem")
                .set("font-weight", "600")
                .set("text-transform", "uppercase")
                .set("opacity", "0.7")
                .set("padding-inline", "var(--lumo-space-m)");
        sectionTitle.getStyle().set("margin-bottom", "var(--lumo-space-m)");

        Div divider = new Div();
        divider.getStyle()
              .set("height", "1px")
              .set("background-color", "var(--lumo-contrast-20pct)")
              .set("margin", "var(--lumo-space-s) 0");

        RouterLink dashboard = new RouterLink("Dashboard", HomeView.class);
        dashboard.addComponentAsFirst(new Icon(VaadinIcon.DASHBOARD));

        // Placeholder for future sections
        RouterLink workOrders = new RouterLink("Work Orders", HomeView.class);
        workOrders.addComponentAsFirst(new Icon(VaadinIcon.CLIPBOARD_CHECK));

        RouterLink assets = new RouterLink("Assets", HomeView.class);
        assets.addComponentAsFirst(new Icon(VaadinIcon.FACTORY));

        nav.add(sectionTitle, divider, dashboard, workOrders, assets);

        addToDrawer(nav);
    }

}
