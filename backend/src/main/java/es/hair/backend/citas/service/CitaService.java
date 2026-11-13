package es.hair.backend.citas.service;

import es.hair.backend.citas.config.HorarioProperties;
import es.hair.backend.citas.dao.CitaDao;
import es.hair.backend.citas.dto.CitaAdminResponse;
import es.hair.backend.citas.dto.CitaResponse;
import es.hair.backend.citas.dto.CrearCitaAdminRequest;
import es.hair.backend.citas.dto.CrearCitaRequest;
import es.hair.backend.citas.exception.CancelacionBloqueoInconsistenteException;
import es.hair.backend.citas.exception.CitaAjenaException;
import es.hair.backend.citas.exception.CitaEnPasadoException;
import es.hair.backend.citas.exception.CitaFuturaExistenteException;
import es.hair.backend.citas.exception.CitaNoCancelableException;
import es.hair.backend.citas.exception.CitaNoEncontradaException;
import es.hair.backend.citas.exception.DependenciaCitasNoConfiguradaException;
import es.hair.backend.citas.exception.HuecoNoDisponibleException;
import es.hair.backend.citas.exception.UsuarioDestinoNoValidoException;
import es.hair.backend.citas.integration.DisponibilidadConsulta;
import es.hair.backend.citas.integration.UsuarioConsulta;
import es.hair.backend.citas.integration.UsuarioConsulta.DatosClienteCita;
import es.hair.backend.citas.model.Cita;
import es.hair.backend.citas.model.EstadoCita;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CitaService {

    private static final int DURACION_CITA_MINUTOS = 40;
    private static final LocalTime FIN_PLAZO_CANCELACION = LocalTime.of(23, 59, 59, 999_999_999);

    private final CitaDao citaDao;
    private final HorarioProperties horario;
    private final ObjectProvider<DisponibilidadConsulta> disponibilidadProvider;
    private final ObjectProvider<UsuarioConsulta> usuarioProvider;
    private final Clock clock;

    @Autowired
    public CitaService(
            CitaDao citaDao,
            HorarioProperties horario,
            ObjectProvider<DisponibilidadConsulta> disponibilidadProvider,
            ObjectProvider<UsuarioConsulta> usuarioProvider
    ) {
        this(citaDao, horario, disponibilidadProvider, usuarioProvider,
                Clock.system(horario.getZonaHoraria()));
    }

    CitaService(
            CitaDao citaDao,
            HorarioProperties horario,
            ObjectProvider<DisponibilidadConsulta> disponibilidadProvider,
            ObjectProvider<UsuarioConsulta> usuarioProvider,
            Clock clock
    ) {
        this.citaDao = Objects.requireNonNull(citaDao);
        this.horario = Objects.requireNonNull(horario);
        this.disponibilidadProvider = Objects.requireNonNull(disponibilidadProvider);
        this.usuarioProvider = Objects.requireNonNull(usuarioProvider);
        this.clock = Objects.requireNonNull(clock);
    }

    @Transactional
    public CitaResponse crearParaCliente(Integer usuarioAutenticadoId, CrearCitaRequest solicitud) {
        Objects.requireNonNull(usuarioAutenticadoId, "El usuario autenticado es obligatorio");
        Objects.requireNonNull(solicitud, "La solicitud de cita es obligatoria");

        LocalDateTime ahora = ahoraLocal();
        LocalDateTime inicio = combinar(solicitud.fecha(), solicitud.horaInicio());
        validarInicioFuturo(inicio, ahora);
        validarHuecoDisponible(inicio);
        validarSinOtraCitaFutura(usuarioAutenticadoId, ahora, null);

        return crearCita(usuarioAutenticadoId, inicio);
    }

    @Transactional
    public CitaAdminResponse crearParaAdministrador(CrearCitaAdminRequest solicitud) {
        Objects.requireNonNull(solicitud, "La solicitud administrativa es obligatoria");

        UsuarioConsulta usuarios = usuarios();
        UsuarioConsulta.UsuarioActivoParaCita usuario = usuarios
                .obtenerUsuarioActivoParaCita(solicitud.usuarioId())
                .orElseThrow(() -> new UsuarioDestinoNoValidoException(solicitud.usuarioId()));

        LocalDateTime ahora = ahoraLocal();
        LocalDateTime inicio = combinar(solicitud.fecha(), solicitud.horaInicio());
        validarInicioFuturo(inicio, ahora);
        validarHuecoDisponible(inicio);
        validarSinOtraCitaFutura(usuario.id(), ahora, null);

        CitaResponse cita = crearCita(usuario.id(), inicio);
        return enriquecerParaAdministrador(cita, usuario.id());
    }

    @Transactional(readOnly = true)
    public List<CitaResponse> obtenerMias(Integer usuarioAutenticadoId) {
        Objects.requireNonNull(usuarioAutenticadoId, "El usuario autenticado es obligatorio");
        LocalDateTime ahora = ahoraLocal();
        return citaDao.buscarPorUsuario(usuarioAutenticadoId).stream()
                .map(cita -> convertir(cita, ahora))
                .toList();
    }

    @Transactional(readOnly = true)
    public CitaResponse obtenerPorIdParaCliente(Integer usuarioAutenticadoId, Integer citaId) {
        Objects.requireNonNull(usuarioAutenticadoId, "El usuario autenticado es obligatorio");
        Cita cita = buscarObligatoria(citaId);
        comprobarPropiedad(cita, usuarioAutenticadoId);
        return convertir(cita, ahoraLocal());
    }

    @Transactional(readOnly = true)
    public List<CitaAdminResponse> consultarPorFecha(LocalDate fecha, EstadoCita estado) {
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        List<Cita> citas = citaDao.buscarPorFechaYEstado(fecha, estado);
        Map<Integer, DatosClienteCita> clientes = cargarClientes(citas);
        LocalDateTime ahora = ahoraLocal();

        return citas.stream()
                .map(cita -> convertirParaAdministrador(cita, clientes, ahora))
                .toList();
    }

    @Transactional
    public CitaResponse cancelarPorCliente(Integer usuarioAutenticadoId, Integer citaId) {
        Objects.requireNonNull(usuarioAutenticadoId, "El usuario autenticado es obligatorio");
        Cita cita = buscarObligatoria(citaId);
        comprobarPropiedad(cita, usuarioAutenticadoId);
        comprobarConfirmada(cita);

        LocalDateTime ahora = ahoraLocal();
        LocalDateTime limite = calcularLimiteCancelacion(cita.inicio().toLocalDate());
        if (ahora.isAfter(limite)) {
            throw new CitaNoCancelableException(
                    "El plazo de cancelación para esta cita ha finalizado");
        }

        if (!citaDao.cancelarConfirmadaPorId(citaId)) {
            throw new CitaNoCancelableException(
                    "La cita ya no está confirmada o fue modificada");
        }
        return convertir(buscarObligatoria(citaId), ahoraLocal());
    }

    @Transactional
    public CitaResponse cancelarPorAdministrador(Integer citaId) {
        Cita cita = buscarObligatoria(citaId);
        comprobarConfirmada(cita);
        if (!citaDao.cancelarConfirmadaPorId(citaId)) {
            throw new CitaNoCancelableException(
                    "La cita ya no está confirmada o fue modificada");
        }
        return convertir(buscarObligatoria(citaId), ahoraLocal());
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public List<CitaAdminCancelacion> cancelarPorBloqueo(LocalDateTime inicio, LocalDateTime fin) {
        validarIntervalo(inicio, fin);
        List<Cita> afectadas = citaDao.buscarSolapamientosConfirmados(inicio, fin);
        int actualizadas = citaDao.cancelarConfirmadasSolapadasEnIntervalo(inicio, fin);

        if (actualizadas != afectadas.size()) {
            throw new CancelacionBloqueoInconsistenteException(afectadas.size(), actualizadas);
        }

        return afectadas.stream()
                .map(cita -> new CitaAdminCancelacion(
                        cita.id(), cita.usuarioId(), cita.inicio(), cita.fin()))
                .toList();
    }

    private CitaResponse crearCita(Integer usuarioId, LocalDateTime inicio) {
        LocalDateTime fin = inicio.plusMinutes(DURACION_CITA_MINUTOS);
        Integer citaId = citaDao.insertar(new Cita(
                null,
                usuarioId,
                inicio,
                fin,
                EstadoCita.CONFIRMADA,
                null,
                null
        ));
        return convertir(new Cita(
                citaId,
                usuarioId,
                inicio,
                fin,
                EstadoCita.CONFIRMADA,
                null,
                null
        ), ahoraLocal());
    }

    private CitaAdminResponse enriquecerParaAdministrador(CitaResponse respuesta, Integer usuarioId) {
        Map<Integer, DatosClienteCita> clientes = cargarClientesPorIds(Set.of(usuarioId));
        DatosClienteCita cliente = clientes.get(usuarioId);
        if (cliente == null) {
            throw new UsuarioDestinoNoValidoException(usuarioId);
        }
        return new CitaAdminResponse(
                respuesta.id(),
                usuarioId,
                cliente.nombre(),
                cliente.apellidos(),
                cliente.telefono(),
                respuesta.inicio(),
                respuesta.fin(),
                respuesta.estado()
        );
    }

    private CitaAdminResponse convertirParaAdministrador(
            Cita cita,
            Map<Integer, DatosClienteCita> clientes,
            LocalDateTime ahora
    ) {
        DatosClienteCita cliente = clientes.get(cita.usuarioId());
        if (cliente == null) {
            throw new UsuarioDestinoNoValidoException(cita.usuarioId());
        }
        CitaResponse respuesta = convertir(cita, ahora);
        return new CitaAdminResponse(
                respuesta.id(),
                cita.usuarioId(),
                cliente.nombre(),
                cliente.apellidos(),
                cliente.telefono(),
                respuesta.inicio(),
                respuesta.fin(),
                respuesta.estado()
        );
    }

    private Map<Integer, DatosClienteCita> cargarClientes(List<Cita> citas) {
        Set<Integer> usuarioIds = citas.stream()
                .map(Cita::usuarioId)
                .collect(Collectors.toUnmodifiableSet());
        return cargarClientesPorIds(usuarioIds);
    }

    private Map<Integer, DatosClienteCita> cargarClientesPorIds(Set<Integer> usuarioIds) {
        if (usuarioIds.isEmpty()) {
            return Map.of();
        }
        return Map.copyOf(usuarios().obtenerDatosClientesPorIds(usuarioIds));
    }

    private Cita buscarObligatoria(Integer citaId) {
        Objects.requireNonNull(citaId, "El ID de la cita es obligatorio");
        return citaDao.buscarPorId(citaId).orElseThrow(
                () -> new CitaNoEncontradaException(citaId));
    }

    private void validarSinOtraCitaFutura(Integer usuarioId, LocalDateTime ahora,
                                           Integer excluirCitaId) {
        if (citaDao.existeCitaFuturaConfirmada(usuarioId, ahora, excluirCitaId)) {
            throw new CitaFuturaExistenteException(usuarioId);
        }
    }

    private void validarHuecoDisponible(LocalDateTime inicio) {
        if (!disponibilidad().estaDisponible(inicio)) {
            throw new HuecoNoDisponibleException(inicio);
        }
    }

    private void validarInicioFuturo(LocalDateTime inicio, LocalDateTime ahora) {
        if (!inicio.isAfter(ahora)) {
            throw new CitaEnPasadoException(inicio);
        }
    }

    private void comprobarPropiedad(Cita cita, Integer usuarioAutenticadoId) {
        if (!cita.usuarioId().equals(usuarioAutenticadoId)) {
            throw new CitaAjenaException();
        }
    }

    private void comprobarConfirmada(Cita cita) {
        if (cita.estado() != EstadoCita.CONFIRMADA) {
            throw new CitaNoCancelableException("La cita no está confirmada");
        }
    }

    private CitaResponse convertir(Cita cita, LocalDateTime ahora) {
        ZoneId zoneId = horario.getZonaHoraria();
        LocalDateTime limiteLocal = calcularLimiteCancelacion(cita.inicio().toLocalDate());
        return new CitaResponse(
                cita.id(),
                cita.inicio().atZone(zoneId).toOffsetDateTime(),
                cita.fin().atZone(zoneId).toOffsetDateTime(),
                cita.estado(),
                cita.estado() == EstadoCita.CONFIRMADA && !ahora.isAfter(limiteLocal),
                limiteLocal.atZone(zoneId).toOffsetDateTime()
        );
    }

    private LocalDateTime calcularLimiteCancelacion(LocalDate fechaCita) {
        return fechaCita.minusDays(2).atTime(FIN_PLAZO_CANCELACION);
    }

    private LocalDateTime combinar(LocalDate fecha, LocalTime hora) {
        Objects.requireNonNull(fecha, "La fecha es obligatoria");
        Objects.requireNonNull(hora, "La hora de inicio es obligatoria");
        return LocalDateTime.of(fecha, hora);
    }

    private LocalDateTime ahoraLocal() {
        return LocalDateTime.now(clock);
    }

    private void validarIntervalo(LocalDateTime inicio, LocalDateTime fin) {
        Objects.requireNonNull(inicio, "El inicio del intervalo es obligatorio");
        Objects.requireNonNull(fin, "El fin del intervalo es obligatorio");
        if (!inicio.isBefore(fin)) {
            throw new IllegalArgumentException("El intervalo debe cumplir inicio < fin");
        }
    }

    private DisponibilidadConsulta disponibilidad() {
        return Optional.ofNullable(disponibilidadProvider.getIfAvailable())
                .orElseThrow(() -> new DependenciaCitasNoConfiguradaException(
                        DisponibilidadConsulta.class.getSimpleName()));
    }

    private UsuarioConsulta usuarios() {
        return Optional.ofNullable(usuarioProvider.getIfAvailable())
                .orElseThrow(() -> new DependenciaCitasNoConfiguradaException(
                        UsuarioConsulta.class.getSimpleName()));
    }

    public record CitaAdminCancelacion(
            Integer citaId,
            Integer usuarioId,
            LocalDateTime inicio,
            LocalDateTime fin
    ) {
    }
}
