package net.publicworks.app.frontend.view;

import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.Key;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Route;
import net.publicworks.app.backend.service.HelloService;
import net.publicworks.app.frontend.layout.MainLayout;

@Route(value = "app.deprecated.demo", layout = MainLayout.class)
@SuppressWarnings("unused")
public class MainView extends Composite<Div> {

    public MainView(HelloService helloService) {
        // TextField setup
        TextField tf = new TextField();
        tf.setLabel("Please enter a name to greet");
        tf.setPlaceholder("World");
        tf.setId("name-input");
        tf.focus();

        // Button setup
        Button b = new Button("Greet!");
        b.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        b.setId("greet-button");
        b.addClickShortcut(Key.ENTER);
        b.addClickListener(event -> {
            getContent().add(new Div(new Text(helloService.sayHello(tf.getValue()))));
            tf.focus();
            tf.clear();
        });

        // Main content
        getContent().add(new H1("Greeting Service"));

        FormLayout formLayout = new FormLayout(tf, b);
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0px", 1)
        );

        getContent().add(formLayout);
    }
}
