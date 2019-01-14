package org.saiku.service.util.export.excel;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.olap4j.metadata.Measure;
import org.saiku.olap.dto.resultset.AbstractBaseCell;
import org.saiku.olap.dto.resultset.CellDataSet;
import org.saiku.olap.dto.resultset.DataCell;
import org.saiku.olap.dto.resultset.MemberCell;
import org.saiku.olap.query2.ThinHierarchy;
import org.saiku.olap.query2.ThinLevel;
import org.saiku.olap.query2.ThinMember;
import org.saiku.olap.util.SaikuProperties;
import org.saiku.service.olap.totals.TotalNode;
import org.saiku.service.olap.totals.aggregators.BlankAggregator;
import org.saiku.service.olap.totals.aggregators.TotalAggregator;
import org.saiku.service.util.exception.SaikuServiceException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;

/**
 * Created with IntelliJ IDEA. User: sramazzina Date: 21/06/12 Time: 7.35 To
 * change this template use File | Settings | File Templates.
 */

public class ExcelWorksheetBuilder {

    private static final Logger log = LogManager.getLogger(ExcelWorksheetBuilder.class);

    private static final String BASIC_SHEET_FONT_FAMILY = "Arial";
    private static final short BASIC_SHEET_FONT_SIZE = 11;
    private static final String EMPTY_STRING = "";
    private static final String CSS_COLORS_CODE_PROPERTIES = "css-colors-codes.properties";
    private static final int MAX_CHAR_CELL = 100;
    private static final int MIN_CHAR_CELL = 4;

    private int maxRows = -1;
    private int maxColumns = -1;

    private AbstractBaseCell[][] rowsetHeader;
    private AbstractBaseCell[][] rowsetBody;

    private Map<Integer, TotalAggregator[][]> rowScanTotals;
    private Map<Integer, String[]> rowScanTotalsCaptions;
    private Map<Integer, TotalAggregator[][]> colScanTotals;
    private Map<Integer, String[]> colScanTotalsCaptions;

    private CellDataSet table;
    private Workbook excelWorkbook;
    private Sheet workbookSheet;
    private String sheetName;
    private int topLeftCornerWidth;
    private int topLeftCornerHeight;
    private CellStyle basicCS;
    private CellStyle totalsCS;
    private CellStyle numberCS;
    private CellStyle lighterHeaderCellCS;
    private Font highLightFont;
    private Font highLightTotalsFont;
    private List<ThinHierarchy> queryFilters;
    private Map<String, Integer> colorCodesMap;

    private Map<Integer, Integer> columnWidth = new HashMap<>();

    private int nextAvailableColorCode = 41;
    private Properties cssColorCodesProperties;

    private HSSFPalette customColorsPalette;
    private ExcelBuilderOptions options;

    public ExcelWorksheetBuilder(CellDataSet table, List<ThinHierarchy> filters, ExcelBuilderOptions options) {
        init(table, filters, options);
    }

    private void init(CellDataSet table, List<ThinHierarchy> filters, ExcelBuilderOptions options) {
        this.table = table;
        this.options = options;
        queryFilters = filters;
        maxRows = SpreadsheetVersion.EXCEL2007.getMaxRows();
        maxColumns = SpreadsheetVersion.EXCEL2007.getMaxColumns();

        if ("xls".equals(SaikuProperties.webExportExcelFormat)) {
            HSSFWorkbook wb = new HSSFWorkbook();
            customColorsPalette = wb.getCustomPalette();
            excelWorkbook = wb;
            maxRows = SpreadsheetVersion.EXCEL97.getMaxRows();
            maxColumns = SpreadsheetVersion.EXCEL97.getMaxColumns();
        } else if ("xlsx".equals(SaikuProperties.webExportExcelFormat)) {
            excelWorkbook = new XSSFWorkbook();
        } else {
            excelWorkbook = new XSSFWorkbook();
        }

        colorCodesMap = new HashMap<>();
        this.sheetName = options.sheetName;
        rowsetHeader = table.getCellSetHeaders();
        rowsetBody = table.getCellSetBody();

        topLeftCornerWidth = findTopLeftCornerWidth();
        topLeftCornerHeight = findTopLeftCornerHeight();

        initCellStyles();

        // Row totals and subtotals
        rowScanTotals = new HashMap<>();
        rowScanTotalsCaptions = new HashMap<>();
        colScanTotals = new HashMap<>();
        colScanTotalsCaptions = new HashMap<>();
        scanRowAndColumnAggregations(table.getRowTotalsLists(), rowScanTotals, rowScanTotalsCaptions, table.getColTotalsLists(), colScanTotals, colScanTotalsCaptions);
    }

