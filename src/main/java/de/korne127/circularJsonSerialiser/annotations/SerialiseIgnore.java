package de.korne127.circularJsonSerialiser.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

/**
 * SerialiseIgnore-Annotation für Felder:<br>
 * Wenn ein Attribut mit dieser Annotation belegt ist, wird es beim Serialisierungsprozess ignoriert
 * und nicht in dem generierten String kodiert mit zurückgegeben, es sei denn, der angegebene Wert der
 * Annotation ist als IgnoreExceptionID über
 * {@link de.korne127.circularJsonSerialiser.serialiser.Serialiser#ignoreExceptionIDs(Set) ignoreExceptionIDs}
 * gesetzt worden.
 * @author Korne127
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface SerialiseIgnore {
	String value() default "";
}
