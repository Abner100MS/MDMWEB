package service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import repository.StockRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class StockService {

    // =====================================================
    // REPOSITORY
    // =====================================================

    @Autowired
    private StockRepository stockRepository;

    // =====================================================
    // GENERAR REPORTE EXCEL DE STOCK
    // =====================================================

    public byte[] obtenerReporteStock(
            String buscar,
            String filtro,
            String columnas) throws IOException {

        // =====================================================
        // OBTENER TODOS LOS EQUIPOS DE STOCK
        // =====================================================

        List<Map<String, Object>> datos = stockRepository.listarStockCompleto();

        // =====================================================
        // PREPARAR FILTROS
        // =====================================================

        String textoBuscar = buscar == null
                ? ""
                : buscar.trim().toLowerCase();

        String filtroStock = filtro == null
                ? ""
                : filtro.trim();

        // =====================================================
        // FILTRAR DATOS
        // =====================================================

        List<Map<String, Object>> filtrados = new ArrayList<>();

        for (Map<String, Object> item : datos) {

            // =================================================
            // BUSCADOR
            // =================================================

            boolean coincideTexto = textoBuscar.isEmpty()

                    ||

                    valorStock(item, "activo")
                            .toLowerCase()
                            .contains(textoBuscar)

                    ||

                    valorStock(item, "modelo")
                            .toLowerCase()
                            .contains(textoBuscar)

                    ||

                    valorStock(item, "empleado")
                            .toLowerCase()
                            .contains(textoBuscar)

                    ||

                    valorStock(item, "area")
                            .toLowerCase()
                            .contains(textoBuscar);

            // =================================================
            // FILTRO DE STOCK
            // =================================================

            boolean coincideFiltro = true;

            // REPORTANDO
            if ("REPORTANDO".equals(filtroStock)) {

                Object reportando = item.get("reportando");

                coincideFiltro = Boolean.TRUE.equals(reportando);
            }

            // BUENO / CON FALLA
            else if (!filtroStock.isEmpty()) {

                coincideFiltro = filtroStock.equals(
                        valorStock(
                                item,
                                "condicion"));
            }

            // =================================================
            // AGREGAR SI CUMPLE LOS DOS FILTROS
            // =================================================

            if (coincideTexto && coincideFiltro) {

                filtrados.add(item);
            }
        }

        // =====================================================
        // COLUMNAS SELECCIONADAS
        // =====================================================

        Set<String> cols = new HashSet<>();

        if (columnas != null &&
                !columnas.isBlank()) {

            cols.addAll(
                    Arrays.asList(
                            columnas.split(",")));
        }

        // =====================================================
        // CREAR EXCEL
        // =====================================================

        Workbook workbook = new XSSFWorkbook();

        Sheet sheet = workbook.createSheet(
                "Stock");

        int fila = 0;

        int col = 0;

        // =====================================================
        // ENCABEZADOS
        // =====================================================

        Row encabezado = sheet.createRow(
                fila++);

        if (cols.contains("activo")) {

            encabezado
                    .createCell(col++)
                    .setCellValue("Activo");
        }

        if (cols.contains("equipo")) {

            encabezado
                    .createCell(col++)
                    .setCellValue("Equipo");
        }

        if (cols.contains("modelo")) {

            encabezado
                    .createCell(col++)
                    .setCellValue("Modelo");
        }

        if (cols.contains("codigo")) {

            encabezado
                    .createCell(col++)
                    .setCellValue("Código");
        }

        if (cols.contains("empleado")) {

            encabezado
                    .createCell(col++)
                    .setCellValue("Empleado");
        }

        if (cols.contains("planta")) {

            encabezado
                    .createCell(col++)
                    .setCellValue("Planta");
        }

        if (cols.contains("area")) {

            encabezado
                    .createCell(col++)
                    .setCellValue("Área");
        }

        if (cols.contains("departamento")) {

            encabezado
                    .createCell(col++)
                    .setCellValue("Departamento");
        }

        if (cols.contains("condicion")) {

            encabezado
                    .createCell(col++)
                    .setCellValue("Condición");
        }

        if (cols.contains("motivoIngreso")) {

            encabezado
                    .createCell(col++)
                    .setCellValue("Motivo de ingreso");
        }

        if (cols.contains("fechaIngreso")) {

            encabezado
                    .createCell(col++)
                    .setCellValue("Fecha de ingreso");
        }

        if (cols.contains("reportando")) {

            encabezado
                    .createCell(col++)
                    .setCellValue("Reportando");
        }

        // =====================================================
        // LLENAR FILAS
        // =====================================================

        for (Map<String, Object> item : filtrados) {

            Row row = sheet.createRow(
                    fila++);

            col = 0;

            // ACTIVO
            if (cols.contains("activo")) {

                row.createCell(col++)
                        .setCellValue(
                                valorStock(
                                        item,
                                        "activo"));
            }

            // EQUIPO
            if (cols.contains("equipo")) {

                row.createCell(col++)
                        .setCellValue(
                                valorStock(
                                        item,
                                        "equipo"));
            }

            // MODELO
            if (cols.contains("modelo")) {

                row.createCell(col++)
                        .setCellValue(
                                valorStock(
                                        item,
                                        "modelo"));
            }

            // CÓDIGO
            if (cols.contains("codigo")) {

                row.createCell(col++)
                        .setCellValue(
                                valorStock(
                                        item,
                                        "codigo"));
            }

            // EMPLEADO
            if (cols.contains("empleado")) {

                row.createCell(col++)
                        .setCellValue(
                                valorStock(
                                        item,
                                        "empleado"));
            }

            // PLANTA
            if (cols.contains("planta")) {

                row.createCell(col++)
                        .setCellValue(
                                valorStock(
                                        item,
                                        "planta"));
            }

            // ÁREA
            if (cols.contains("area")) {

                row.createCell(col++)
                        .setCellValue(
                                valorStock(
                                        item,
                                        "area"));
            }

            // DEPARTAMENTO
            if (cols.contains("departamento")) {

                row.createCell(col++)
                        .setCellValue(
                                valorStock(
                                        item,
                                        "departamento"));
            }

            // CONDICIÓN
            if (cols.contains("condicion")) {

                row.createCell(col++)
                        .setCellValue(
                                valorStock(
                                        item,
                                        "condicion"));
            }

            // MOTIVO
            if (cols.contains("motivoIngreso")) {

                row.createCell(col++)
                        .setCellValue(
                                valorStock(
                                        item,
                                        "motivoIngreso"));
            }

            // FECHA INGRESO
            if (cols.contains("fechaIngreso")) {

                row.createCell(col++)
                        .setCellValue(
                                valorStock(
                                        item,
                                        "fechaIngreso"));
            }

            // REPORTANDO
            if (cols.contains("reportando")) {

                Object reportando = item.get("reportando");

                boolean estaReportando = Boolean.TRUE.equals(
                        reportando);

                row.createCell(col++)
                        .setCellValue(
                                estaReportando
                                        ? "SÍ"
                                        : "NO");
            }
        }

        // =====================================================
        // AJUSTAR ANCHO DE COLUMNAS
        // =====================================================

        int cantidadColumnas = encabezado.getLastCellNum();

        for (int i = 0; i < cantidadColumnas; i++) {

            sheet.autoSizeColumn(i);
        }

        // =====================================================
        // GENERAR ARCHIVO
        // =====================================================

        ByteArrayOutputStream out = new ByteArrayOutputStream();

        workbook.write(out);

        workbook.close();

        return out.toByteArray();
    }

    // =====================================================
    // CONVERTIR VALOR DE STOCK A TEXTO
    // =====================================================

    private String valorStock(
            Map<String, Object> item,
            String campo) {

        Object valor = item.get(campo);

        if (valor == null) {

            return "";
        }

        return valor.toString();
    }
}