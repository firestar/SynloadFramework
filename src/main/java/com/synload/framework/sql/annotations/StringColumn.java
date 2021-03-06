package com.synload.framework.sql.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Inherited
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface StringColumn {
    public int length();

    public String Type() default "varchar";

    public String CharSet() default "utf8";

    public String Default() default "utf8_general_ci";

    public String Collation() default "";

    public boolean NULL() default false;

    public boolean AutoIncrement() default false;

    public boolean Index() default false;

    public boolean Key() default false;
}