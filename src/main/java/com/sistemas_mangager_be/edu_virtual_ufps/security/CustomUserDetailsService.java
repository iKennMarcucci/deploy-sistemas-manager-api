package com.sistemas_mangager_be.edu_virtual_ufps.security;

import org.springframework.stereotype.Service;

import com.sistemas_mangager_be.edu_virtual_ufps.entities.Admin;
import com.sistemas_mangager_be.edu_virtual_ufps.repositories.AdminRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.ArrayList;
import java.util.Collection;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private AdminRepository adminRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Admin admin = adminRepository.findByEmail(username)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));

        // Verificar si la cuenta está activa
        if (admin.getActivo() == null || !admin.getActivo()) {
            throw new UsernameNotFoundException("La cuenta está inactiva. Contacte al administrador.");
        }

        // Crear una lista de autoridades basada en el rol del administrador
        Collection<GrantedAuthority> authorities = new ArrayList<>();
        if (admin.getEsSuperAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }

        return new CustomUserDetails(
                admin.getEmail(),
                admin.getPassword(),
                true, // Cuenta siempre habilitada ya que validamos activo manualmente
                true,
                true,
                true,
                authorities,
                admin.getPrimerNombre(),
                admin.getPrimerApellido(),
                admin.getEsSuperAdmin());
    }

    // Clase personalizada para extender UserDetails y incluir información adicional
    public static class CustomUserDetails extends User {
        private final String primerNombre;
        private final String primerApellido;
        private final Boolean esSuperAdmin;

        public CustomUserDetails(String username, String password, boolean enabled,
                boolean accountNonExpired, boolean credentialsNonExpired,
                boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities,
                String primerNombre, String primerApellido, Boolean esSuperAdmin) {
            super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
            this.primerNombre = primerNombre;
            this.primerApellido = primerApellido;
            this.esSuperAdmin = esSuperAdmin;
        }

        public String getPrimerNombre() {
            return primerNombre;
        }

        public String getPrimerApellido() {
            return primerApellido;
        }

        public Boolean getEsSuperAdmin() {
            return esSuperAdmin;
        }
    }
}