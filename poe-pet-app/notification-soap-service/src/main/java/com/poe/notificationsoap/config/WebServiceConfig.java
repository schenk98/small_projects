package com.poe.notificationsoap.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.config.annotation.EnableWs;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/** SOAP servlet, schema, and WSDL exposure. */
@EnableWs
@Configuration
public class WebServiceConfig {
    @Bean
    ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext applicationContext) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(applicationContext);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean(name = "notifications")
    DefaultWsdl11Definition defaultWsdl11Definition(XsdSchema notificationSchema) {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
        definition.setPortTypeName("NotificationsPort");
        definition.setLocationUri("/ws");
        definition.setTargetNamespace("http://poe.com/notificationsoap");
        definition.setSchema(notificationSchema);
        return definition;
    }

    @Bean
    XsdSchema notificationSchema() {
        return new SimpleXsdSchema(new ClassPathResource("send-notification.xsd"));
    }
}
