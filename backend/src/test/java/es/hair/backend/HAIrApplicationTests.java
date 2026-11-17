package es.hair.backend;

import es.hair.backend.auth.Rol;
import es.hair.backend.auth.Usuario;
import es.hair.backend.auth.security.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class HAIrApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void contextLoads() {
    }

    @Test
    void usuarioAdminPasaAutorizacionAunqueRutaNoExista() throws Exception {
        String token = jwtTokenService.emitir(usuario(Rol.ADMIN));

        mockMvc.perform(get("/api/admin/prueba")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void usuarioClienteNoPuedeAccederAAdministracion() throws Exception {
        String token = jwtTokenService.emitir(usuario(Rol.CLIENTE));

        mockMvc.perform(get("/api/admin/prueba")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    private Usuario usuario(Rol rol) {
        return new Usuario(1L, "Prueba", "Usuario", "prueba@example.com",
                null, null, null, rol, true, LocalDateTime.now());
    }
}
