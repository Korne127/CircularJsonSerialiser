package de.korne127.circularJsonSerialiser.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * SerialiseFile-Annotation für Klassen<br>
 * Bei einer Anfrage nach einer separierenden Serialisierung eines Objektes wird die Kodierung
 * aller Objekte aus einer mit dieser Annotation und einem bestimmten Wert belegten Klasse
 * zusammen separiert in einem anderen String zurückgegeben.<br>
 * Es werden in dem Fall also so viele Strings zurückgegeben, wie verschiedene Werte dieser
 * Annotation existieren und mit dem zu Serialisierenden Objekt verbunden sind.<br>
 * Wenn diese Annotation mit dem Wert "Standard" oder ohne Wert benutzt wird, hat dies keinen
 * Effekt und wird nicht beachtet.
 * @author Korne127
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface SerialiseFile {
	String fileName() default "Standard";
}