    private void initCellStyles() {

        Font font = excelWorkbook.createFont();
        font.setFontHeightInPoints((short) BASIC_SHEET_FONT_SIZE);
        font.setFontName(BASIC_SHEET_FONT_FAMILY);

        highLightFont = excelWorkbook.createFont();
        highLightFont.setFontHeightInPoints((short) BASIC_SHEET_FONT_SIZE);
        highLightFont.setFontName(BASIC_SHEET_FONT_FAMILY);
        ((XSSFFont) highLightFont).setColor(new XSSFColor(new java.awt.Color(193, 0, 32)));

        basicCS = excelWorkbook.createCellStyle();
        basicCS.setFont(font);
        basicCS.setAlignment(CellStyle.ALIGN_LEFT);
        basicCS.setVerticalAlignment(CellStyle.VERTICAL_TOP);
        basicCS.setWrapText(true); //Set wordwrap
        setCellBordersColor(basicCS);

        Font totalsFont = excelWorkbook.createFont();
        totalsFont.setFontHeightInPoints((short) BASIC_SHEET_FONT_SIZE);
        totalsFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        totalsFont.setFontName(BASIC_SHEET_FONT_FAMILY);

        highLightTotalsFont = excelWorkbook.createFont();
        highLightTotalsFont.setFontHeightInPoints((short) BASIC_SHEET_FONT_SIZE);
        highLightTotalsFont.setBoldweight(Font.BOLDWEIGHT_BOLD);
        highLightTotalsFont.setFontName(BASIC_SHEET_FONT_FAMILY);
        ((XSSFFont) highLightTotalsFont).setColor(new XSSFColor(new java.awt.Color(193, 0, 32)));

        totalsCS = excelWorkbook.createCellStyle();
        totalsCS.setFont(totalsFont);
        totalsCS.setAlignment(CellStyle.ALIGN_RIGHT);
        setCellBordersColor(totalsCS);

        // Setting the default styling for number cells
        numberCS = excelWorkbook.createCellStyle();
        numberCS.setFont(font);
        numberCS.setAlignment(CellStyle.ALIGN_RIGHT);

        /*
         * justasg: Let's set default format, used if measure has no format at
         * all. More info:
         * http://poi.apache.org/apidocs/org/apache/poi/ss/usermodel/
         * BuiltinFormats.html#getBuiltinFormat(int) If we don't have default
         * format, it will output values up to maximum detail, i.e.
         * 121212.3456789 and we like them as 121,212.346
         */
        DataFormat fmt = excelWorkbook.createDataFormat();
        short dataFormat = fmt.getFormat(SaikuProperties.webExportExcelDefaultNumberFormat);
        numberCS.setDataFormat(dataFormat);

        Font headerFont = excelWorkbook.createFont();
        headerFont.setFontHeightInPoints((short) BASIC_SHEET_FONT_SIZE);
        headerFont.setFontName(BASIC_SHEET_FONT_FAMILY);
        headerFont.setBoldweight(Font.BOLDWEIGHT_BOLD);

        lighterHeaderCellCS = excelWorkbook.createCellStyle();
        lighterHeaderCellCS.setFont(headerFont);
        lighterHeaderCellCS.setAlignment(CellStyle.ALIGN_CENTER);
        lighterHeaderCellCS.setVerticalAlignment(CellStyle.VERTICAL_CENTER);
        lighterHeaderCellCS.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        lighterHeaderCellCS.setFillPattern(CellStyle.SOLID_FOREGROUND);
        lighterHeaderCellCS.setWrapText(true);
        setCellBordersColor(lighterHeaderCellCS);

        CellStyle darkerHeaderCellCS = excelWorkbook.createCellStyle();
        darkerHeaderCellCS.setFont(headerFont);
        darkerHeaderCellCS.setAlignment(CellStyle.ALIGN_CENTER);
        darkerHeaderCellCS.setFillForegroundColor(IndexedColors.GREY_40_PERCENT.getIndex());
        darkerHeaderCellCS.setFillPattern(CellStyle.SOLID_FOREGROUND);
        setCellBordersColor(darkerHeaderCellCS);

    }

    private void setCellBordersColor(CellStyle style) {

        style.setBorderBottom(CellStyle.BORDER_THIN);
        style.setBottomBorderColor(IndexedColors.GREY_80_PERCENT.getIndex());
        style.setBorderTop(CellStyle.BORDER_THIN);
        style.setTopBorderColor(IndexedColors.GREY_80_PERCENT.getIndex());
        style.setBorderLeft(CellStyle.BORDER_THIN);
        style.setLeftBorderColor(IndexedColors.GREY_80_PERCENT.getIndex());
        style.setBorderRight(CellStyle.BORDER_THIN);
        style.setRightBorderColor(IndexedColors.GREY_80_PERCENT.getIndex());
    }

    public byte[] build() throws SaikuServiceException {

        Long start = (new Date()).getTime();
        int startRow = initExcelSheet();
        Long init = (new Date()).getTime();
        int lastHeaderRow = buildExcelTableHeader(startRow);
        Long header = (new Date()).getTime();
        addExcelTableRows(lastHeaderRow);
        if (SaikuProperties.webExportExcelTotalSummary) {
            addTotalsSummary(lastHeaderRow);
        }
        setColumnWidths();
        Long content = (new Date()).getTime();
        finalizeExcelSheet(startRow);
        Long finalizing = (new Date()).getTime();

        log.debug("Init: " + (init - start) + "ms header: " + (header - init) + "ms content: " + (content - header)
          + "ms finalizing: " + (finalizing - content) + "ms ");
        ByteArrayOutputStream bout = new ByteArrayOutputStream();

        try {
            excelWorkbook.write(bout);
        } catch (IOException e) {
            throw new SaikuServiceException("Error creating excel export for query", e);
        }
        return bout.toByteArray();
    }

    private void setColumnWidths() {
        if (MapUtils.isNotEmpty(columnWidth)) {
            for (Map.Entry<Integer, Integer> entry : columnWidth.entrySet()) {
                workbookSheet.setColumnWidth(entry.getKey(), entry.getValue());
            }
        }
    }

    private void registerColumnWidth(Integer aColumnIndex, String aValue) {
        registerColumnWidth(aColumnIndex, aValue, -1);
    }

