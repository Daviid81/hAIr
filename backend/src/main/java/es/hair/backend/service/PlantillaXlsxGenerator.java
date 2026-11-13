package es.hair.backend.service;

import es.hair.backend.exception.ErrorPersistenciaException;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddressList;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Set;

@Component
public class PlantillaXlsxGenerator {

    public static final String HOJA = "Calendario";
    public static final int FILA_MES = 0;
    public static final int FILA_ANIO = 1;
    public static final int FILA_CABECERA = 3;
    public static final int FILA_PRIMERA_SEMANA = 4;
    public static final int MAX_SEMANAS = 6;
    private static final String[] DIAS = {"Lun", "Mar", "Mié", "Jue", "Vie", "Sáb", "Dom"};

    /**
     * @param bloqueados   días bloqueados actualmente (solo se usa si el mes ya está importado)
     * @param mesImportado si es false, los sábados y domingos salen con X
     */
    public byte[] generar(YearMonth ym, Set<LocalDate> bloqueados, boolean mesImportado) {
        try (Workbook wb = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sh = wb.createSheet(HOJA);

            Font negrita = wb.createFont();
            negrita.setBold(true);

            CellStyle estCabecera = estilo(wb, negrita, null);
            CellStyle estDia = estilo(wb, negrita, null);
            CellStyle estMarca = estilo(wb, null, null);
            CellStyle estMarcaFinde = estilo(wb, null, IndexedColors.GREY_25_PERCENT);

            sh.createRow(FILA_MES).createCell(0).setCellValue("Mes");
            sh.getRow(FILA_MES).createCell(1).setCellValue(ym.getMonthValue());
            sh.createRow(FILA_ANIO).createCell(0).setCellValue("Año");
            sh.getRow(FILA_ANIO).createCell(1).setCellValue(ym.getYear());

            Row cab = sh.createRow(FILA_CABECERA);
            for (int c = 0; c < 7; c++) {
                Cell cell = cab.createCell(c);
                cell.setCellValue(DIAS[c]);
                cell.setCellStyle(estCabecera);
                sh.setColumnWidth(c, 12 * 256);
            }

            // Filas de la rejilla
            for (int s = 0; s < MAX_SEMANAS; s++) {
                sh.createRow(FILA_PRIMERA_SEMANA + s * 2);
                sh.createRow(FILA_PRIMERA_SEMANA + s * 2 + 1);
            }

            int offset = ym.atDay(1).getDayOfWeek().getValue() - 1;
            for (int d = 1; d <= ym.lengthOfMonth(); d++) {
                int pos = offset + d - 1;
                int semana = pos / 7;
                int col = pos % 7;
                LocalDate fecha = ym.atDay(d);

                Cell celdaDia = sh.getRow(FILA_PRIMERA_SEMANA + semana * 2).createCell(col);
                celdaDia.setCellValue(d);
                celdaDia.setCellStyle(estDia);

                boolean finde = col >= 5;
                boolean marcar = mesImportado ? bloqueados.contains(fecha) : finde;
                Cell celdaMarca = sh.getRow(FILA_PRIMERA_SEMANA + semana * 2 + 1).createCell(col);
                celdaMarca.setCellStyle(!mesImportado && finde ? estMarcaFinde : estMarca);
                if (marcar) celdaMarca.setCellValue("X");
            }

            // Validación de datos: las celdas de marca solo admiten X
            DataValidationHelper dvh = sh.getDataValidationHelper();
            for (int s = 0; s < MAX_SEMANAS; s++) {
                int filaMarca = FILA_PRIMERA_SEMANA + s * 2 + 1;
                DataValidation dv = dvh.createValidation(
                        dvh.createExplicitListConstraint(new String[]{"X"}),
                        new CellRangeAddressList(filaMarca, filaMarca, 0, 6));
                dv.setShowErrorBox(true);
                dv.setErrorStyle(DataValidation.ErrorStyle.STOP);
                dv.createErrorBox("Valor no válido", "Solo se permite una X (o dejar la celda vacía).");
                sh.addValidationData(dv);
            }

            crearHojaInstrucciones(wb);
            wb.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ErrorPersistenciaException("No se pudo generar la plantilla", e);
        }
    }

    private void crearHojaInstrucciones(Workbook wb) {
        Sheet sh = wb.createSheet("Instrucciones");
        String[] lineas = {
                "Cómo usar esta plantilla",
                "1. Pon una X en la fila de debajo del número de cualquier día que quieras cerrar.",
                "2. Borra la X de un día para abrirlo (por ejemplo, un fin de semana).",
                "3. Los sábados y domingos vienen cerrados por defecto en meses sin importar.",
                "4. No cambies el mes ni el año de la hoja «Calendario».",
                "5. Si cierras un día con citas, se cancelarán y se avisará por correo a los clientes.",
                "6. Los tramos horarios se bloquean desde el panel, no desde este archivo."
        };
        for (int i = 0; i < lineas.length; i++) {
            sh.createRow(i).createCell(0).setCellValue(lineas[i]);
        }
        sh.setColumnWidth(0, 90 * 256);
    }

    private CellStyle estilo(Workbook wb, Font fuente, IndexedColors fondo) {
        CellStyle st = wb.createCellStyle();
        st.setAlignment(HorizontalAlignment.CENTER);
        if (fuente != null) st.setFont(fuente);
        if (fondo != null) {
            st.setFillForegroundColor(fondo.getIndex());
            st.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        return st;
    }
}
