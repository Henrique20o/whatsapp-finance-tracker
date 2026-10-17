package com.wa.finance.web.mobile;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class ClientIpResolver {

    private static final Pattern NUMERIC_IP = Pattern.compile("[0-9a-fA-F:.]{2,45}");

    public String resolver(HttpServletRequest request) {
        String remoteAddress = request.getRemoteAddr();
        if (remoteAddress == null
                || !NUMERIC_IP.matcher(remoteAddress).matches()
                || (!remoteAddress.contains(".") && !remoteAddress.contains(":"))) {
            throw new InvalidClientAddressException();
        }

        try {
            return InetAddress.getByName(remoteAddress).getHostAddress().toLowerCase(Locale.ROOT);
        } catch (UnknownHostException exception) {
            throw new InvalidClientAddressException();
        }
    }
}
