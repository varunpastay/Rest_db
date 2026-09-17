package com.restro.security;

import com.restro.entity.Owner;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Plain wrapper built by hand in OwnerUserDetailsService (not a Spring bean) - Owner is set via constructor, not @Autowired. */
@Getter
public class OwnerUserDetails implements UserDetails {

    private final Owner owner;

    public OwnerUserDetails(Owner owner) {
        this.owner = owner;
    }

    public Integer getOwnerId() {
        return owner.getOwnerId();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_OWNER"));
    }

    @Override
    public String getPassword() {
        return owner.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return owner.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return owner.isActive(); }
}
