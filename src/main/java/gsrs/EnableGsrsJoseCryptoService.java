package gsrs;

import gsrs.module.substance.services.JoseCryptoServiceConfiguration;
import gsrs.module.substance.services.JoseCryptoService;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.context.annotation.Import;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Import( { JoseCryptoServiceConfiguration.class, JoseCryptoService.class})
public @interface EnableGsrsJoseCryptoService {
}