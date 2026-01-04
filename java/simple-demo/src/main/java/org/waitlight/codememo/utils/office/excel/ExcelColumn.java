package org.waitlight.codememo.utils.office.excel;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
public @interface ExcelColumn {
    /**
     * Corresponding title
     */
    String title() default "";

    /**
     * Column index
     * <p>
     * Takes precedence over the {@link #title()}
     */
    int index() default -1;

    boolean canImport() default true;

    boolean canExport() default true;
}