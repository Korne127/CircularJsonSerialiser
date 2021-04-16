package de.korne127.circularJsonSerialiser.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Set;

/**
 * Setter-Annotation für Felder:
 * Wenn ein Feld mit dieser Annotation belegt ist, wird bei der Deserialisierung das Feld auf das Objekt gesetzt,
 * das im Serialiser zu dem Annotationswert mit der
 * {@link de.korne127.circularJsonSerialiser.Serialiser#setParameters(java.util.Map) setParameters}
 * Methode angegeben wurde, es sei denn, der Annotationswert ist als IgnoreSetterID über
 * {@link de.korne127.circularJsonSerialiser.Serialiser#ignoreSetterIDs(Set) ignoreSetterIDs} gesetzt worden.
 * @author Korne127
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Setter {
	String value();
}
