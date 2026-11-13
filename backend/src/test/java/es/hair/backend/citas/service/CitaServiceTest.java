package es.hair.backend.citas.service;

import es.hair.backend.citas.config.HorarioProperties;
import es.hair.backend.citas.dao.CitaDao;
import es.hair.backend.citas.dto.CitaResponse;
import es.hair.backend.citas.dto.CrearCitaAdminRequest;
import es.hair.backend.citas.dto.CrearCitaRequest;
import es.hair.backend.citas.exception.CitaAjenaException;
import es.hair.backend.citas.exception.CitaEnPasadoException;
import es.hair.backend.citas.exception.CitaFuturaExistenteException;
import es.hair.backend.citas.exception.CitaNoCancelableException;
import es.hair.backend.citas.exception.HuecoNoDisponibleException;
import es.hair.backend.citas.exception.UsuarioDestinoNoValidoException;
import es.hair.backend.citas.integration.DisponibilidadConsulta;
import es.hair.backend.citas.integration.UsuarioConsulta;
import es.hair.backend.citas.model.Cita;
import es.hair.backend.citas.model.EstadoCita;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.mockito.ArgumentCaptor;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CitaServiceTest {

    private static final Integer USER_ID = 17;
    private static final Integer OTHER_USER_ID = 23;
    private static final ZoneId MADRID = ZoneId.of("Europe/Madrid");
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 11, 13, 10, 0);
    private static final LocalDateTime INICIO_FUTURO = LocalDateTime.of(2026, 11, 20, 10, 0);

    private CitaDao citaDao;
    private DisponibilidadConsulta disponibilidad;
    private UsuarioConsulta usuarios;
    private CitaService citaService;

    @BeforeEach
    void prepararServicio() {
        citaDao = mock(CitaDao.class);
        disponibilidad = mock(DisponibilidadConsulta.class);
        usuarios = mock(UsuarioConsulta.class);

        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("disponibilidad", disponibilidad);
        beanFactory.addBean("usuarios", usuarios);

        HorarioProperties horario = new HorarioProperties();
        Clock clock = Clock.fixed(Instant.parse("2026-11-13T09:00:00Z"), MADRID);
        citaService = new CitaService(
                citaDao,
                horario,
                beanFactory.getBeanProvider(DisponibilidadConsulta.class),
                beanFactory.getBeanProvider(UsuarioConsulta.class),
                clock
        );
    }

    @Test
    void creaReservaValidaConDuracionDeCuarentaMinutosYZonaDeMadrid() {
        when(disponibilidad.estaDisponible(INICIO_FUTURO)).thenReturn(true);
        when(citaDao.existeCitaFuturaConfirmada(USER_ID, AHORA, null)).thenReturn(false);
        when(citaDao.insertar(any(Cita.class))).thenReturn(51);

        CitaResponse respuesta = citaService.crearParaCliente(
                USER_ID, solicitud(INICIO_FUTURO));

        assertEquals(51, respuesta.id());
        assertEquals(INICIO_FUTURO.atZone(MADRID).toOffsetDateTime(), respuesta.inicio());
        assertEquals(INICIO_FUTURO.plusMinutes(40).atZone(MADRID).toOffsetDateTime(),
                respuesta.fin());
        assertEquals(EstadoCita.CONFIRMADA, respuesta.estado());
        assertTrue(respuesta.cancelable());
        ArgumentCaptor<Cita> captor = ArgumentCaptor.forClass(Cita.class);
        verify(citaDao).insertar(captor.capture());
        assertEquals(USER_ID, captor.getValue().usuarioId());
        assertEquals(INICIO_FUTURO, captor.getValue().inicio());
        assertEquals(INICIO_FUTURO.plusMinutes(40), captor.getValue().fin());
        assertEquals(EstadoCita.CONFIRMADA, captor.getValue().estado());
    }

    @Test
    void rechazaUnaReservaEnElPasadoSinConsultarDisponibilidad() {
        LocalDateTime pasada = AHORA.minusMinutes(1);

        assertThrows(CitaEnPasadoException.class,
                () -> citaService.crearParaCliente(USER_ID, solicitud(pasada)));

        verify(disponibilidad, never()).estaDisponible(any());
        verify(citaDao, never()).insertar(any());
    }

    @Test
    void reservaHuecoDisponibleQueAntesOcupabaUnaCitaCancelada() {
        when(disponibilidad.estaDisponible(INICIO_FUTURO)).thenReturn(true);
        when(citaDao.existeCitaFuturaConfirmada(USER_ID, AHORA, null)).thenReturn(false);
        when(citaDao.insertar(any(Cita.class))).thenReturn(54);

        CitaResponse respuesta = citaService.crearParaCliente(
                USER_ID, solicitud(INICIO_FUTURO));

        assertEquals(54, respuesta.id());
        assertEquals(EstadoCita.CONFIRMADA, respuesta.estado());
        verify(disponibilidad).estaDisponible(INICIO_FUTURO);
        verify(citaDao).existeCitaFuturaConfirmada(USER_ID, AHORA, null);
        verify(citaDao).insertar(any(Cita.class));
    }

    @Test
    void rechazaUnInicioQueNoPerteneceAHuecoDisponible() {
        when(disponibilidad.estaDisponible(INICIO_FUTURO.plusMinutes(10))).thenReturn(false);

        assertThrows(HuecoNoDisponibleException.class, () -> citaService.crearParaCliente(
                USER_ID, solicitud(INICIO_FUTURO.plusMinutes(10))));

        verify(citaDao, never()).insertar(any());
    }

    @Test
    void rechazaUnHuecoOcupadoSegunElContratoDeDisponibilidad() {
        when(disponibilidad.estaDisponible(INICIO_FUTURO)).thenReturn(false);

        assertThrows(HuecoNoDisponibleException.class,
                () -> citaService.crearParaCliente(USER_ID, solicitud(INICIO_FUTURO)));

        verify(citaDao, never()).insertar(any());
    }

    @Test
    void rechazaClienteConOtraCitaFuturaConfirmada() {
        when(disponibilidad.estaDisponible(INICIO_FUTURO)).thenReturn(true);
        when(citaDao.existeCitaFuturaConfirmada(USER_ID, AHORA, null)).thenReturn(true);

        assertThrows(CitaFuturaExistenteException.class,
                () -> citaService.crearParaCliente(USER_ID, solicitud(INICIO_FUTURO)));

        verify(citaDao, never()).insertar(any());
    }

    @Test
    void clientePuedeCancelarSuCitaDentroDelPlazo() {
        Cita confirmada = cita(101, USER_ID, INICIO_FUTURO, EstadoCita.CONFIRMADA);
        Cita cancelada = cita(101, USER_ID, INICIO_FUTURO, EstadoCita.CANCELADA);
        when(citaDao.buscarPorId(101)).thenReturn(Optional.of(confirmada), Optional.of(cancelada));
        when(citaDao.cancelarConfirmadaPorId(101)).thenReturn(true);

        CitaResponse respuesta = citaService.cancelarPorCliente(USER_ID, 101);

        assertEquals(EstadoCita.CANCELADA, respuesta.estado());
        assertFalse(respuesta.cancelable());
        verify(citaDao).cancelarConfirmadaPorId(101);
    }

    @Test
    void clienteNoPuedeCancelarDesdeMedianocheLocalDelDiaAnteriorAlLimite() {
        LocalDateTime citaProxima = LocalDateTime.of(2026, 11, 20, 10, 0);
        LocalDateTime medianocheMadrid = LocalDateTime.of(2026, 11, 19, 0, 0);
        Clock despuesDelLimite = Clock.fixed(
                medianocheMadrid.atZone(MADRID).toInstant(), MADRID);
        CitaService servicioDespuesDelLimite = servicioConClock(despuesDelLimite);
        when(citaDao.buscarPorId(101)).thenReturn(Optional.of(
                cita(101, USER_ID, citaProxima, EstadoCita.CONFIRMADA)));

        assertThrows(CitaNoCancelableException.class,
                () -> servicioDespuesDelLimite.cancelarPorCliente(USER_ID, 101));

        verify(citaDao, never()).cancelarConfirmadaPorId(any());
    }

    @Test
    void clientePuedeCancelarExactamenteHastaElFinalDelMinutoLimite() {
        LocalDateTime citaProxima = LocalDateTime.of(2026, 11, 20, 10, 0);
        LocalDateTime limite = LocalDateTime.of(2026, 11, 18, 23, 59, 59, 999_999_999);
        Clock enElLimite = Clock.fixed(limite.atZone(MADRID).toInstant(), MADRID);
        CitaService servicioEnElLimite = servicioConClock(enElLimite);
        Cita confirmada = cita(101, USER_ID, citaProxima, EstadoCita.CONFIRMADA);
        Cita cancelada = cita(101, USER_ID, citaProxima, EstadoCita.CANCELADA);
        when(citaDao.buscarPorId(101)).thenReturn(Optional.of(confirmada), Optional.of(cancelada));
        when(citaDao.cancelarConfirmadaPorId(101)).thenReturn(true);

        CitaResponse respuesta = servicioEnElLimite.cancelarPorCliente(USER_ID, 101);

        assertEquals(EstadoCita.CANCELADA, respuesta.estado());
        verify(citaDao).cancelarConfirmadaPorId(101);
    }

    @Test
    void clienteNoPuedeCancelarLaCitaDeOtraPersona() {
        when(citaDao.buscarPorId(101)).thenReturn(Optional.of(
                cita(101, OTHER_USER_ID, INICIO_FUTURO, EstadoCita.CONFIRMADA)));

        assertThrows(CitaAjenaException.class,
                () -> citaService.cancelarPorCliente(USER_ID, 101));

        verify(citaDao, never()).cancelarConfirmadaPorId(any());
    }

    @Test
    void clienteNoPuedeCancelarFueraDelPlazo() {
        LocalDateTime citaProxima = LocalDateTime.of(2026, 11, 20, 10, 0);
        Clock despuesDelLimite = Clock.fixed(
                Instant.parse("2026-11-19T00:00:00Z"), MADRID);
        CitaService servicioDespuesDelLimite = servicioConClock(despuesDelLimite);
        when(citaDao.buscarPorId(101)).thenReturn(Optional.of(
                cita(101, USER_ID, citaProxima, EstadoCita.CONFIRMADA)));

        assertThrows(CitaNoCancelableException.class,
                () -> servicioDespuesDelLimite.cancelarPorCliente(USER_ID, 101));

        verify(citaDao, never()).cancelarConfirmadaPorId(any());
    }

    @Test
    void cancelacionAdministrativaNoAplicaElLimiteDelCliente() {
        LocalDateTime citaProxima = LocalDateTime.of(2026, 11, 20, 10, 0);
        Cita confirmada = cita(101, USER_ID, citaProxima, EstadoCita.CONFIRMADA);
        Cita cancelada = cita(101, USER_ID, citaProxima, EstadoCita.CANCELADA);
        when(citaDao.buscarPorId(101)).thenReturn(Optional.of(confirmada), Optional.of(cancelada));
        when(citaDao.cancelarConfirmadaPorId(101)).thenReturn(true);

        CitaResponse respuesta = citaService.cancelarPorAdministrador(101);

        assertEquals(EstadoCita.CANCELADA, respuesta.estado());
        verify(citaDao).cancelarConfirmadaPorId(101);
    }

    @Test
    void calculaElLimiteDeCancelacionEnZonaEuropeMadrid() {
        Cita cita = cita(101, USER_ID, INICIO_FUTURO, EstadoCita.CONFIRMADA);
        when(citaDao.buscarPorId(101)).thenReturn(Optional.of(cita));

        CitaResponse respuesta = citaService.obtenerPorIdParaCliente(USER_ID, 101);

        assertEquals(
                LocalDateTime.of(2026, 11, 18, 23, 59, 59, 999_999_999)
                        .atZone(MADRID).toOffsetDateTime(),
                respuesta.limiteCancelacion());
        assertEquals(MADRID.getRules().getOffset(INICIO_FUTURO),
                respuesta.inicio().getOffset());
    }

    @Test
    void obtieneCitasPropiasYCalculaCancelabilidad() {
        Cita propia = cita(101, USER_ID, INICIO_FUTURO, EstadoCita.CONFIRMADA);
        when(citaDao.buscarPorUsuario(USER_ID)).thenReturn(List.of(propia));

        List<CitaResponse> respuestas = citaService.obtenerMias(USER_ID);

        assertEquals(1, respuestas.size());
        assertEquals(101, respuestas.getFirst().id());
        assertTrue(respuestas.getFirst().cancelable());
    }

    @Test
    void consultaAdministrativaEnriqueceClientesConUnaConsultaPorLote() {
        Cita propia = cita(101, USER_ID, INICIO_FUTURO, EstadoCita.CONFIRMADA);
        Cita otra = cita(102, OTHER_USER_ID, INICIO_FUTURO.plusHours(1),
                EstadoCita.CANCELADA);
        when(citaDao.buscarPorFechaYEstado(INICIO_FUTURO.toLocalDate(), null))
                .thenReturn(List.of(propia, otra));
        when(usuarios.obtenerDatosClientesPorIds(Set.of(USER_ID, OTHER_USER_ID)))
                .thenReturn(Map.of(
                        USER_ID, new UsuarioConsulta.DatosClienteCita(
                                USER_ID, "Ana", "Pérez", "600000001"),
                        OTHER_USER_ID, new UsuarioConsulta.DatosClienteCita(
                                OTHER_USER_ID, "Luis", "López", "600000002")));

        var respuestas = citaService.consultarPorFecha(INICIO_FUTURO.toLocalDate(), null);

        assertEquals(2, respuestas.size());
        assertEquals("Ana", respuestas.getFirst().nombre());
        assertEquals("Luis", respuestas.getLast().nombre());
        verify(usuarios).obtenerDatosClientesPorIds(Set.of(USER_ID, OTHER_USER_ID));
    }

    @Test
    void convierteFechasDeVeranoConElOffsetEuropeMadridCorrecto() {
        LocalDateTime inicioVerano = LocalDateTime.of(2026, 7, 10, 10, 0);
        Clock antesDelVerano = Clock.fixed(
                Instant.parse("2026-06-30T08:00:00Z"), MADRID);
        CitaService servicioEnVerano = servicioConClock(antesDelVerano);
        LocalDateTime ahoraEnVerano = LocalDateTime.of(2026, 6, 30, 10, 0);
        when(disponibilidad.estaDisponible(inicioVerano)).thenReturn(true);
        when(citaDao.existeCitaFuturaConfirmada(USER_ID, ahoraEnVerano, null)).thenReturn(false);
        when(citaDao.insertar(any(Cita.class))).thenReturn(53);

        CitaResponse respuesta = servicioEnVerano.crearParaCliente(
                USER_ID, solicitud(inicioVerano));

        assertEquals(2 * 60 * 60, respuesta.inicio().getOffset().getTotalSeconds());
        assertEquals(inicioVerano.atZone(MADRID).toOffsetDateTime(), respuesta.inicio());
    }

    @Test
    void reservaAdministrativaValidaUsuarioActivoYUsaDatosEnLote() {
        when(usuarios.obtenerUsuarioActivoParaCita(USER_ID))
                .thenReturn(Optional.of(new UsuarioConsulta.UsuarioActivoParaCita(USER_ID)));
        when(usuarios.obtenerDatosClientesPorIds(Set.of(USER_ID))).thenReturn(Map.of(
                USER_ID, new UsuarioConsulta.DatosClienteCita(
                        USER_ID, "Ana", "Pérez", "600000000")));
        when(disponibilidad.estaDisponible(INICIO_FUTURO)).thenReturn(true);
        when(citaDao.existeCitaFuturaConfirmada(USER_ID, AHORA, null)).thenReturn(false);
        when(citaDao.insertar(any(Cita.class))).thenReturn(52);

        var respuesta = citaService.crearParaAdministrador(
                new CrearCitaAdminRequest(USER_ID, INICIO_FUTURO.toLocalDate(),
                        INICIO_FUTURO.toLocalTime()));

        assertEquals("Ana", respuesta.nombre());
        assertEquals("Pérez", respuesta.apellidos());
        assertEquals("600000000", respuesta.telefono());
        verify(usuarios).obtenerDatosClientesPorIds(Set.of(USER_ID));
    }

    @Test
    void reservaAdministrativaRechazaUsuarioInexistenteOInactivo() {
        when(usuarios.obtenerUsuarioActivoParaCita(USER_ID)).thenReturn(Optional.empty());

        assertThrows(UsuarioDestinoNoValidoException.class,
                () -> citaService.crearParaAdministrador(
                        new CrearCitaAdminRequest(USER_ID, INICIO_FUTURO.toLocalDate(),
                                INICIO_FUTURO.toLocalTime())));

        verify(disponibilidad, never()).estaDisponible(any());
        verify(citaDao, never()).insertar(any());
    }

    @Test
    void cancelarPorBloqueoDevuelveSoloDatosMinimosYRespetaElIntervalo() {
        LocalDateTime inicio = INICIO_FUTURO;
        LocalDateTime fin = inicio.plusHours(1);
        Cita afectada = cita(103, USER_ID, inicio, EstadoCita.CONFIRMADA);
        when(citaDao.buscarSolapamientosConfirmados(inicio, fin)).thenReturn(List.of(afectada));
        when(citaDao.cancelarConfirmadasSolapadasEnIntervalo(inicio, fin)).thenReturn(1);

        List<CitaService.CitaAdminCancelacion> resultado =
                citaService.cancelarPorBloqueo(inicio, fin);

        assertEquals(1, resultado.size());
        assertEquals(new CitaService.CitaAdminCancelacion(
                103, USER_ID, inicio, inicio.plusMinutes(40)), resultado.getFirst());
        verify(citaDao).buscarSolapamientosConfirmados(inicio, fin);
        verify(citaDao).cancelarConfirmadasSolapadasEnIntervalo(inicio, fin);
    }

    private CitaService servicioConClock(Clock clock) {
        StaticListableBeanFactory beanFactory = new StaticListableBeanFactory();
        beanFactory.addBean("disponibilidad", disponibilidad);
        beanFactory.addBean("usuarios", usuarios);
        return new CitaService(
                citaDao,
                new HorarioProperties(),
                beanFactory.getBeanProvider(DisponibilidadConsulta.class),
                beanFactory.getBeanProvider(UsuarioConsulta.class),
                clock
        );
    }

    private CrearCitaRequest solicitud(LocalDateTime inicio) {
        return new CrearCitaRequest(inicio.toLocalDate(), inicio.toLocalTime());
    }

    private Cita cita(Integer id, Integer usuarioId, LocalDateTime inicio, EstadoCita estado) {
        return new Cita(id, usuarioId, inicio, inicio.plusMinutes(40), estado, AHORA, AHORA);
    }
}
