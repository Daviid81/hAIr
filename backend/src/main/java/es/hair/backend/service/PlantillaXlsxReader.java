package es.hair.backend.service;

import es.hair.backend.exception.ArchivoInvalidoException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static es.hair.backend.service.PlantillaXlsxGenerator.*;

@Component
public class PlantillaXlsxReader {

    public record ResultadoLectura(YearMonth mes, Set<LocalDate> diasBloqueados, List<String> errores) {}

    public ResultadoLectura leer(byte[] contenido) {
        try (Workbook wb = new XSSFWorkbook(new ByteArrayInputStream(contenido))) {
            Sheet sh = wb.getSheet(HOJA);
            if (sh == null) {
                throw new ArchivoInvalidoException("Falta la hoja «Calendario». Usa la plantilla descargada.");
            }
            int mes = leerEntero(sh, FILA_MES, "Mes");
            int anio = leerEntero(sh, FILA_ANIO, "Año");
            YearMonth ym;
            try {
                ym = YearMonth.of(anio, mes);
            } catch (DateTimeException e) {
                throw new ArchivoInvalidoException("El mes o el año de la plantilla no son válidos.");
            }

            Set<LocalDate> bloqueados = new TreeSet<>();
            List<String> errores = new ArrayList<>();

            for (int s = 0; s < MAX_SEMANAS; s++) {
                Row filaDias = sh.getRow(FILA_PRIMERA_SEMANA + s * 2);
                Row filaMarca = sh.getRow(FILA_PRIMERA_SEMANA + s * 2 + 1);
                for (int col = 0; col < 7; col++) {
                    Cell celdaDia = filaDias == null ? null : filaDias.getCell(col);
                    Cell celdaMarca = filaMarca == null ? null : filaMarca.getCell(col);
                    boolean marcado = esX(celdaMarca);

                    if (celdaDia == null || celdaDia.getCellType() != CellType.NUMERIC) {
                        if (marcado) errores.add("Hay una X en la semana " + (s + 1) + " sin día asociado.");
                        continue;
                    }
                    int dia = (int) celdaDia.getNumericCellValue();
                    if (dia < 1 || dia > ym.lengthOfMonth()) {
                        errores.add("Día " + dia + " fuera del mes en la semana " + (s + 1) + ".");
                        continue;
                    }
                    LocalDate fecha = ym.atDay(dia);
                    if (fecha.getDayOfWeek().getValue() - 1 != col) {
                        errores.add("El día " + dia + " no está en la columna que le corresponde.");
                        continue;
                    }
                    if (marcado) bloqueados.add(fecha);
                    else if (celdaMarca != null && celdaMarca.getCellType() != CellType.BLANK
                            && celdaMarca.getCellType() != CellType.STRING) {
                        errores.add("Valor no válido en el día " + dia + " (solo se admite X).");
                    }
                }
            }
            return new ResultadoLectura(ym, bloqueados, errores);
        } catch (IOException | RuntimeException e) {
            if (e instanceof ArchivoInvalidoException a) throw a;
            throw new ArchivoInvalidoException("El archivo no es un .xlsx legible.");
        }
    }

    private int leerEntero(Sheet sh, int fila, String nombre) {
        Row row = sh.getRow(fila);
        Cell cell = row == null ? null : row.getCell(1);
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            throw new ArchivoInvalidoException("No se encuentra el valor de «" + nombre + "» en la plantilla.");
        }
        return (int) cell.getNumericCellValue();
    }

    private boolean esX(Cell cell) {
        return cell != null && cell.getCellType() == CellType.STRING
                && "X".equalsIgnoreCase(cell.getStringCellValue().trim());
    }
}
