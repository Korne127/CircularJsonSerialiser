package de.korne127.circularJsonSerialiser.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * BeforeSerialise-Annotation für Methoden:<br>
 * Wenn eine Methode mit dieser Annotation belegt ist, wird sie vor der Serialisierung eines Objektes der
 * entsprechenden Klasse ausgeführt.
 * @author Korne127
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface BeforeSerialise {
}
