package net.publicworks.app.frontend;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.login.LoginOverlay;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.component.UI;
import net.publicworks.app.backend.commands.CommandBus;
import net.publicworks.app.backend.commands.LoginCommand;
import net.publicworks.app.backend.commands.RegisterCommand;
import net.publicworks.app.backend.itf.IResult;
import net.publicworks.app.backend.service.AuthService;
import net.publicworks.app.backend.itf.Result;
import net.publicworks.app.backend.entity.User;
import net.publicworks.app.backend.service.IAuthService;

@Route("")           // root
@PageTitle("Sign in | Public Works")
public class AuthView extends VerticalLayout {

    private final CommandBus commandBus;

    public AuthView(CommandBus commandBus) {
        this.commandBus = commandBus;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        H1 title = new H1("Public Works Portal");

//        Tabs tabs = new Tabs(new Tab("Login"), new Tab("Register"));
//        tabs.setWidthFull();

        Component loginForm = createLoginForm();
        loginForm.setId("login-form");
        Component registerForm = createRegisterForm();

        registerForm.setVisible(false);
        loginForm.setVisible(true);

//        tabs.addSelectedChangeListener(e -> {
//            boolean loginSelected = e.getSelectedTab().getLabel().equals("Login");
//            loginForm.setVisible(loginSelected);
//            registerForm.setVisible(!loginSelected);
//        });

        add(title, loginForm, registerForm);
    }

    private Component createLoginForm() {
        LoginOverlay loginOverlay = new LoginOverlay();
        loginOverlay.setTitle("Public Works Portal");
        loginOverlay.setDescription("Please log in to continue.");
//        add(loginOverlay);
        loginOverlay.setOpened(true);
        loginOverlay.getElement().setAttribute("no-autofocus", "");

        addClassName("login-rich-context");
//        LoginForm form = new LoginForm();
        loginOverlay.getElement().getThemeList().add("dark");
//        form.setForgotPasswordButtonVisible(false);

        loginOverlay.addLoginListener(event -> {
            IResult result = commandBus.dispatch(
                    new LoginCommand(event.getUsername(), event.getPassword())
            );

            if (result.isSuccess()) {
                // TODO: store user/tenant in session if you want
                CurrentUser.set(result.getData());
                UI.getCurrent().navigate("app"); // go to MainView
            } else {
                loginOverlay.setError(true);
            }
        });

        return loginOverlay;
    }

    private Component createRegisterForm() {
        FormLayout layout = new FormLayout();

        TextField email = new TextField("Email");
        PasswordField password = new PasswordField("Password");
        TextField tenant = new TextField("City / Organization");
        Button register = new Button("Create Account");
        Span status = new Span();

        register.addClickListener(e -> {
            IResult result = commandBus.dispatch(
                    new RegisterCommand(
                            email.getValue(),
                            password.getValue(),
                            tenant.getValue()
                    )
            );

            if (result.isSuccess()) {
                status.getStyle().set("color", "green");
                status.setText("Account created. You can now log in.");
            } else {
                status.getStyle().set("color", "red");
                status.setText(result.getMessage());
            }
        });

        layout.add(email, password, tenant, register, status);
        layout.setMaxWidth("400px");
        return layout;
    }
}
