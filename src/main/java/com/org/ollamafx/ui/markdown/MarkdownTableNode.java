package com.org.ollamafx.ui.markdown;

import com.vladsch.flexmark.ext.tables.TableBlock;
import com.vladsch.flexmark.ext.tables.TableBody;
import com.vladsch.flexmark.ext.tables.TableCell;
import com.vladsch.flexmark.ext.tables.TableHead;
import com.vladsch.flexmark.ext.tables.TableRow;
import com.vladsch.flexmark.util.ast.Node;
import javafx.geometry.HPos;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.text.TextFlow;

final class MarkdownTableNode {

    private MarkdownTableNode() {
    }

    static javafx.scene.Node build(TableBlock table) {
        GridPane grid = new GridPane();
        grid.getStyleClass().add("md-table");

        int row = 0;
        int maxCols = 0;

        Node child = table.getFirstChild();
        while (child != null) {
            if (child instanceof TableHead) {
                Node hr = child.getFirstChild();
                while (hr != null) {
                    if (hr instanceof TableRow) {
                        int cols = addRow(grid, (TableRow) hr, row, true);
                        maxCols = Math.max(maxCols, cols);
                        row++;
                    }
                    hr = hr.getNext();
                }
            } else if (child instanceof TableBody) {
                Node br = child.getFirstChild();
                while (br != null) {
                    if (br instanceof TableRow) {
                        int cols = addRow(grid, (TableRow) br, row, false);
                        maxCols = Math.max(maxCols, cols);
                        row++;
                    }
                    br = br.getNext();
                }
            }
            child = child.getNext();
        }

        for (int i = 0; i < maxCols; i++) {
            ColumnConstraints cc = new ColumnConstraints();
            cc.setHgrow(Priority.SOMETIMES);
            grid.getColumnConstraints().add(cc);
        }

        HBox wrapper = new HBox(grid);
        wrapper.getStyleClass().add("md-table-wrapper");
        HBox.setHgrow(grid, Priority.ALWAYS);
        return wrapper;
    }

    private static int addRow(GridPane grid, TableRow row, int rowIndex, boolean header) {
        int col = 0;
        Node cell = row.getFirstChild();
        while (cell != null) {
            if (cell instanceof TableCell) {
                TableCell tc = (TableCell) cell;
                TextFlow flow = new TextFlow();
                flow.getStyleClass().add(header ? "md-table-header-cell" : "md-table-cell");
                InlineRenderer.renderChildren(tc, flow, InlineRenderer.InlineStyle.EMPTY);

                StackPane cellPane = new StackPane(flow);
                cellPane.getStyleClass().add(header ? "md-table-header" : "md-table-body-cell");
                cellPane.setPadding(new Insets(8, 12, 8, 12));

                TableCell.Alignment align = tc.getAlignment();
                if (align == TableCell.Alignment.CENTER) {
                    StackPane.setAlignment(flow, Pos.CENTER);
                } else if (align == TableCell.Alignment.RIGHT) {
                    StackPane.setAlignment(flow, Pos.CENTER_RIGHT);
                } else {
                    StackPane.setAlignment(flow, Pos.CENTER_LEFT);
                }

                grid.add(cellPane, col, rowIndex);
                GridPane.setHgrow(cellPane, Priority.SOMETIMES);
                GridPane.setHalignment(cellPane, HPos.LEFT);
                col++;
            }
            cell = cell.getNext();
        }
        return col;
    }
}
