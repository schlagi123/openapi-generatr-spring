/*
 * This class is auto generated by https://github.com/hauner/openapi-generatr-spring.
 * DO NOT EDIT.
 */

package generated.api;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;

public interface Api {

    @GetMapping(path = "/array-global", produces = {"application/vnd.any"})
    ResponseEntity<Collection<String>> getArrayGlobal();

    @GetMapping(path = "/array-global-response", produces = {"application/vnd.global-response"})
    ResponseEntity<List<String>> getArrayGlobalResponse();

    @GetMapping(path = "/array-endpoint-response", produces = {"application/vnd.any"})
    ResponseEntity<Set<String>> getArrayEndpointResponse();

}
