package org.waitlight.codememo.utils.office.excel;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.lang.reflect.Method;


@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CellHandler {
    private int index;
    private String title;
    private Method method;
    private boolean canImport;
    private boolean canExport;
}