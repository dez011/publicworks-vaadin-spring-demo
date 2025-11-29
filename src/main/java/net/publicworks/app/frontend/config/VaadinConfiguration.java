package net.publicworks.app.frontend.config;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.BodySize;
import com.vaadin.flow.spring.annotation.EnableVaadin;
import com.vaadin.flow.theme.Theme;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableVaadin
@Theme("app-theme")
@BodySize(height = "100%")
public class VaadinConfiguration implements AppShellConfigurator {
}
