package com.finance.manager;

import com.finance.manager.ui.LoginFrame;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

import javax.swing.*;

/**
 * Spring Boot entry point for Tally.
 *
 * <p>
 * Starts both an embedded HTTP server for the REST API and the Swing desktop
 * client. Both adapters call the same transactional application services.
 */
@SpringBootApplication
public class FinanceManagerApplication {

    public static void main(String[] args) {
        System.setProperty("java.awt.headless", "false");

        ConfigurableApplicationContext ctx = new SpringApplicationBuilder(FinanceManagerApplication.class)
                .headless(false)
                .run(args);

        if (ctx.getEnvironment().getProperty("app.ui.enabled", Boolean.class, true)) {
            SwingUtilities.invokeLater(() -> ctx.getBean(LoginFrame.class).setVisible(true));
        }
    }
}
