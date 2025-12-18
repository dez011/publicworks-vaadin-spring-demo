// src/main/java/net/publicworks/app/frontend/view/HomeView.java
package net.publicworks.app.frontend.layout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import net.publicworks.app.backend.commands.CommandBus;
import net.publicworks.app.backend.commands.CreateWorkOrderCommand;
import net.publicworks.app.backend.commands.CrudOperation;
import net.publicworks.app.backend.entity.User;
import net.publicworks.app.backend.entity.WorkOrder;
import net.publicworks.app.backend.handlers.ResultUtil;
import net.publicworks.app.backend.itf.IResult;
import net.publicworks.app.backend.itf.Result;
import net.publicworks.app.frontend.CurrentUser;

@Route(value = "app", layout = MainLayout.class)
@PageTitle("Dashboard | Public Works")
public class HomeView extends VerticalLayout implements BeforeEnterObserver {

    private final CommandBus commandBus;

    public HomeView(CommandBus commandBus) {
        this.commandBus = commandBus;
        setSizeFull();
        setPadding(true);
        setSpacing(true);
        setDefaultHorizontalComponentAlignment(FlexComponent.Alignment.STRETCH);

        User user = CurrentUser.get();

        // Greeting and user context
        String greeting = (user != null)
                ? "Welcome, " + user.getEmail()
                : "Welcome";

        String tenant = (user != null && user.getTenantId() != null)
                ? "User: " + user.getTenantId()
                : "No user configured yet";

        H2 title = new H2(greeting);
        Paragraph subtitle = new Paragraph(tenant);

        add(title, subtitle);

        // Search bar for quick lookups
        TextField searchField = new TextField();
        searchField.setPlaceholder("Search assets, work orders, requests…");
        searchField.setWidthFull();
        searchField.setClearButtonVisible(true);

        // Quick action buttons
        Button newWorkOrderBtn = new Button("New Work Order", new Icon(VaadinIcon.PLUS_CIRCLE));
        Button newServiceRequestBtn = new Button("New Request", new Icon(VaadinIcon.ENVELOPE));
        Button viewMapBtn = new Button("Open Map", new Icon(VaadinIcon.GLOBE));

        // wire the buttons to navigate to their respective views
        newWorkOrderBtn.addClickListener(e -> openNewWorkOrderDialog());
        newServiceRequestBtn.addClickListener(e ->
                UI.getCurrent().navigate("requests"));          // replace with your route
        viewMapBtn.addClickListener(e ->
                UI.getCurrent().navigate(String.valueOf(MapView.class)));       // replace with your map view class

        HorizontalLayout actions = new HorizontalLayout(newWorkOrderBtn, newServiceRequestBtn, viewMapBtn);
        actions.setSpacing(true);
        // enable wrapping for actions on mobile
        actions.getStyle().set("flex-wrap", "wrap");

        add(searchField, actions);

        // Responsive cards layout (mobile-first)
        HorizontalLayout cards = new HorizontalLayout();
        cards.setWidthFull();
        cards.getStyle().set("flex-wrap", "wrap");
        cards.setSpacing(true);

        // Dashboard cards summarising important features
        cards.add(
                createCard("Open Work Orders",
                        "Track and assign field work for today."),
                createCard("Service Requests",
                        "View, create and manage citizen service requests."),
                createCard("Assets",
                        "Access inventory of mains, valves, hydrants and other critical infrastructure."),
                createCard("Reports",
                        "Generate activity and compliance reports."),
                createCard("Map",
                        "View GIS map overlay of assets and ongoing work.")
        );

        add(cards);
        expand(cards);

        // Placeholder for GIS map integration
        Div mapPlaceholder = new Div();
        mapPlaceholder.setWidthFull();
        mapPlaceholder.setHeight("300px");
        mapPlaceholder.getStyle().set("border", "1px solid var(--lumo-contrast-10pct)")
                .set("margin-top", "1rem")
                .set("border-radius", "8px")
                .set("display", "flex")
                .set("align-items", "center")
                .set("justify-content", "center");
        mapPlaceholder.setText("GIS Map Integration Coming Soon");
        add(mapPlaceholder);
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

    private void openNewWorkOrderDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create Work Order");

        // Core fields (required)
        TextField titleField = new TextField("Title");
        titleField.setRequiredIndicatorVisible(true);
        titleField.getElement().setAttribute("data-testid", "wo-title");

        TextArea descriptionField = new TextArea("Description");
        descriptionField.setMinHeight("150px");
        descriptionField.getElement().setAttribute("data-testid", "wo-description");

        // Common “customer” fields
        TextField requestedByField = new TextField("Requested by");
        requestedByField.setRequiredIndicatorVisible(true);
        requestedByField.getElement().setAttribute("data-testid", "wo-requestedBy");

        TextField contactField = new TextField("Contact info (email)");
        contactField.getElement().setAttribute("data-testid", "wo-contact");
        TextField phoneField = new TextField("Contact info (phone)");
        phoneField.getElement().setAttribute("data-testid", "wo-phone");
        TextField locationField = new TextField("Location / Asset");
        locationField.getElement().setAttribute("data-testid", "wo-location");

        ComboBox<String> priorityField = new ComboBox<>("Priority");
        priorityField.setItems("Low", "Normal", "High", "Emergency");
        priorityField.setValue("Normal");

        // Layout
        FormLayout formLayout = new FormLayout();
        formLayout.add(
                titleField,
                descriptionField,
                requestedByField,
                contactField,
                phoneField,
                locationField,
                priorityField
        );
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        // Action buttons
        Button save = new Button("Save", e -> {
            // Basic validation of required fields
            boolean hasError = false;

            if (titleField.getValue() == null || titleField.getValue().trim().isEmpty()) {
                titleField.setInvalid(true);
                titleField.setErrorMessage("Title is required");
                hasError = true;
            } else {
                titleField.setInvalid(false);
            }

            if (requestedByField.getValue() == null || requestedByField.getValue().trim().isEmpty()) {
                requestedByField.setInvalid(true);
                requestedByField.setErrorMessage("Requested by is required");
                hasError = true;
            } else {
                requestedByField.setInvalid(false);
            }

            if (hasError) {
                return;
            }

            String title = titleField.getValue().trim();
            String description = descriptionField.getValue() != null ? descriptionField.getValue().trim() : "";
            String requestedBy = requestedByField.getValue().trim();
            String email = contactField.getValue() != null ? contactField.getValue().trim() : "";
            String phone = phoneField.getValue() != null ? phoneField.getValue().trim() : "";
            String location = locationField.getValue() != null ? locationField.getValue().trim() : "";
            String priority = priorityField.getValue();

            // TODO: Dispatch to backend to actually create and persist a WorkOrder entity
            var user = CurrentUser.get();
            String tenantId = user != null ? user.getTenantId() : null;

            var cmd = CreateWorkOrderCommand.builder()
                    .tenantId(tenantId)
                    .title(title)
                    .description(description)
                    .requestedBy(requestedBy)
                    .requesterContact(email)
                    .location(location)
                    .priority(priority)
                    .requesterPhone(phone)
                    .operation(CrudOperation.CREATE)
                    .customerDiferentiator(CurrentUser.getCustomerDiferentiator())
                    .build();

            IResult result = commandBus.dispatch(cmd);

            // Show a confirmation banner with actions
            WorkOrder saved = ResultUtil.require(result, WorkOrder.class);

            Result.UiBanner banner = ResultUtil.require(result, Result.UiBanner.class);
            BannerUtil.show(banner);

            dialog.close();
        });

        save.getElement().setAttribute("data-testid", "wo-save");

        Button cancel = new Button("Cancel", e -> dialog.close());

        HorizontalLayout actions = new HorizontalLayout(save, cancel);
        actions.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        actions.setWidthFull();

        dialog.add(formLayout, actions);
        dialog.open();
    }

}