    private void registerColumnWidth(Integer aColumnIndex, String aValue, int aMaxLength) {
        int width = checkWidthByNumChars(aValue, aMaxLength);
        if (width > 0) {
            if (columnWidth.get(aColumnIndex) == null) {
                columnWidth.put(aColumnIndex, width);
            }

            if (columnWidth.get(aColumnIndex) < width) {
                columnWidth.put(aColumnIndex, width);
            }
        }
    }

    private Integer checkWidthByNumChars(String aValue, int aMaxLength) {
        if (StringUtils.isBlank(aValue)) {
            return 0;
        }

        int maxChars = aMaxLength == -1 ? MAX_CHAR_CELL : aMaxLength;
        int charsCount = aValue.length() < MIN_CHAR_CELL ? MIN_CHAR_CELL : aValue.length() < maxChars ? aValue.length()+1 : maxChars;
        return ((int)(charsCount*1.3))*256;
    }

    private void checkRowLimit(int rowIndex) {
        if ((rowIndex + 1) > maxRows) {
            log.warn("Excel sheet is truncated, only outputting " + maxRows + " rows of " + (rowIndex + 1));
        }
    }

    private void addTotalsSummary(int startingRow) {
        int rowIndex = startingRow + rowsetBody.length + 2; // Lines offset after data, in order to add summary
        checkRowLimit(rowIndex);

        // Columns summary
        if (colScanTotals.keySet().size() > 0) {
            Row row = workbookSheet.createRow(rowIndex);
            row.setHeight((short)-1);
            Cell cell = row.createCell(0);
            cell.setCellStyle(lighterHeaderCellCS);
            cell.setCellValue("Columns");

            for (Integer colKey : colScanTotals.keySet()) {
                TotalAggregator[][] colAggregator = colScanTotals.get(colKey);

                if (colAggregator == null) continue;

                for (int x = 0; x < colAggregator.length; x++) {
                    rowIndex++;
                    checkRowLimit(rowIndex);

                    Measure measure = this.table.getSelectedMeasures()[x];

                    TotalAggregator agg = colAggregator[x][0];
                    row = workbookSheet.createRow(rowIndex);
                    row.setHeight((short)-1);

                    // Measure name
                    cell = row.createCell(0);
                    cell.setCellStyle(lighterHeaderCellCS);
                    cell.setCellValue(measure.getCaption() +  ":");

                    // Measure aggregator
                    cell = row.createCell(1);
                    cell.setCellStyle(basicCS);
                    cell.setCellValue(agg.getClass().getSimpleName().substring(0, 3));
                }
            }
        }

        // Rows summary
        if (rowScanTotals.keySet().size() > 0) {
            rowIndex++;
            checkRowLimit(rowIndex);

            Row row = workbookSheet.createRow(rowIndex);
            row.setHeight((short)-1);
            Cell cell = row.createCell(0);
            cell.setCellStyle(lighterHeaderCellCS);
            cell.setCellValue("Rows");

            for (Integer rowKey : rowScanTotals.keySet()) {
                TotalAggregator[][] rowAggregator = rowScanTotals.get(rowKey);

                if (rowAggregator == null) continue;

                for (int x = 0; x < rowAggregator.length; x++) {
                    for (int y = 0; y < this.table.getSelectedMeasures().length; y++) {
                        rowIndex++;
                        checkRowLimit(rowIndex);

                        Measure measure = this.table.getSelectedMeasures()[y];
                        TotalAggregator agg = rowAggregator[x][y];

                        row = workbookSheet.createRow(rowIndex);
                        row.setHeight((short)-1);

                        // Measure name
                        cell = row.createCell(0);
                        cell.setCellStyle(lighterHeaderCellCS);
                        cell.setCellValue(measure.getCaption() +  ":");

                        // Measure aggregator
                        cell = row.createCell(1);
                        cell.setCellStyle(basicCS);
                        cell.setCellValue(agg.getClass().getSimpleName().substring(0, 3));
                    }
                }
            }
        }

    }

    private void finalizeExcelSheet(int startRow) {
/*
        boolean autoSize = (rowsetBody != null && rowsetBody.length > 0 && rowsetBody.length < 10000
          && rowsetHeader != null && rowsetHeader.length > 0 && rowsetHeader[0].length < 200);

        if (autoSize) {
            log.warn("Skipping auto-sizing columns, more than 10000 rows and/or 200 columns");
        }

        Long start = (new Date()).getTime();
        if (autoSize) {
            //Detect max column size
            int maxColumnsSize = rowsetBody[0].length;
            if (!colScanTotals.isEmpty()) {
                maxColumnsSize = Collections.max(colScanTotals.keySet()) + 1;
            }
            // Autosize columns
            for (int i = 0; i < maxColumns && i < maxColumnsSize; i++) {
                workbookSheet.autoSizeColumn(i);
            }
        }
        Long end = (new Date()).getTime();
        log.debug("Autosizing: " + (end - start) + "ms");*/
        // Freeze the header columns
        int headerWidth = rowsetHeader.length;
        workbookSheet.createFreezePane(0, startRow + headerWidth, 0, startRow + headerWidth);
    }

    private int initExcelSheet() {
        // Main Workbook Sheet
        if (StringUtils.isNotBlank(options.sheetName)) {
            workbookSheet = excelWorkbook.createSheet(this.sheetName);
        } else {
            workbookSheet = excelWorkbook.createSheet();
        }
        initSummarySheet();
        return 0;
    }

