package org.tfelab.common.db;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * 在配置文件中定义数据对象属于哪个数据库对象
 * @author karajan@tfelab.org
 * 2017/05/09
 */
@Target(TYPE)
@Retention(RUNTIME)
public @interface DBName {
	String value() default "";
}