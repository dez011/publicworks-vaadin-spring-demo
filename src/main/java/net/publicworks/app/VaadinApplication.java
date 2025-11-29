package net.publicworks.app;

import net.publicworks.app.backend.service.AuthService;
import net.publicworks.app.backend.service.IAuthService;
import net.publicworks.app.frontend.config.VaadinConfiguration;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@EntityScan(basePackages = "net.publicworks.app")
//@EnableJpaRepositories(basePackages = "net.publicworks.app")
@ComponentScan(basePackages = "net.publicworks.app")
public class VaadinApplication {

    public static void main(String[] args) {
        SpringApplication.run(VaadinApplication.class, args);
    }

    @Bean
    CommandLineRunner initAdmin(IAuthService authService) {
        return args -> {
            var result = authService.login("admin@publicworks.local", "admin123!");

            if (!result.isSuccess()) {
                // no valid admin with that email/pass, so (re)create it
                authService.register(
                        "test",
                        "test",         // change this later
                        "test user"          // tenant/org name
                );
                System.out.println("✅ Seeded admin user: test / test");
            } else {
                System.out.println("ℹ️ Admin user already exists");
            }
        };
    }

}
