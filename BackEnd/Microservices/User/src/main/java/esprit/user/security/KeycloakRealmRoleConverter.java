package esprit.user.security;

import org.springframework.core.convert.converter.Converter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Rôles realm Keycloak ({@code realm_access.roles}) → {@code ROLE_*} pour Spring Security.
 */
public class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

    @Override
    @SuppressWarnings("unchecked")
    public Collection<GrantedAuthority> convert(Jwt jwt) {
        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
        if (realmAccess == null || realmAccess.get("roles") == null) {
            return Collections.emptyList();
        }
        Object rolesObj = realmAccess.get("roles");
        if (!(rolesObj instanceof List<?> raw)) {
            return Collections.emptyList();
        }
        List<String> roles = new ArrayList<>();
        for (Object o : raw) {
            if (o != null) {
                roles.add(o.toString());
            }
        }
        return roles.stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r))
                .collect(Collectors.toList());
    }
}