    private void initSummarySheet() {

        // Main Workbook Sheet
        Sheet summarySheet = excelWorkbook.createSheet("Summary page");

        int row = 1;

        Row sheetRow = summarySheet.createRow((int) row);
        sheetRow.setHeight((short)-1);
        Cell cell = sheetRow.createCell(0);
        String todayDate = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).format(new Date());
        cell.setCellValue("Export date and time: " + todayDate);
        summarySheet.addMergedRegion(new CellRangeAddress(1, 1, 0, 2));
        row = row + 2;

        sheetRow = summarySheet.createRow((int) row);
        sheetRow.setHeight((short)-1);
        cell = sheetRow.createCell(0);
        cell.setCellValue("Dimension");
        cell = sheetRow.createCell(1);
        cell.setCellValue("Level");
        cell = sheetRow.createCell(2);
        cell.setCellValue("Filter Applied");
        row++;

        if (queryFilters != null) {
            for (ThinHierarchy item : queryFilters) {
                for (ThinLevel s : item.getLevels().values()) {
                    for (ThinMember i : s.getSelection().getMembers()) {
                        sheetRow = summarySheet.createRow((short) row);
                        sheetRow.setHeight((short)-1);
                        cell = sheetRow.createCell(0);
                        cell.setCellValue(item.getCaption());
                        cell = sheetRow.createCell(1);
                        cell.setCellValue(s.getCaption());
                        cell = sheetRow.createCell(2);
                        cell.setCellValue(i.getCaption());
                        row++;
                    }
                }
            }
        }

        row += 2;

        int rowLength = (rowsetBody != null) ? rowsetBody.length : 0;
        int columnCount = (rowsetHeader != null && rowsetHeader.length > 0) ? rowsetHeader[0].length : 0;
        int headerLength = (rowsetHeader != null) ? rowsetHeader.length : 0;

        if (columnCount > maxColumns) {
            sheetRow = summarySheet.createRow((int) row);
            sheetRow.setHeight((short)-1);
            cell = sheetRow.createCell(0);
            cell.setCellValue("Excel sheet is truncated, only contains " + maxColumns + " columns of " + (columnCount));
            summarySheet.addMergedRegion(new CellRangeAddress(row, row, 0, 10));
            row++;
        }

        if ((headerLength + rowLength) > maxRows) {
            sheetRow = summarySheet.createRow((int) row);
            sheetRow.setHeight((short)-1);
            cell = sheetRow.createCell(0);
            cell.setCellValue(
              "Excel sheet is truncated, only contains " + maxRows + " rows of " + (headerLength + rowLength));
            summarySheet.addMergedRegion(new CellRangeAddress(row, row, 0, 10));
            row++;
        }

        row++;

        sheetRow = summarySheet.createRow((int) row);
        sheetRow.setHeight((short)-1);
        cell = sheetRow.createCell(0);
        cell.setCellValue(SaikuProperties.webExportExcelPoweredBy);
        summarySheet.addMergedRegion(new CellRangeAddress(row, row, 0, 10));

