package de.korne127.circularJsonSerialiser.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * IgnoreCasting-Annotation für Felder:<br>
 * Wenn ein Feld mit dieser Annotation belegt ist, wird, falls das Feld eine Collection / Map ist oder enthält,
 * die zu einer anderen gecastet werden muss, keine Warnung ausgegeben (es sei denn, man befinde sich im
 * DEBUG-Modus).<br>
 * Dies ist hilfreich, wenn man nicht alle Warnungen ausschalten will, aber ein bestimmtes Feld hat, bei dem es
 * beabsichtigt ist, dass es umgecastet wird (zB eine Konstruktion mit Annotations um eine EnumMap zu
 * (de)serialisieren).
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface IgnoreCasting {
}
