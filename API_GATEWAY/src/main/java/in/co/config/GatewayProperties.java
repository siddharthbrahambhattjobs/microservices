package in.co.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

    private List<String> adminEmails = List.of();

    public List<String> getAdminEmails() { return adminEmails; }
    public void setAdminEmails(List<String> adminEmails) { this.adminEmails = adminEmails; }
}