        // Autosize columns for summary sheet
        for (int i = 0; i < 5; i++) {
            summarySheet.autoSizeColumn(i);
        }
    }

    private void addExcelTableRows(int startingRow) {

        Row sheetRow = null;
        Cell cell = null;
        Map<Integer, String> tmpCellUniqueValueByColumn = new HashMap<>();
        Map<Integer, Map<Integer, Boolean>> mergeRowsByColumn = new HashMap<>();

        if ((startingRow + rowsetBody.length) > maxRows) {
            log.warn("Excel sheet is truncated, only outputting " + maxRows + " rows of "
              + (rowsetBody.length + startingRow));
        }
        if (rowsetBody.length > 0 && rowsetBody[0].length > maxColumns) {
            log.warn("Excel sheet is truncated, only outputting " + maxColumns + " columns of "
              + (rowsetBody[0].length));
        }

        int rowCount = startingRow;

        for (int x = 0; (x + startingRow) < maxRows && x < rowsetBody.length; x++) {

            int excelRowIndex = x + startingRow;
            sheetRow = workbookSheet.createRow(excelRowIndex);
            sheetRow.setHeight((short)-1);

            int column = 0;
            for (int y = 0; y < maxColumns && y < rowsetBody[x].length; y++) {
                cell = sheetRow.createCell(column);

                AbstractBaseCell baseCell = rowsetBody[x][y];

                //Detect merge cells
                findMergeCells(baseCell, excelRowIndex, y, mergeRowsByColumn, tmpCellUniqueValueByColumn);

                String value = baseCell.getFormattedValue();

                if (value == null && options.repeatValues) {
                    // If the row cells has a null values it means the value is
                    // repeated in the data internally
                    // but not in the interface. To properly format the Excel
                    // export file we need that value so we
                    // get it from the same position in the prev row
                    value = workbookSheet.getRow(sheetRow.getRowNum() - 1).getCell(column).getStringCellValue();
                }

                cell.setCellStyle(basicCS);
                cell.setCellValue(value);
                // Use rawNumber only is there is a formatString
                if (rowsetBody[x][y] instanceof DataCell) {
                    DataCell dataCell = (DataCell) rowsetBody[x][y];
                    String formatString = dataCell.getFormatString();
                    if ((dataCell.getRawNumber() != null) && (formatString != null) && !formatString.trim().isEmpty()) {
                        Number numberValue = dataCell.getRawNumber();
                        cell.setCellValue(numberValue.doubleValue());
                        applyCellFormatting(cell, dataCell,
                          SaikuProperties.webExportExcelHighlightNegativeNumbers && numberValue.doubleValue() < 0);
                    }
                }

                registerColumnWidth(column, value);

                //Set column sub totalstotals
                column = setColTotalAggregationCell(colScanTotals, colScanTotalsCaptions, sheetRow, x, column, true, x == 0);

                //Set column grand totals
                if (y == rowsetBody[x].length - 1) {
                    setColTotalAggregationCell(colScanTotals, colScanTotalsCaptions, sheetRow, x, column - 1, true, x == 0);
                }
            }

            // Set row sub totals
            startingRow = setRowTotalAggregationCell(rowScanTotals, rowScanTotalsCaptions, startingRow, x, true);
            rowCount = startingRow + x;
        }

        //Set row grand totals
        setRowTotalAggregationCell(rowScanTotals, rowScanTotalsCaptions, rowCount, 0, true);

        //Add merge cells
        addMergedRegions(mergeRowsByColumn);
    }

    private void scanRowAndColumnAggregations(List<TotalNode>[] rowTotalsLists,
                                              Map<Integer, TotalAggregator[][]> rowScanTotals,
                                              Map<Integer, String[]> aRowScanTotalsCaptions,
                                              List<TotalNode>[] colTotalsLists,
                                              Map<Integer, TotalAggregator[][]> colScanTotals,
                                              Map<Integer, String[]> aColScanTotalsCaptions) {
        if (rowTotalsLists != null) {
            for (List<TotalNode> totalNodes : rowTotalsLists) {
                //Scan row totals
                scanAggregations(true, totalNodes, rowScanTotals, aRowScanTotalsCaptions);
            }
        }
        if (colTotalsLists != null) {
            for (List<TotalNode> totalNodes : colTotalsLists) {
                //Scan Columns grand totals
                scanAggregations(false, totalNodes, colScanTotals, aColScanTotalsCaptions);
            }
        }
    }

    private void scanAggregations(boolean row, List<TotalNode> totalNodes, Map<Integer, TotalAggregator[][]> scanSums,
                                  Map<Integer, String[]> aTotalsCaptions) {
        if (totalNodes != null && (!totalNodes.isEmpty())) {
            int index;
            if (row) {
                index = rowsetHeader.length - 2;
            } else {
                index = detectColumnStartIndex();
            }
            for (TotalNode n : totalNodes) {
                TotalAggregator[][] tg = n.getTotalGroups();
                if (availableTotalAgg(tg)) {
                    if (n.getSpan() > n.getWidth()) {
                        index += n.getSpan();
                    } else {
                        index += n.getWidth();
                    }
                    index++;
                    scanSums.put(index, tg);
                    aTotalsCaptions.put(index, n.getMemberCaptions());
                }
            }
        }
    }

    private boolean availableTotalAgg(TotalAggregator[][] aTa) {
        if (aTa.length == 0) {
            return false;
        }

        for (TotalAggregator[] ta : aTa) {
            for (TotalAggregator t : ta) {
                if (!(t instanceof BlankAggregator)) {
                    return true;
                }
            }
        }

        return false;
    }

    private int setRowTotalAggregationCell(Map<Integer, TotalAggregator[][]> scanTotals,
                                           Map<Integer, String[]> aScanTotalsCaptions,
                                           int startIndex, int subIndex, boolean grandTotal) {
        if (!scanTotals.isEmpty()) {
            int row = subIndex + startIndex;
            TotalAggregator[][] aggregatorsTable = scanTotals.get(row);
            String[] totalCaptions = aScanTotalsCaptions.get(row);
            if (aggregatorsTable != null) {
                int aggIndex = 0;
                for (TotalAggregator[] aggregators : aggregatorsTable) {
                    //Create totals row
                    Row sheetRow = workbookSheet.createRow(row + 1);
                    sheetRow.setHeight((short)-1);

                    //Detect column start index
                    int startColumnIndex = detectColumnStartIndex();

                    if (grandTotal) {
                        setGrandTotalLabel(sheetRow, startColumnIndex, totalCaptions == null ? null : totalCaptions[aggIndex], false);
                    }

                    int column = startColumnIndex;

                    for (TotalAggregator aggregator : aggregators) {

                        //Calculate column sub total index
                        column = setColTotalAggregationCell(colScanTotals, colScanTotalsCaptions, sheetRow, -1, column, false, false);

                        //Create row totals cell
                        Cell cell = sheetRow.createCell(column);
                        Double value = aggregator.getValue();
                        if (value != null) {
                            cell.setCellValue(value);
                        }

                        String formatString = aggregator.getFormat().getFormatString();
                        CellStyle totalCSClone = excelWorkbook.createCellStyle();
                        totalCSClone.cloneStyleFrom(totalsCS);
                        setDataFormat(totalCSClone, formatString);
                        cell.setCellStyle(totalCSClone);
                        if (SaikuProperties.webExportExcelHighlightNegativeNumbers && value != null && value < 0) {
                            totalCSClone.setFont(highLightTotalsFont);
                        }

                        registerColumnWidth(column-1, String.valueOf(value));
                    }

                    aggIndex++;
                    row++;
                }
                startIndex++;
            }
        }
        return startIndex;
    }

    /**
     * @return columns data start index
     */
    private int detectColumnStartIndex() {
        int index = 0;
        if (rowsetBody.length > 0) {
            for (AbstractBaseCell cell : rowsetBody[0]) {
                if (cell instanceof MemberCell) {
                    index++;
                }
            }
            index--;
        }
        return index;
    }

    private int setColTotalAggregationCell(Map<Integer, TotalAggregator[][]> scanTotals,
                                           Map<Integer, String[]> aScanTotalsCaptions,
                                           Row sheetRow, int x, int column, boolean setValue, boolean grandTotal) {
        column++;

        if (!scanTotals.isEmpty()) {
            TotalAggregator[][] aggregatorsTable = scanTotals.get(column);
            String[] totalCaptions = aScanTotalsCaptions.get(column);

            if (aggregatorsTable != null) {
                if (setValue) {
                    int aggIndex = 0;
                    for (TotalAggregator[] aggregators : aggregatorsTable) {
                        if (grandTotal) {
                            setGrandTotalLabel(sheetRow.getRowNum() - 1, column, totalCaptions == null ? null : totalCaptions[aggIndex], true);
                        }

                        Cell cell = sheetRow.createCell(column);
                        Double value = aggregators[x].getValue();
                        if (value != null) {
                            cell.setCellValue(value);
                        }

                        String formatString = aggregators[x].getFormat().getFormatString();
                        CellStyle totalCSClone = excelWorkbook.createCellStyle();
                        totalCSClone.cloneStyleFrom(totalsCS);
                        setDataFormat(totalCSClone, formatString);
                        if (SaikuProperties.webExportExcelHighlightNegativeNumbers && value != null && value < 0) {
                            totalCSClone.setFont(highLightTotalsFont);
                        }
                        cell.setCellStyle(totalCSClone);

                        registerColumnWidth(column, String.valueOf(value));
                        column++;
                        aggIndex++;
                    }
                }
            }
        }

        return column;
    }

    private void setDataFormat(CellStyle aCSClone, String aFormatString) {
        try {
            aFormatString = FormatUtil.getFormatString(aFormatString);
            DataFormat fmt = excelWorkbook.createDataFormat();
            short dataFormat = fmt.getFormat(aFormatString);
            aCSClone.setDataFormat(dataFormat);
        } catch (Exception ex) {
        }
    }

    private void setGrandTotalLabel(int x, int y, String aTotalCaption, boolean header) {
        Row sheetRow = workbookSheet.getRow(x);
        if (sheetRow != null) {
            setGrandTotalLabel(sheetRow, y, aTotalCaption, header);
        }
    }

    private void setGrandTotalLabel(Row sheetRow, int y, String aTotalCaption, boolean header) {
        Cell cell = sheetRow.createCell(y);
        //TODO i18n переделать под properties файл
        String value = (StringUtils.isNotBlank(aTotalCaption) ? aTotalCaption + " " : "") + "Общий итог";
        if (header) {
            fillHeaderCell(sheetRow, value, y);
        } else {
            cell.setCellValue(value);
            cell.setCellStyle(basicCS);

            registerColumnWidth(y, value);
        }
    }

    /**
     * Apply exact number format to excel Cell from its DataCell. Caller checks
     * the DataCell rawNumber and formatString are correct.
     *  @param cell The excel cell to apply formatting
     * @param dataCell The source
     * @param aHighLightNegNumber highlight negative numbers
     */
    private void applyCellFormatting(Cell cell, DataCell dataCell, boolean aHighLightNegNumber) {
        /*
        * Previously, the CellStyles were being kept on a hash map for reuse,
        * but the key used was just the formatString (not considering the
        * colours), so, if many cells shared the same formatString but using
        * different colours, all those cells would have the last cell colour.
        */
        String formatString = dataCell.getFormatString();
        CellStyle numberCSClone = excelWorkbook.createCellStyle();

        numberCSClone.cloneStyleFrom(numberCS);

        setDataFormat(numberCSClone, formatString);

        // Check for cell background
        Map<String, String> properties = dataCell.getProperties();

        // Just style the cell if it contains a value
        if (dataCell.getRawNumber() != null && properties.containsKey("style")) {
            String colorCode = properties.get("style");
            short colorCodeIndex = getColorFromCustomPalette(colorCode);

            if (colorCodeIndex != -1) {
                numberCSClone.setFillForegroundColor(colorCodeIndex);
                numberCSClone.setFillPattern(CellStyle.SOLID_FOREGROUND);
            } else if (customColorsPalette == null) {
                try {

                    if (cssColorCodesProperties != null && cssColorCodesProperties.containsKey(colorCode)) {
                        colorCode = cssColorCodesProperties.getProperty(colorCode);
                    }

                    int redCode   = Integer.parseInt(colorCode.substring(1, 3), 16);
                    int greenCode = Integer.parseInt(colorCode.substring(3, 5), 16);
                    int blueCode  = Integer.parseInt(colorCode.substring(5, 7), 16);

                    numberCSClone.setFillPattern(CellStyle.SOLID_FOREGROUND);

                    ((XSSFCellStyle) numberCSClone).setFillForegroundColor(
                      new XSSFColor(new java.awt.Color(redCode, greenCode, blueCode)));
                    ((XSSFCellStyle) numberCSClone).setFillBackgroundColor(
                      new XSSFColor(new java.awt.Color(redCode, greenCode, blueCode)));
                } catch (Exception e) {
                    // we tried to set the color, no luck, lets continue
                    // without
                }

            }
        } else {
            numberCSClone.setFillForegroundColor(numberCS.getFillForegroundColor());
            numberCSClone.setFillBackgroundColor(numberCS.getFillBackgroundColor());
        }

        numberCSClone.setBorderTop(CellStyle.BORDER_THIN);
        numberCSClone.setBorderBottom(CellStyle.BORDER_THIN);
        numberCSClone.setBorderLeft(CellStyle.BORDER_THIN);
        numberCSClone.setBorderRight(CellStyle.BORDER_THIN);
        ((XSSFCellStyle) numberCSClone).setFillBackgroundColor(new XSSFColor(new java.awt.Color(255, 255, 255)));

        if (aHighLightNegNumber) {
          numberCSClone.setFont(highLightFont);
        }

        cell.setCellStyle(numberCSClone);
    }

    private short getColorFromCustomPalette(String style) {

        short returnedColorIndex = -1;
        InputStream is = null;

        if (colorCodesMap.containsKey(style)) {
            returnedColorIndex = colorCodesMap.get(style).shortValue();
        } else {
            try {

                if (cssColorCodesProperties == null) {
                    is = getClass().getResourceAsStream(CSS_COLORS_CODE_PROPERTIES);
                    if (is != null) {
                        cssColorCodesProperties = new Properties();
                        cssColorCodesProperties.load(is);
                    }
                }

                String colorCode = cssColorCodesProperties.getProperty(style);
                if (colorCode != null) {
                    try {
                        int redCode = Integer.parseInt(colorCode.substring(1, 3), 16);
                        int greenCode = Integer.parseInt(colorCode.substring(3, 5), 16);
                        int blueCode = Integer.parseInt(colorCode.substring(5, 7), 16);
                        if (customColorsPalette != null) {
                            customColorsPalette.setColorAtIndex((byte) nextAvailableColorCode, (byte) redCode,
                              (byte) greenCode, (byte) blueCode);
                            returnedColorIndex = customColorsPalette.getColor(nextAvailableColorCode).getIndex();
                            colorCodesMap.put(style, (int) returnedColorIndex);
                        } else {
                            return -1;
                        }
                        nextAvailableColorCode++;
                    } catch (Exception e) {
                        // we tried to set the color, no luck, lets continue
                        // without
                        return -1;
                    }
                }
            } catch (IOException e) {
                log.error("IO Exception", e);
            } finally {
                try {
                    if (is != null)
                        is.close();
                } catch (IOException e) {
                    log.error("IO Exception", e);
                }
            }

        }

        return returnedColorIndex; // To change body of created methods use File
        // | Settings | File Templates.
    }

    private int buildExcelTableHeader(int startRow) {

        Row sheetRow = null;
        int x = 0;
        int y = 0;
        int startSameFromPos = 0;
        int mergedCellsWidth = 0;
        boolean isLastHeaderRow = false;
        boolean isLastColumn = false;
        String nextHeader = EMPTY_STRING;
        String currentHeader = EMPTY_STRING;
        ArrayList<ExcelMergedRegionItemConfig> mergedItemsConfig = new ArrayList<>();

        for (x = 0; x < rowsetHeader.length; x++) {

            sheetRow = workbookSheet.createRow((int) x + startRow);
            sheetRow.setHeight((short)-1);

            nextHeader = EMPTY_STRING;
            isLastColumn = false;
            startSameFromPos = 0;
            mergedCellsWidth = 0;

            if (x + 1 == rowsetHeader.length) {
                isLastHeaderRow = true;
            }

            int column = 0;
            for (y = 0; y < maxColumns && y < rowsetHeader[x].length; y++) {

                currentHeader = rowsetHeader[x][y].getFormattedValue();
                if (currentHeader != null) {
                    if (rowsetHeader[x].length == y + 1) {
                        isLastColumn = true;
                    } else {
                        nextHeader = rowsetHeader[x][y + 1].getFormattedValue();
                    }

                    manageColumnHeaderDisplay(sheetRow, x, column, currentHeader);

                    if (!isLastHeaderRow) {
                        if (nextHeader != null && !nextHeader.equals(currentHeader) || isLastColumn) {
                            manageCellsMerge(column, x + startRow, mergedCellsWidth + 1, startSameFromPos,
                              mergedItemsConfig);
                            startSameFromPos = column + 1;
                            mergedCellsWidth = 0;
                        } else if (nextHeader != null && nextHeader.equals(currentHeader)) {
                            mergedCellsWidth++;
                        }
                    }
                } else {
                    startSameFromPos++;
                }

                //Set sub total column space
                int nextColumn = setColTotalAggregationCell(colScanTotals, colScanTotalsCaptions, sheetRow, x, column, false, false);
                if (column != nextColumn - 1) {
                    startSameFromPos++;
                }
                column = nextColumn;
            }
            // Manage the merge condition on exit from columns scan
            if (!isLastHeaderRow)
                manageCellsMerge(y - 1, x, mergedCellsWidth + 1, startSameFromPos, mergedItemsConfig);
        }

        if (topLeftCornerHeight > 0 && topLeftCornerWidth > 0) {
            workbookSheet.addMergedRegion(
              new CellRangeAddress(startRow, startRow + topLeftCornerHeight - 1, 0, topLeftCornerWidth - 1));
        }

        if (mergedItemsConfig.size() > 0) {
            for (ExcelMergedRegionItemConfig item : mergedItemsConfig) {
                int lastCol = item.getStartX() + item.getWidth() - 1;
                lastCol = lastCol >= maxColumns ? maxColumns - 1 : lastCol;
                workbookSheet.addMergedRegion(new CellRangeAddress(item.getStartY(),
                  item.getStartY() + item.getHeight(), item.getStartX(), lastCol));
            }
        }

        return x + startRow;
    }

    private void manageColumnHeaderDisplay(Row sheetRow, int x, int y, String currentHeader) {
        if (topLeftCornerHeight > 0 && x >= topLeftCornerHeight) {
            fillHeaderCell(sheetRow, currentHeader, y);
        } else if ((topLeftCornerHeight > 0 && x < topLeftCornerHeight)
          && (topLeftCornerWidth > 0 && y >= topLeftCornerWidth)) {
            fillHeaderCell(sheetRow, currentHeader, y);
        } else if (topLeftCornerHeight == 0 && topLeftCornerWidth == 0)
            fillHeaderCell(sheetRow, currentHeader, y);
    }

    private void manageCellsMerge(int rowPos, int colPos, int width, int startSameFromPos,
                                  ArrayList<ExcelMergedRegionItemConfig> mergedItemsConfig) {

        ExcelMergedRegionItemConfig foundItem = null;
        boolean itemGetFromList = false;

        if (width == 1)
            return;

        for (ExcelMergedRegionItemConfig item : mergedItemsConfig) {
            if (item.getStartY() == colPos && item.getStartX() == rowPos) {
                foundItem = item;
                itemGetFromList = true;
            }
        }

        if (foundItem == null)
            foundItem = new ExcelMergedRegionItemConfig();

        foundItem.setHeight(0);
        foundItem.setWidth(width);
        foundItem.setStartX(startSameFromPos);
        foundItem.setStartY(colPos);
        if (mergedItemsConfig.isEmpty() || !itemGetFromList)
            mergedItemsConfig.add(foundItem);
    }

    private void fillHeaderCell(Row sheetRow, String formattedValue, int y) {
        sheetRow.setHeight((short)-1);
        Cell cell = sheetRow.createCell(y);
        cell.setCellValue(formattedValue);
        cell.setCellStyle(lighterHeaderCellCS);

        registerColumnWidth(y, formattedValue, 12);
    }

    /**
     * Find the width in cells of the top left corner of the table
     *
     * @return
     */
    private int findTopLeftCornerWidth() {

        int width = 0;
        int x = 0;
        boolean exit = (rowsetHeader.length < 1 || rowsetHeader[0][0].getRawValue() != null);
        String cellValue = null;

        for (x = 0; (!exit && rowsetHeader[0].length > x); x++) {

            cellValue = rowsetHeader[0][x].getRawValue();
            if (cellValue == null) {
                width = x + 1;
            } else {
                exit = true;
            }
        }

        return width;
    }

    /**
     * Find the height in cells of the top left corner of the table
     *
     * @return
     */
    private int findTopLeftCornerHeight() {
        return rowsetHeader.length > 0 ? rowsetHeader.length - 1 : 0;
    }

    /**
     * @param mergeRowsByColumn merged indexes
     */
    private void addMergedRegions(Map<Integer, Map<Integer, Boolean>> mergeRowsByColumn) {
        if (mergeRowsByColumn != null) {
            for (Map.Entry<Integer, Map<Integer, Boolean>> e : mergeRowsByColumn.entrySet()) {

                int col = e.getKey();

                Map<Integer, Boolean> rows = e.getValue();

                if (rows != null) {

                    int mergeCount = 1;
                    for (Map.Entry<Integer, Boolean> rowEntry : rows.entrySet()) {

                        int row = rowEntry.getKey();

                        boolean current = rowEntry.getValue();

                        Boolean next = rows.get(rowEntry.getKey() + 1);

                        if (current) {
                            if (next == null || !next) {
                                workbookSheet.addMergedRegion(new CellRangeAddress(row - mergeCount, row, col, col));
                            }
                            mergeCount++;
                        } else {
                            mergeCount = 1;
                        }
                    }
                }
            }
        }
    }

    /**
     * @param baseCell                   current cell
     * @param excelRowIndex              row index
     * @param y                          column
     * @param mergeRowsByColumn          merge indexes store
     * @param tmpCellUniqueValueByColumn tmp map to compare previews value(max possible value = columns size)
     */
    private void findMergeCells(AbstractBaseCell baseCell,
                                int excelRowIndex,
                                int y,
                                Map<Integer, Map<Integer, Boolean>> mergeRowsByColumn,
                                Map<Integer, String> tmpCellUniqueValueByColumn) {
        if (baseCell instanceof MemberCell) {

            MemberCell memberCell = (MemberCell) baseCell;

            Map<Integer, Boolean> rowMerge = mergeRowsByColumn.get(y);
            if (rowMerge == null) {
                rowMerge = new TreeMap<>();
                mergeRowsByColumn.put(y, rowMerge);
            }

            //Compare preview and current cells
            String previousValue = tmpCellUniqueValueByColumn.get(y);

            Map<Integer, Boolean> previousColumn = mergeRowsByColumn.get(y - 1);

            boolean merge = previousValue != null && previousValue.equals(memberCell.getUniqueName());

            if (previousColumn != null) {
                Boolean previewColumnCellmergeValue = previousColumn.get(excelRowIndex);
                if ((previewColumnCellmergeValue != null) && (!previewColumnCellmergeValue) && merge) {
                    merge = false;
                }
            }
            rowMerge.put(excelRowIndex, merge);

            tmpCellUniqueValueByColumn.put(y, memberCell.getUniqueName());
        }
    }
}
