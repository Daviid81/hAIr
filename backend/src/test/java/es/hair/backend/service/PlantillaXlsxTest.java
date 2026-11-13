package es.hair.backend.service;

import org.junit.jupiter.api.Test;
import es.hair.backend.exception.ArchivoInvalidoException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PlantillaXlsxTest {

    private final PlantillaXlsxGenerator generador = new PlantillaXlsxGenerator();
    private final PlantillaXlsxReader lector = new PlantillaXlsxReader();

    @Test
    void mesSinImportar_traeSoloFinesDeSemanaBloqueados() {
        YearMonth ym = YearMonth.of(2026, 12);
        var res = lector.leer(generador.generar(ym, Set.of(), false));

        assertEquals(ym, res.mes());
        assertTrue(res.errores().isEmpty());
        assertTrue(res.diasBloqueados().stream()
                .allMatch(d -> d.getDayOfWeek().getValue() >= 6));
        assertTrue(res.diasBloqueados().contains(LocalDate.of(2026, 12, 5))); // sábado
        assertFalse(res.diasBloqueados().contains(LocalDate.of(2026, 12, 4))); // viernes
    }

    @Test
    void mesImportado_reflejaLosDiasDeLaBD() {
        YearMonth ym = YearMonth.of(2026, 12);
        Set<LocalDate> bd = Set.of(LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 25));
        var res = lector.leer(generador.generar(ym, bd, true));

        assertEquals(bd, res.diasBloqueados());
    }

    @Test
    void archivoQueNoEsXlsx_lanzaArchivoInvalido() {
        assertThrows(ArchivoInvalidoException.class,
                () -> lector.leer("esto no es un excel".getBytes()));
    }
}
