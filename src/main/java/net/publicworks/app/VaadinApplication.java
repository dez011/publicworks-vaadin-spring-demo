package net.publicworks.app;

import net.publicworks.app.backend.commands.RegisterCommand;
import net.publicworks.app.backend.itf.CustomerDiferentiator;
import net.publicworks.app.backend.itf.IAuthService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;

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
                // no valid admin with that email/pass, so (re)crno valid admin with that email/pass, so (re)create iteate it
                authService.register(RegisterCommand.builder()
                        .email("test")
                        .password("test")
                        .tenantName("test user")
                        .role("ADMIN")
                        .customerDiferentiator(CustomerDiferentiator.DEFAULT)
                        .build());

                System.out.println("✅ Seeded admin user: test / test");
            } else {
                System.out.println("ℹ️ Admin user already exists");
            }
        };
    }

}
