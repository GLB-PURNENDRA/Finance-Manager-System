package com.financemanager.dto;

import java.util.List;

public class StatementPreviewDto {

    private String fileName;
    private int totalRows;
    private int validRows;
    private int duplicateRows;
    private List<StatementRowDto> rows;

    public StatementPreviewDto() {}

    public StatementPreviewDto(String fileName, int totalRows, int validRows, int duplicateRows, List<StatementRowDto> rows) {
        this.fileName = fileName;
        this.totalRows = totalRows;
        this.validRows = validRows;
        this.duplicateRows = duplicateRows;
        this.rows = rows;
    }

    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }

    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }

    public int getValidRows() { return validRows; }
    public void setValidRows(int validRows) { this.validRows = validRows; }

    public int getDuplicateRows() { return duplicateRows; }
    public void setDuplicateRows(int duplicateRows) { this.duplicateRows = duplicateRows; }

    public List<StatementRowDto> getRows() { return rows; }
    public void setRows(List<StatementRowDto> rows) { this.rows = rows; }
}
